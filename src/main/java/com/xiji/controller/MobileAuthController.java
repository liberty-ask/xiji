package com.xiji.controller;

import cn.hutool.core.util.PhoneUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiji.common.response.ResultVo;
import com.xiji.config.CustomConfig;
import com.xiji.entity.domain.Family;
import com.xiji.entity.domain.User;
import com.xiji.entity.dto.request.ForgotPasswordRequest;
import com.xiji.entity.dto.request.MobileLoginRequest;
import com.xiji.entity.dto.request.MobileRegisterRequest;
import com.xiji.entity.dto.request.SendSmsCodeRequest;
import com.xiji.entity.dto.response.MobileLoginResponse;
import com.xiji.entity.dto.response.MobileUserResponse;
import com.xiji.service.FamilyMemberService;
import com.xiji.service.FamilyService;
import com.xiji.service.SmsService;
import com.xiji.service.SmsService.SmsSendResult;
import com.xiji.service.UserService;
import com.xiji.utils.AvatarUtils;
import com.xiji.utils.JwtUtils;
import com.xiji.utils.LoginAttemptUtils;
import com.xiji.utils.PasswordUtils;
import com.xiji.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.Map;

/**
 * 手机端认证相关接口
 */
@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
@RequiredArgsConstructor
public class MobileAuthController {

    private final UserService userService;
    private final SmsService smsService;
    private final FamilyService familyService;
    private final FamilyMemberService familyMemberService;
    private final LoginAttemptUtils loginAttemptUtils;

    /**
     * 发送登录验证码
     * POST /send-code
     */
    @PostMapping("/send-code")
    public ResultVo sendLoginCode(@Valid @RequestBody SendSmsCodeRequest request) {
        String phone = request.getPhone();
        if (!PhoneUtil.isPhone(phone)) {
            return ResultVo.error("手机号格式不正确");
        }
        
        // 检查手机号是否已注册
        User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (user == null) {
            return ResultVo.error("该手机号未注册");
        }
        
        SmsSendResult result = smsService.sendSmsCode(phone, "login");
        if (result.isSuccess()) {
            if (result.getCode() != null) {
                // 开发模式：返回验证码给前端
                Map<String, String> data = new HashMap<>();
                data.put("code", result.getCode());
                data.put("message", "验证码已生成（开发模式，未发送短信）");
                return ResultVo.success("验证码已生成", data);
            } else {
                // 生产模式：发送短信成功
                return ResultVo.success("验证码已发送");
            }
        } else {
            return ResultVo.error("验证码发送失败，请稍后重试");
        }
    }

    /**
     * 登录
     * POST /login
     */
    @PostMapping("/login")
    public ResultVo login(@RequestBody MobileLoginRequest request) {
        String mode = StringUtils.isNotEmpty(request.getMode()) ? request.getMode() : "password";
        User user = null;
        String loginIdentifier = null;
        
        if ("code".equals(mode)) {
            // 验证码登录
            if (StringUtils.isEmpty(request.getPhone())) {
                return ResultVo.error("手机号不能为空");
            }
            if (!ValidationUtils.isValidPhone(request.getPhone())) {
                return ResultVo.error("手机号格式不正确");
            }
            loginIdentifier = request.getPhone();
            
            // 检查登录失败次数（防止暴力破解）
            if (loginAttemptUtils.isLoginBlocked(loginIdentifier)) {
                Long remainingTime = loginAttemptUtils.getRemainingLockTime(loginIdentifier);
                long minutes = remainingTime > 0 ? (remainingTime / 60) + 1 : 1;
                return ResultVo.error("登录失败次数过多，请" + minutes + "分钟后再试");
            }
            
            if (StringUtils.isEmpty(request.getCode())) {
                return ResultVo.error("验证码不能为空");
            }
            
            // 验证短信验证码
            if (!smsService.verifySmsCode(request.getPhone(), request.getCode(), "login")) {
                // 验证码错误，记录失败次数
                loginAttemptUtils.recordLoginFailure(loginIdentifier);
                return ResultVo.error("验证码错误或已过期");
            }
            
            // 根据手机号查询用户
            user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone()));
            if (user == null) {
                // 用户不存在也记录失败（防止手机号枚举攻击）
                loginAttemptUtils.recordLoginFailure(loginIdentifier);
                return ResultVo.error("该手机号未注册");
            }
            
