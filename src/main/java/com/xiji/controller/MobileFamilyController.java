package com.xiji.controller;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiji.common.annotation.CheckPermission;
import com.xiji.common.annotation.OperationLog;
import com.xiji.common.response.ResultVo;
import com.xiji.entity.domain.Family;
import com.xiji.entity.domain.FamilyApplication;
import com.xiji.entity.domain.User;
import com.xiji.entity.domain.FamilyMember;
import com.xiji.entity.dto.request.ApplyToJoinFamilyRequest;
import com.xiji.entity.dto.request.ChangePasswordRequest;
import com.xiji.entity.dto.request.ProcessApplicationRequest;
import com.xiji.entity.dto.request.SwitchFamilyRequest;
import com.xiji.entity.dto.request.UpdateProfileRequest;
import com.xiji.entity.dto.response.MobileFamilyMemberResponse;
import com.xiji.entity.dto.response.MobileFamilyResponse;
import com.xiji.entity.dto.response.MobilePendingApplicationResponse;
import com.xiji.entity.dto.response.MobileProfileResponse;
import com.xiji.service.FamilyApplicationService;
import com.xiji.service.FamilyMemberService;
import com.xiji.service.FamilyService;
import com.xiji.service.UserService;
import com.xiji.config.CustomConfig;
import com.xiji.utils.AvatarUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 手机端家庭相关接口
 */
@RestController
@RequestMapping("/api/v1")
@Slf4j
@RequiredArgsConstructor
public class MobileFamilyController extends BaseController {

    private final UserService userService;
    private final FamilyService familyService;
    private final FamilyApplicationService familyApplicationService;
    private final FamilyMemberService familyMemberService;

    /**
     * 获取用户的所有家庭列表
     * GET /families/list
     */
    @GetMapping("/families/list")
    public ResultVo getUserFamilies(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }

        // 获取用户的所有家庭ID
        List<Long> familyIds = familyMemberService.getUserFamilyIds(userId);
        if (familyIds.isEmpty()) {
            return ResultVo.success(new ArrayList<>());
        }

        // 查询家庭信息
        List<Family> families = familyService.listByIds(familyIds);
        
        // 获取当前选择的家庭ID
        Long currentFamilyId = getCurrentFamilyId(userId);

        // 转换为响应DTO
        List<MobileFamilyResponse> responseList = families.stream().map(family -> {
            MobileFamilyResponse response = new MobileFamilyResponse();
            response.setId(String.valueOf(family.getId()));
            response.setName(family.getName());
            response.setIsCurrent(family.getId().equals(currentFamilyId));
            
            // 获取用户在家庭中的角色
            Integer role = familyMemberService.getMemberRole(family.getId(), userId);
            response.setRole(role);
            
            return response;
        }).collect(Collectors.toList());

