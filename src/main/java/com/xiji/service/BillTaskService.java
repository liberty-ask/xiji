package com.xiji.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiji.entity.domain.BillTask;

/**
 * 账单任务Service接口
 * @author liberty
 */
public interface BillTaskService extends IService<BillTask> {

    /**
     * 更新任务状态
     * @param taskId 任务ID
     * @param status 状态
     * @param progress 进度
     * @param totalCount 总记录数
     * @param successCount 成功记录数
     * @param failCount 失败记录数
     * @param errorMessage 错误信息
     */
    void updateTaskStatus(Long taskId, Integer status, Integer progress, Integer totalCount, Integer successCount, Integer failCount, String errorMessage);

    /**
     * 获取用户的任务列表
     * @param userId 用户ID
     * @return 任务列表
     */
    java.util.List<BillTask> getTaskListByUser(Long userId);
}