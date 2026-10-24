package com.xiji.controller;

import com.xiji.common.response.ResultVo;
import com.xiji.entity.dto.request.SyncPushRequest;
import com.xiji.service.FamilyMemberService;
import com.xiji.service.SyncRecordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 手机端端到端加密同步控制器
 * 推（发件箱批量上传）与拉（游标增量/新设备全量），服务端仅存密文。
 * 同步失败/网络错误不触发登出，语义见 design.md D5。
 * @author liberty
 */
@RestController
@RequestMapping("/api/v1/sync")
@Slf4j
@RequiredArgsConstructor
public class MobileSyncController extends BaseController {

    private final SyncRecordService syncRecordService;
    private final FamilyMemberService familyMemberService;

    /**
     * 批量推送同步记录
     * POST /api/v1/sync/push
     */
    @PostMapping("/push")
    public ResultVo push(@Valid @RequestBody SyncPushRequest request, HttpServletRequest httpRequest) {
        String userId = getCurrentUserId(httpRequest);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }
        if (!familyMemberService.isMemberOfFamily(request.getFamilyId(), userId)) {
            return ResultVo.error("无该家庭的访问权限");
        }
        return ResultVo.success(syncRecordService.push(request.getFamilyId(), request.getRecords()));
    }

    /**
     * 按游标增量拉取同步记录
     * GET /api/v1/sync/pull?familyId=&cursorTime=&cursorId=&limit=&recordId=
     * 游标缺省时从头全量拉取（新设备恢复）；
     * recordId 非空时精确拉取该单条记录（vault 探测等专项场景，1 次 RTT，
     * 忽略游标；家庭归属不匹配时返回空，服务端对记录内容零感知不变）
     */
    @GetMapping("/pull")
    public ResultVo pull(@RequestParam String familyId,
                         @RequestParam(required = false) Long cursorTime,
                         @RequestParam(required = false) String cursorId,
                         @RequestParam(required = false) Integer limit,
                         @RequestParam(required = false) String recordId,
                         HttpServletRequest httpRequest) {
        String userId = getCurrentUserId(httpRequest);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }
        if (!familyMemberService.isMemberOfFamily(familyId, userId)) {
            return ResultVo.error("无该家庭的访问权限");
        }
        return ResultVo.success(syncRecordService.pull(familyId, cursorTime, cursorId, limit, recordId));
    }
}