        return ResultVo.success(responseList);
    }

    /**
     * 获取用户当前家庭信息
     */
    @GetMapping("/families/detail")
    public ResultVo getCurrentFamily(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }
        Long currentFamilyId = getCurrentFamilyId(userId);
        if (currentFamilyId == null) {
            return ResultVo.error("请先选择家庭");
        }
        Family family = familyService.getById(currentFamilyId);
        if (family == null) {
            return ResultVo.error("家庭不存在");
        }
        return ResultVo.success(family);

    }

    /**
     * 切换家庭
     * POST /families/switch
     */
    @OperationLog(description = "切换家庭")
    @PostMapping("/families/switch")
    public ResultVo switchFamily(@RequestBody SwitchFamilyRequest request, HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId(httpRequest);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }

        if (request.getFamilyId() == null) {
            return ResultVo.error("家庭ID不能为空");
        }

        Long familyId = request.getFamilyId();

        // 检查用户是否属于该家庭
        if (!familyMemberService.isMemberOfFamily(familyId, userId)) {
            return ResultVo.error("您不属于该家庭");
        }

        // 更新当前选择的家庭
        User user = userService.getById(userId);
        if (user == null) {
            return ResultVo.error("用户不存在");
        }

        user.setCurrentFamilyId(familyId);
        // 更新时间由MyBatis-Plus自动填充

        if (userService.updateById(user)) {
            return ResultVo.success("切换成功");
        } else {
            return ResultVo.error("切换失败");
        }
    }

    /**
     * 获取家庭成员列表
     * GET /families/members
     */
    @GetMapping("/families/members")
    public ResultVo getFamilyMembers(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }

        // 获取当前选择的家庭ID
        User currentUser = userService.getById(userId);
        if (currentUser == null) {
            return ResultVo.error("用户不存在");
        }
        
        if (currentUser.getCurrentFamilyId() == null) {
            return ResultVo.error("请先选择家庭");
        }

        // 根据家庭ID查询所有成员（通过family_member表）
        List<FamilyMember> members = familyMemberService.list(
            new LambdaQueryWrapper<FamilyMember>()
                .eq(FamilyMember::getFamilyId, currentUser.getCurrentFamilyId()));

        // 获取成员的用户信息
        List<Long> memberUserIds = members.stream()
            .map(FamilyMember::getUserId)
            .collect(Collectors.toList());
        
        if (memberUserIds.isEmpty()) {
            return ResultVo.success(new ArrayList<>());
        }

        List<User> users = userService.listByIds(memberUserIds);
        Map<Long, FamilyMember> memberMap = members.stream()
            .collect(Collectors.toMap(FamilyMember::getUserId, m -> m));

        List<MobileFamilyMemberResponse> responseList = users.stream()
            .filter(user -> user.getStatus() == null || user.getStatus() == 0) // 只查询正常状态的用户
            .map(user -> {
                MobileFamilyMemberResponse member = new MobileFamilyMemberResponse();
                member.setId(String.valueOf(user.getId()));
                member.setName(user.getName() != null ? user.getName() : user.getUsername());
                
                // 从family_member表获取角色
                FamilyMember memberInfo = memberMap.get(user.getId());
                if (memberInfo == null) {
                    member.setRole(0);
                }else {
                    member.setRole(memberInfo.getRole());
                }

                if (user.getAvatar() != null) {
                    member.setAvatar(AvatarUtils.processAvatarForResponse(user.getAvatar()));
                }
                return member;
            })
            .collect(Collectors.toList());

        return ResultVo.success(responseList);
    }

    /**
     * 获取待审核申请列表
     * GET /families/pending-applications
     */
    @GetMapping("/families/pending-applications")
    @CheckPermission
    public ResultVo getPendingApplications(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }

        // 获取当前选择的家庭ID
        User currentUser = userService.getById(userId);
        if (currentUser == null || currentUser.getCurrentFamilyId() == null) {
            return ResultVo.success(new ArrayList<>());
        }

        // 检查是否为家庭管理员
        Integer role = familyMemberService.getMemberRole(currentUser.getCurrentFamilyId(), userId);
        if (role == null || role != 1) {
            return ResultVo.error("只有家庭管理员可以查看申请");
        }

        // 查询待审核申请
        List<FamilyApplication> applications = familyApplicationService.getPendingApplications(currentUser.getCurrentFamilyId());
        
        // 转换为响应DTO
        List<MobilePendingApplicationResponse> responseList = applications.stream().map(app -> {
            MobilePendingApplicationResponse response = new MobilePendingApplicationResponse();
            response.setId(app.getId());
            
            // 查询申请人信息
            User applicant = userService.getById(app.getUserId());
            if (applicant != null) {
                response.setName(applicant.getName() != null ? applicant.getName() : applicant.getUsername());
                if (applicant.getAvatar() != null) {
                    response.setAvatar(AvatarUtils.processAvatarForResponse(applicant.getAvatar()));
                }
            }
            
            // 格式化时间
            if (app.getCreatedAt() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
                response.setTime(app.getCreatedAt().format(formatter));
            }
            
            response.setNote(app.getNote());
            response.setIsNew(true); // 简化处理，可以根据时间判断
            
            return response;
        }).collect(Collectors.toList());

        return ResultVo.success(responseList);
    }

    /**
     * 处理申请（批准或拒绝）
     * POST /families/process-application
     */
    @OperationLog(description = "处理成员申请")
    @PostMapping("/families/process-application")
    @CheckPermission
    public ResultVo processApplication(@RequestBody ProcessApplicationRequest request, HttpServletRequest httpRequest) {
        Long id = request.getId();
        String action = request.getAction();
        
        if (ObjectUtil.isEmpty(id)) {
            return ResultVo.error("申请ID不能为空");
        }
        if (StringUtils.isEmpty(action)) {
            return ResultVo.error("操作类型不能为空");
        }
        if (!"approve".equals(action) && !"reject".equals(action)) {
            return ResultVo.error("操作类型不正确，应为approve或reject");
        }

        // 检查权限：只有家庭管理员可以处理申请
        Long userId = getCurrentUserId(httpRequest);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }
        
        FamilyApplication application = familyApplicationService.getById(id);
        if (application == null) {
            return ResultVo.error("申请不存在");
        }
        
        // 验证是否为该家庭的管理员
        User currentUser = userService.getById(userId);
        if (currentUser == null) {
            return ResultVo.error("用户不存在");
        }
        
        Integer role = familyMemberService.getMemberRole(application.getFamilyId(), userId);
        if (role == null || role != 1) {
            return ResultVo.error("只有家庭管理员可以处理申请");
        }

        // 处理申请
        Integer status = "approve".equals(action) ? 1 : 2;
        boolean success = familyApplicationService.processApplication(id, status);
        
        if (success) {
            return ResultVo.success("操作成功");
        } else {
            return ResultVo.error("操作失败");
        }
    }

    /**
     * 获取用户信息
     * GET /user/profile
     */
    @GetMapping("/user/profile")
    public ResultVo getProfile(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }

        User user = userService.getById(userId);
        if (user == null) {
            return ResultVo.error("用户不存在");
        }

        MobileProfileResponse response = new MobileProfileResponse();
        response.setNickname(user.getName() != null ? user.getName() : user.getUsername());
        response.setEmail(user.getEmail());
        // 从当前家庭获取角色
        int role = 0; // 默认普通成员
        if (user.getCurrentFamilyId() != null) {
            Integer memberRole = familyMemberService.getMemberRole(user.getCurrentFamilyId(), userId);
            role = memberRole != null ? memberRole : 0;
        }
        response.setRole(role);
        if (user.getAvatar() != null) {
            response.setAvatar(AvatarUtils.processAvatarForResponse(user.getAvatar()));
        }
        
        // 设置当前选择的家庭ID
        if (user.getCurrentFamilyId() != null) {
            response.setFamilyId(String.valueOf(user.getCurrentFamilyId()));
            
            // 如果是家庭管理员，返回待审核申请数量
            if (role == 1) {
                List<FamilyApplication> pendingApps = familyApplicationService.getPendingApplications(user.getCurrentFamilyId());
                response.setAuditCount(pendingApps.size());
            }
        } else {
            response.setFamilyId(null);
        }

        return ResultVo.success(response);
    }

    /**
     * 修改密码
     * POST /user/change-password
     */
    @OperationLog(description = "修改密码")
    @PostMapping("/user/change-password")
    public ResultVo changePassword(@Valid @RequestBody ChangePasswordRequest request, HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId(httpRequest);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }

        String oldPassword = request.getOldPassword();
        String newPassword = request.getNewPassword();

        if (StringUtils.isEmpty(oldPassword)) {
            return ResultVo.error("旧密码不能为空");
        }
        if (StringUtils.isEmpty(newPassword)) {
            return ResultVo.error("新密码不能为空");
        }

        User user = userService.getById(userId);
        if (user == null) {
            return ResultVo.error("用户不存在");
        }

        // 验证旧密码
        if (!com.xiji.utils.PasswordUtils.matches(oldPassword, user.getPassword())) {
            return ResultVo.error("旧密码错误");
        }

        // 更新密码
        user.setPassword(com.xiji.utils.PasswordUtils.encode(newPassword));
        // 更新时间由MyBatis-Plus自动填充
        
        if (userService.updateById(user)) {
            return ResultVo.success("密码修改成功");
        } else {
            return ResultVo.error("密码修改失败");
        }
    }

    /**
     * 退出家庭
     * POST /families/exit
     */
    @OperationLog(description = "退出家庭")
    @PostMapping("/families/exit")
    public ResultVo exitFamily(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }

        User user = userService.getById(userId);
        if (user == null) {
            return ResultVo.error("用户不存在");
        }
        
        if (user.getCurrentFamilyId() == null) {
            return ResultVo.error("您还没有选择家庭");
        }
        
        Long familyId = user.getCurrentFamilyId();
        
        // 检查是否为家庭管理员（管理员不能退出，需要先转移管理权）
        Integer role = familyMemberService.getMemberRole(familyId, userId);
        if (role != null && role == 1) {
            return ResultVo.error("家庭管理员不能退出，请先转移管理权");
        }
        
        // 从家庭中移除用户
        boolean success = familyMemberService.removeMemberFromFamily(familyId, userId);
        
        if (success) {
            // 如果退出的是当前选择的家庭，清空当前家庭ID
            if (user.getCurrentFamilyId().equals(familyId)) {
                // 切换到其他家庭（如果有）
                List<Long> otherFamilyIds = familyMemberService.getUserFamilyIds(userId);
                if (!otherFamilyIds.isEmpty()) {
                    user.setCurrentFamilyId(otherFamilyIds.get(0));
                } else {
                    user.setCurrentFamilyId(null);
                }
                // 更新时间由MyBatis-Plus自动填充
                userService.updateById(user);
            }
            return ResultVo.success("已退出家庭");
        } else {
            return ResultVo.error("退出家庭失败");
        }
    }
    
    /**
     * 申请加入家庭
     * POST /families/apply
     */
    @OperationLog(description = "申请加入家庭")
    @PostMapping("/families/apply")
    public ResultVo applyToJoinFamily(@Valid @RequestBody ApplyToJoinFamilyRequest request, HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId(httpRequest);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }
        
        if (request.getFamilyId() == null) {
            return ResultVo.error("家庭ID不能为空");
        }
        
        Long familyId = request.getFamilyId();
        String note = request.getNote();
        
        // 检查家庭是否存在
        Family family = familyService.getById(familyId);
        if (family == null) {
            return ResultVo.error("家庭不存在");
        }
        
        // 检查用户是否已经在家庭中
        User user = userService.getById(userId);
        if (user == null) {
            return ResultVo.error("用户不存在");
        }
        if (familyMemberService.isMemberOfFamily(familyId, userId)) {
            return ResultVo.error("您已经是该家庭的成员");
        }
        
        try {
            boolean success = familyApplicationService.applyToJoinFamily(familyId, userId, note);
            if (success) {
                return ResultVo.success("申请已提交，请等待审核");
            } else {
                return ResultVo.error("申请失败");
            }
        } catch (RuntimeException e) {
            return ResultVo.error(e.getMessage());
        }
    }

    /**
     * 更新用户资料
     * POST /user/profile
     */
    @OperationLog(description = "更新资料")
    @PostMapping("/user/profile")
    public ResultVo updateProfile(@Valid @RequestBody UpdateProfileRequest request, HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId(httpRequest);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }

        User user = userService.getById(userId);
        if (user == null) {
            return ResultVo.error("用户不存在");
        }

        if (StringUtils.isNotEmpty(request.getNickname())) {
            user.setName(request.getNickname());
        }
        if (StringUtils.isNotEmpty(request.getAvatar())) {
            String avatar = AvatarUtils.processAvatarForStorage(request.getAvatar());
            user.setAvatar(avatar);
        }
        //邮箱不为空校验格式并赋值
        if (StringUtils.isNotEmpty(request.getEmail())) {
            user.setEmail(request.getEmail());
        }

        // 更新时间由MyBatis-Plus自动填充
        
        if (userService.updateById(user)) {
            return ResultVo.success("更新成功");
        } else {
            return ResultVo.error("更新失败");
        }
    }
}