            // 登录成功，清除失败记录
            loginAttemptUtils.clearLoginFailure(loginIdentifier);
        } else {
            // 密码登录
            if (StringUtils.isEmpty(request.getAccount())) {
                return ResultVo.error("账号不能为空");
            }
            loginIdentifier = request.getAccount();
            
            // 检查登录失败次数（防止暴力破解）
            if (loginAttemptUtils.isLoginBlocked(loginIdentifier)) {
                Long remainingTime = loginAttemptUtils.getRemainingLockTime(loginIdentifier);
                long minutes = remainingTime > 0 ? (remainingTime / 60) + 1 : 1;
                return ResultVo.error("登录失败次数过多，请" + minutes + "分钟后再试");
            }
            
            if (StringUtils.isEmpty(request.getPassword())) {
                return ResultVo.error("密码不能为空");
            }
            
            // 根据账号查询用户（支持手机号或用户名）
            user = userService.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getAccount())
                .or()
                .eq(User::getPhone, request.getAccount())
                .or()
                .eq(User::getName, request.getAccount()));
            if (user == null) {
                // 用户不存在也记录失败（防止账号枚举攻击）
                loginAttemptUtils.recordLoginFailure(loginIdentifier);
                return ResultVo.error("用户不存在");
            }
            
            // 验证密码
            if (!PasswordUtils.matches(request.getPassword(), user.getPassword())) {
                // 密码错误，记录失败次数
                loginAttemptUtils.recordLoginFailure(loginIdentifier);
                return ResultVo.error("密码错误");
            }
            
            // 登录成功，清除失败记录
            loginAttemptUtils.clearLoginFailure(loginIdentifier);
        }
        
        // 检查用户状态
        if (user.getStatus() != null && user.getStatus() == 1) {
            return ResultVo.error("用户已被禁用，请联系管理员解封");
        }
        
        // 构建登录响应
        MobileLoginResponse response = new MobileLoginResponse();
        MobileUserResponse userResponse = new MobileUserResponse();
        BeanUtils.copyProperties(user, userResponse);

        // 从当前家庭获取角色
        int role = 0; // 默认普通成员
        if (user.getCurrentFamilyId() != null) {
            Integer memberRole = familyMemberService.getMemberRole(user.getCurrentFamilyId(), user.getId());
            role = memberRole != null ? memberRole : 0;
        }
        userResponse.setRole(role);
        
        // 修改图片路径
        if (user.getAvatar() != null) {
            userResponse.setAvatar(AvatarUtils.processAvatarForResponse(user.getAvatar()));
        }
        
        // 设置昵称（使用name字段）
        userResponse.setNickname(user.getName());
        
        // 生成JWT Token（不再包含role，权限检查时从家庭成员表获取）
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", user.getUsername());
        claims.put("id", user.getId());
        String token = JwtUtils.generateJwt(claims);
        
        response.setToken(token);
        response.setUser(userResponse);
        
        return ResultVo.success("登录成功", response);
    }

    /**
     * 发送注册验证码
     * POST /register/send-code
     */
    @PostMapping("/register/send-code")
    public ResultVo sendRegisterCode(@Valid @RequestBody SendSmsCodeRequest request) {
        String phone = request.getPhone();
        if (!PhoneUtil.isPhone(phone)) {
            return ResultVo.error("手机号格式不正确");
        }
        
        // 检查手机号是否已注册
        if (userService.getOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone)) != null) {
            return ResultVo.error("该手机号已被注册");
        }
        
        SmsSendResult result = smsService.sendSmsCode(phone, "register");
        if (result.isSuccess()) {
            if (result.getCode() != null) {
                // 开发模式：返回验证码给前端
                Map<String, String> data = new HashMap<>();
                data.put("code", result.getCode());
                data.put("message", "验证码已生成（开发模式，未发送短信）");
                return ResultVo.success("验证码已生成", data);
            } else {
                // 生产模式：发送短信成功
                return ResultVo.success("验证码已发送");
            }
        } else {
            return ResultVo.error("验证码发送失败，请稍后重试");
        }
    }

    /**
     * 注册
     * POST /register
     */
    @PostMapping("/register")
    @Transactional(rollbackFor = Exception.class)
    public ResultVo register(@Valid @RequestBody MobileRegisterRequest request) {
        if (!PhoneUtil.isPhone(request.getPhone())) {
            return ResultVo.error("手机号格式不正确");
        }
        // 验证短信验证码
        if (!smsService.verifySmsCode(request.getPhone(), request.getCode(), "register")) {
            return ResultVo.error("验证码错误或已过期");
        }
        // 检查手机号是否已注册
        if (userService.getOne(new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone())) != null) {
            return ResultVo.error("该手机号已被注册");
        }
        
        try {
            // 创建用户
            User user = new User();
            user.setUsername(request.getPhone()); // 使用手机号作为用户名
            user.setName(request.getNickname());
            user.setPhone(request.getPhone());
            user.setPassword(PasswordUtils.encode(request.getPassword()));
            user.setStatus(0); // 默认正常状态
            // 创建时间和更新时间由MyBatis-Plus自动填充
            if (!userService.save(user)) {
                return ResultVo.error("注册失败");
            }
            
            // 注册成功后，自动创建家庭
            Long familyId = null;
            int role = 0; // 默认普通成员
            
            String familyName = request.getNickname() + "的家庭";
            Family family = familyService.createFamily(familyName, user.getId());
            familyId = family.getId();
            // 将用户加入家庭（通过family_member表），创建者自动成为管理员
            familyMemberService.addMemberToFamily(familyId, user.getId(), 1);
            role = 1; // 创建者自动成为管理员
            // 设置当前选择的家庭
            user.setCurrentFamilyId(familyId);
            userService.updateById(user);
            log.info("用户注册成功并创建家庭，用户ID={}，家庭ID={}", user.getId(), familyId);
            
            // 生成JWT Token（不再包含role，权限检查时从家庭成员表获取）
            Map<String, Object> claims = new HashMap<>();
            claims.put("username", user.getUsername());
            claims.put("id", user.getId());
            String token = JwtUtils.generateJwt(claims);
            
            // 构建响应
            MobileLoginResponse response = new MobileLoginResponse();
            response.setToken(token);
            
            MobileUserResponse userResponse = new MobileUserResponse();
            userResponse.setId(user.getId());
            userResponse.setNickname(user.getName());
            userResponse.setPhone(user.getPhone());
            userResponse.setRole(role);
            userResponse.setEmail(user.getEmail());
            response.setUser(userResponse);
            
            return ResultVo.success("注册成功", response);
        } catch (Exception e) {
            log.error("用户注册失败，手机号={}", request.getPhone(), e);
            // 手动标记事务回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            // 返回统一格式的错误信息
            return ResultVo.error("注册失败：" + e.getMessage());
        }
    }

    /**
     * 发送忘记密码验证码
     * POST /forgot-password/send-code
     */
    @PostMapping("/forgot-password/send-code")
    public ResultVo sendForgotPasswordCode(@Valid @RequestBody SendSmsCodeRequest request) {
        String phone = request.getPhone();
        if (!PhoneUtil.isPhone(phone)) {
            return ResultVo.error("手机号格式不正确");
        }
        
        // 检查手机号是否已注册
        User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (user == null) {
            return ResultVo.error("该手机号未注册");
        }
        
        SmsSendResult result = smsService.sendSmsCode(phone, "forgot-password");
        if (result.isSuccess()) {
            if (result.getCode() != null) {
                // 开发模式：返回验证码给前端
                Map<String, String> data = new HashMap<>();
                data.put("code", result.getCode());
                data.put("message", "验证码已生成（开发模式，未发送短信）");
                return ResultVo.success("验证码已生成", data);
            } else {
                // 生产模式：发送短信成功
                return ResultVo.success("验证码已发送");
            }
        } else {
            return ResultVo.error("验证码发送失败，请稍后重试");
        }
    }

    /**
     * 重置密码
     * POST /forgot-password/reset
     */
    @PostMapping("/forgot-password/reset")
    public ResultVo resetPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        if (!PhoneUtil.isPhone(request.getPhone())) {
            return ResultVo.error("手机号格式不正确");
        }
        // 验证短信验证码（使用forgot-password类型）
        if (!smsService.verifySmsCode(request.getPhone(), request.getCode(), "forgot-password")) {
            return ResultVo.error("验证码错误或已过期");
        }
        
        // 查询用户
        User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone()));
        if (user == null) {
            return ResultVo.error("该手机号未注册");
        }
        
        // 更新密码
        user.setPassword(PasswordUtils.encode(request.getNewPassword()));
        // 更新时间由MyBatis-Plus自动填充
        
        if (userService.updateById(user)) {
            return ResultVo.success("密码重置成功");
        } else {
            return ResultVo.error("密码重置失败");
        }
    }
}

