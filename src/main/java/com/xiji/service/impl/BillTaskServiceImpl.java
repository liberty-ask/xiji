package com.xiji.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiji.entity.domain.BillTask;
import com.xiji.mapper.BillTaskMapper;
import com.xiji.service.BillTaskService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 账单任务Service实现类
 * @author liberty
 */
@Service
public class BillTaskServiceImpl extends ServiceImpl<BillTaskMapper, BillTask> implements BillTaskService {

    @Override
    public void updateTaskStatus(Long taskId, Integer status, Integer progress, Integer totalCount, Integer successCount, Integer failCount, String errorMessage) {
        BillTask task = new BillTask();
        task.setId(taskId);
        task.setStatus(status);
        task.setProgress(progress);
        task.setTotalCount(totalCount);
        task.setSuccessCount(successCount);
        task.setFailCount(failCount);
        task.setErrorMessage(errorMessage);
        
        // 如果是开始处理，设置开始时间
        if (status == 1 && progress == 0) {
            task.setStartTime(LocalDateTime.now());
        }
        
        // 如果是处理完成（成功或失败），设置结束时间
        if ((status == 2 || status == 3) && progress == 100) {
            task.setEndTime(LocalDateTime.now());
        }
        
        updateById(task);
    }

    @Override
    public List<BillTask> getTaskListByUser(Long userId) {
        LambdaQueryWrapper<BillTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BillTask::getUserId, userId);
        queryWrapper.orderByDesc(BillTask::getCreatedAt);
        return list(queryWrapper);
    }
}