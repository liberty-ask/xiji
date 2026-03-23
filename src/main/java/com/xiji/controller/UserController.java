package com.xiji.controller;

import cn.hutool.core.codec.Base64Encoder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiji.config.CustomConfig;
import com.xiji.common.annotation.CheckPermission;
import com.xiji.common.annotation.OperationLog;
import com.xiji.entity.dto.request.LoginUser;
import com.xiji.entity.dto.request.RegisterUser;
import com.xiji.entity.dto.request.UserPassword;
import com.xiji.entity.dto.request.PageParam;
import com.xiji.entity.domain.User;
import com.xiji.entity.dto.response.UserLoginResponse;
import com.xiji.service.CaptchaService;
import com.xiji.service.SmsService;
import com.xiji.service.SmsService.SmsSendResult;
import com.xiji.service.UserService;
import com.xiji.utils.AvatarUtils;
import com.xiji.utils.JwtUtils;
import com.xiji.utils.LoginAttemptUtils;
import com.xiji.utils.PasswordUtils;
import com.xiji.common.response.ResultVo;
import com.xiji.utils.ValidationUtils;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 用户管理
 */
@RestController
@RequestMapping("/api/v1/user")
@Slf4j
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CaptchaService captchaService;
    private final SmsService smsService;
    private final LoginAttemptUtils loginAttemptUtils;
    @Resource
    private DefaultKaptcha defaultKaptcha;

    /**
     * 获取验证码
     */
    @PostMapping("/captcha")
    public ResultVo imageCode(HttpServletRequest request) {
        log.info("获取验证码");
        //生成的验证码
        String code = defaultKaptcha.createText();
        // 生成验证码token并存入Redis
        String captchaToken = captchaService.generateCaptchaToken(code);
        
        // 生成图片,转换为bas64字符串
        BufferedImage bufferedImage = defaultKaptcha.createImage(code);
        ByteArrayOutputStream outputStream = null;
        try {
            outputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "jpg", outputStream);
            String base64 = Base64Encoder.encode(outputStream.toByteArray());
            String codeImage = "data:image/jpg;base64," + base64.replaceAll("\r\n", "");
            
            // 返回验证码图片和token
            Map<String, Object> result = new HashMap<>();
            result.put("image", codeImage);
            result.put("token", captchaToken);
            return ResultVo.success("获取成功", result);
        } catch (IOException e) {
            log.error("生成验证码图片失败", e);
            return ResultVo.error("生成验证码失败");
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    log.error("关闭输出流失败", e);
                }
            }
        }
    }


    /**
     * 发送短信验证码（注册）
     */
    @PostMapping("/sms/register")
    public ResultVo sendRegisterSmsCode(@RequestParam String phone) {
        if (StringUtils.isEmpty(phone)) {
            return ResultVo.error("手机号不能为空");
        }
        if (!ValidationUtils.isValidPhone(phone)) {
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
                return ResultVo.success("短信验证码发送成功");
            }
        } else {
            return ResultVo.error("短信验证码发送失败，请稍后重试");
        }
    }
    
    /**
     * 发送短信验证码（登录）
     */
    @PostMapping("/sms/login")
    public ResultVo sendLoginSmsCode(@RequestParam String phone) {
        if (StringUtils.isEmpty(phone)) {
            return ResultVo.error("手机号不能为空");
        }
        if (!ValidationUtils.isValidPhone(phone)) {
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
                return ResultVo.success("短信验证码发送成功");
            }
        } else {
            return ResultVo.error("短信验证码发送失败，请稍后重试");
        }
    }
    
    /**
     * 注册
     */
    @PostMapping("/register")
    public ResultVo register(@Valid @RequestBody RegisterUser userDto) {
        // 参数验证
        if (StringUtils.isEmpty(userDto.getUsername())) {
            return ResultVo.error("用户名不能为空");
        }
        if (!ValidationUtils.isValidUsername(userDto.getUsername())) {
            return ResultVo.error("用户名格式不正确，只能包含字母、数字、下划线，长度3-20位");
        }
        if (StringUtils.isEmpty(userDto.getPassword())) {
            return ResultVo.error("密码不能为空");
        }
        if (!ValidationUtils.isValidPassword(userDto.getPassword())) {
            return ResultVo.error("密码长度至少6位，最多50位");
        }
        if (StringUtils.isEmpty(userDto.getConfirmPassword())) {
            return ResultVo.error("确认密码不能为空");
        }
        if (!userDto.getPassword().equals(userDto.getConfirmPassword())) {
            return ResultVo.error("两次输入的密码不一致");
        }
        
        // 手机号和邮箱至少提供一个
        boolean hasPhone = StringUtils.isNotEmpty(userDto.getPhone());
        boolean hasEmail = StringUtils.isNotEmpty(userDto.getEmail());
        
        if (!hasPhone && !hasEmail) {
            return ResultVo.error("手机号和邮箱至少填写一个");
        }
        
        // 验证手机号
        if (hasPhone) {
            if (!ValidationUtils.isValidPhone(userDto.getPhone())) {
                return ResultVo.error("手机号格式不正确");
            }
            // 验证短信验证码
            if (StringUtils.isEmpty(userDto.getSmsCode())) {
                return ResultVo.error("短信验证码不能为空");
            }
            if (!smsService.verifySmsCode(userDto.getPhone(), userDto.getSmsCode(), "register")) {
                return ResultVo.error("短信验证码错误或已过期");
            }
            // 判断手机号是否存在
            if (userService.getOne(new LambdaQueryWrapper<User>().eq(User::getPhone, userDto.getPhone())) != null) {
                return ResultVo.error("该手机号已被注册");
            }
        }
        
        // 验证邮箱
        if (hasEmail) {
            if (!ValidationUtils.isValidEmail(userDto.getEmail())) {
                return ResultVo.error("邮箱格式不正确");
            }
            // 判断邮箱是否存在
            if (userService.getOne(new LambdaQueryWrapper<User>().eq(User::getEmail, userDto.getEmail())) != null) {
                return ResultVo.error("该邮箱已被注册");
            }
        }
        
        //判断用户名是否存在
        if (userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, userDto.getUsername())) != null) {
            return ResultVo.error("用户名已存在");
        }
        
        //密码BCrypt加密
        userDto.setPassword(PasswordUtils.encode(userDto.getPassword()));
        // 把UserDto转换为User对象
        User user = new User();
        BeanUtils.copyProperties(userDto, user);
        // 图片处理
        if (user.getAvatar() != null) {
            String avatar = AvatarUtils.processAvatarForStorage(user.getAvatar());
            user.setAvatar(avatar);
            log.info("图片处理：{}", user.getAvatar());
        }
        // 创建时间和更新时间由MyBatis-Plus自动填充
        //保存用户
        if (userService.save(user)) {
            return ResultVo.success("注册成功");
        }else {
            return ResultVo.error("注册失败");
        }
    }


    /**
     *  登录
     */
//    @OperationLog(description = "用户登录")
    @PostMapping("/login")
    public ResultVo login(@RequestBody LoginUser loginUser, HttpServletRequest request) {
        // 判断登录方式
        String loginType = StringUtils.isNotEmpty(loginUser.getLoginType()) 
            ? loginUser.getLoginType() 
            : "password"; // 默认密码登录
        
        User u = null;
        
        if ("sms".equals(loginType)) {
            // 短信验证码登录
            if (StringUtils.isEmpty(loginUser.getPhone())) {
                return ResultVo.error("手机号不能为空");
            }
            if (!ValidationUtils.isValidPhone(loginUser.getPhone())) {
                return ResultVo.error("手机号格式不正确");
            }
            
            // 检查登录失败次数（防止暴力破解）
            if (loginAttemptUtils.isLoginBlocked(loginUser.getPhone())) {
                Long remainingTime = loginAttemptUtils.getRemainingLockTime(loginUser.getPhone());
                long minutes = remainingTime > 0 ? (remainingTime / 60) + 1 : 1;
                return ResultVo.error("登录失败次数过多，请" + minutes + "分钟后再试");
            }
            
            if (StringUtils.isEmpty(loginUser.getSmsCode())) {
                return ResultVo.error("短信验证码不能为空");
            }
            
            // 验证短信验证码
            if (!smsService.verifySmsCode(loginUser.getPhone(), loginUser.getSmsCode(), "login")) {
                // 验证码错误，记录失败次数
                loginAttemptUtils.recordLoginFailure(loginUser.getPhone());
                return ResultVo.error("短信验证码错误或已过期");
            }
            
            // 根据手机号查询用户
            u = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getPhone, loginUser.getPhone()));
            if (u == null) {
                // 用户不存在也记录失败（防止手机号枚举攻击）
                loginAttemptUtils.recordLoginFailure(loginUser.getPhone());
                return ResultVo.error("该手机号未注册");
            }
            
            // 登录成功，清除失败记录
            loginAttemptUtils.clearLoginFailure(loginUser.getPhone());
        } else {
            // 密码登录
            // 验证图片验证码（使用Redis）
            if (StringUtils.isEmpty(loginUser.getCaptchaToken()) || StringUtils.isEmpty(loginUser.getCaptcha())) {
                return ResultVo.error("图片验证码不能为空");
            }
            if (!captchaService.verifyCaptcha(loginUser.getCaptchaToken(), loginUser.getCaptcha())) {
                log.warn("图片验证码验证失败：用户输入={}", loginUser.getCaptcha());
                return ResultVo.error("图片验证码错误或已过期");
            }
            
            if (StringUtils.isEmpty(loginUser.getUsername())) {
                return ResultVo.error("用户名不能为空");
            }
            
            // 检查登录失败次数（防止暴力破解）
            if (loginAttemptUtils.isLoginBlocked(loginUser.getUsername())) {
                Long remainingTime = loginAttemptUtils.getRemainingLockTime(loginUser.getUsername());
                long minutes = remainingTime > 0 ? (remainingTime / 60) + 1 : 1;
                return ResultVo.error("登录失败次数过多，请" + minutes + "分钟后再试");
            }
            
            // 根据用户名查询用户
            u = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, loginUser.getUsername()));
            if (u == null) {
                // 用户不存在也记录失败（防止用户名枚举攻击）
                loginAttemptUtils.recordLoginFailure(loginUser.getUsername());
                return ResultVo.error("用户不存在");
            }
            
            // 使用BCrypt验证密码
            if (StringUtils.isEmpty(loginUser.getPassword())) {
                return ResultVo.error("密码不能为空");
            }
            if (!PasswordUtils.matches(loginUser.getPassword(), u.getPassword())) {
                // 密码错误，记录失败次数
                loginAttemptUtils.recordLoginFailure(loginUser.getUsername());
                return ResultVo.error("密码错误");
            }
            
            // 登录成功，清除失败记录
            loginAttemptUtils.clearLoginFailure(loginUser.getUsername());
        }
        
        // 检查用户状态
        if (u.getStatus() == 1) {
            return ResultVo.error("用户已被禁用，请联系管理员解封");
        }
        
        // 角色信息已移至家庭成员关联表，不再在用户表中检查
        // 构建登录响应
        UserLoginResponse response = new UserLoginResponse();
        BeanUtils.copyProperties(u, response);
        // 角色信息已移至家庭成员关联表，不在用户表中
        response.setRole(null);
        // 修改图片路径
        if (u.getAvatar() != null) {
            response.setAvatar(AvatarUtils.processAvatarForResponse(u.getAvatar()));
        }
        // 设置token（不再包含role，权限检查时从家庭成员表获取）
        Map<String, Object> data = new HashMap<>();
        data.put("username", response.getUsername());
        data.put("id", response.getId());
        String token = JwtUtils.generateJwt(data);
        // 设置token
        response.setToken(token);
        // 返回结果
        return ResultVo.success("登录成功", response);
    }

    /**
     * 修改密码
     * @param user 用户
     */
    @OperationLog(description = "用户修改密码")
    @PostMapping("/changePassword")
    public ResultVo changePassword(@RequestBody UserPassword user, HttpServletRequest request) {
        // 从JWT token中获取当前用户ID
        Long currentUserId = getCurrentUserIdFromToken(request);
        if (currentUserId == null) {
            return ResultVo.error("用户未登录");
        }
        
        // 参数验证
        if (StringUtils.isEmpty(user.getPassword())) {
            return ResultVo.error("旧密码不能为空");
        }
        if (StringUtils.isEmpty(user.getNewPassword())) {
            return ResultVo.error("新密码不能为空");
        }
        if (!ValidationUtils.isValidPassword(user.getNewPassword())) {
            return ResultVo.error("新密码长度至少6位，最多50位");
        }
        if (user.getPassword().equals(user.getNewPassword())) {
            return ResultVo.error("新密码不能与旧密码相同");
        }
        
        User u = userService.getById(currentUserId);
        if (u == null) {
            return ResultVo.error("用户不存在");
        }
        
        // 使用BCrypt验证旧密码
        if (!PasswordUtils.matches(user.getPassword(), u.getPassword())) {
            return ResultVo.error("旧密码错误");
        }
        // 更新密码：对新密码进行BCrypt加密，并更新到数据库中
        String newPasswordHash = PasswordUtils.encode(user.getNewPassword());
        // 设置新密码
        u.setPassword(newPasswordHash);
        log.info("修改密码：用户ID={}", u.getId());
        if (userService.updateById(u)) {
            return ResultVo.success("修改成功");
        }else {
            return ResultVo.error("修改失败");
        }
    }
    
    /**
     * 从请求中获取当前用户ID
     */
    private Long getCurrentUserIdFromToken(HttpServletRequest request) {
        String token = request.getHeader("token");
        if (token == null || token.isEmpty()) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }

        if (token == null || token.isEmpty()) {
            return null;
        }

        io.jsonwebtoken.Claims claims = JwtUtils.parseJwt(token);
        if (claims == null) {
            return null;
        }

        Object idObj = claims.get("id");
        if (idObj == null) {
            return null;
        }

        if (idObj instanceof Long) {
            return (Long) idObj;
        } else if (idObj instanceof Number) {
            return ((Number) idObj).longValue();
        } else if (idObj instanceof String) {
            try {
                return Long.parseLong((String) idObj);
            } catch (NumberFormatException e) {
                log.warn("无法解析用户ID: {}", idObj);
                return null;
            }
        }

        return null;
    }

    /**
     * 新增
     */
    @OperationLog(description = "新增用户")
    @CheckPermission
    @PostMapping
    public ResultVo add(@RequestBody User user) {
        // 参数验证
        if (StringUtils.isEmpty(user.getUsername())) {
            return ResultVo.error("用户名不能为空");
        }
        if (!ValidationUtils.isValidUsername(user.getUsername())) {
            return ResultVo.error("用户名格式不正确");
        }
        if (StringUtils.isEmpty(user.getPassword())) {
            return ResultVo.error("密码不能为空");
        }
        if (!ValidationUtils.isValidPassword(user.getPassword())) {
            return ResultVo.error("密码长度至少6位");
        }
        if (user.getEmail() != null && !ValidationUtils.isValidEmail(user.getEmail())) {
            return ResultVo.error("邮箱格式不正确");
        }
        
        // 判断用户名是否存在
        if (userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername())) != null) {
            return ResultVo.error("用户名已存在");
        }
        
        // 密码BCrypt加密
        user.setPassword(PasswordUtils.encode(user.getPassword()));
        // 图片处理
        if (user.getAvatar() != null) {
            String avatar = AvatarUtils.processAvatarForStorage(user.getAvatar());
            user.setAvatar(avatar);
            log.info("图片处理：{}", user.getAvatar());
        }
        if (userService.save(user)) {
            return ResultVo.success("新增成功");
        }else {
            return ResultVo.error("新增失败");
        }
    }

    /**
     * 编辑
     */
    @OperationLog(description = "编辑用户")
    @CheckPermission
    @PutMapping
    public ResultVo edit(@RequestBody User user) {
        // 如果提供了新密码且不是BCrypt格式，则加密
        if (user.getPassword() != null && !PasswordUtils.isBCryptFormat(user.getPassword())){
            // 密码BCrypt加密
            user.setPassword(PasswordUtils.encode(user.getPassword()));
        }
        // 更新时间由MyBatis-Plus自动填充
        // 图片处理
        if (user.getAvatar() != null) {
            String avatar = AvatarUtils.processAvatarForStorage(user.getAvatar());
            user.setAvatar(avatar);
            log.info("图片处理：{}", user.getAvatar());
        }
        if (userService.updateById(user)) {
            return ResultVo.success("编辑成功");
        }else {
            return ResultVo.error("编辑失败");
        }
    }

    /**
     * 删除
     */
    @OperationLog(description = "删除用户")
    @CheckPermission
    @DeleteMapping("/{userId}")
    public ResultVo delete(@PathVariable Long userId) {
        if (userService.removeById(userId)) {
            return ResultVo.success("删除成功");
        }else {
            return ResultVo.error("删除失败");
        }
    }

    /**
     * 查询用户列表
     * @param param 分页参数
     * @return 分页数据
     */
    @GetMapping
    public ResultVo getList(PageParam param) {
        // 创建LambdaQueryWrapper
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        // 角色信息已移至家庭成员关联表，这里查询所有用户
        if (StringUtils.isNotEmpty(param.getValue())) {
            queryWrapper.like(User::getName, param.getValue());
        }
        // 创建分页对象
        Page<User> page = new Page<>(Optional.ofNullable(param.getCurrentPage()).orElse(1), param.getPageSize());
        // 执行分页查询
        IPage<User> pageList = userService.page(page, queryWrapper);
        // 返回结果
        return ResultVo.success(pageList);
    }

    /**
     * 查询管理员列表
     * 注意：由于角色信息已移至家庭成员关联表，此接口已废弃。
     * 如需查询特定家庭的管理员，请使用家庭成员相关接口。
     */
    @GetMapping("/admin")
    @Deprecated
    public ResultVo getAdminList(PageParam param) {
        // 角色信息已移至家庭成员关联表，无法再按用户表中的role查询管理员
        // 此接口保留以兼容旧代码，但实际返回空列表
        // 如需查询管理员，请使用家庭成员相关接口，传入家庭ID参数
        return ResultVo.success("角色信息已移至家庭成员关联表，请使用家庭成员相关接口查询管理员");
    }

}
