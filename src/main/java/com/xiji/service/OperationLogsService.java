package com.xiji.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiji.entity.domain.OperationLogs;

/**
* @description 针对表【operation_logs(操作日志表)】的数据库操作Service
*/
public interface OperationLogsService extends IService<OperationLogs> {
    void addLog(OperationLogs operationLogs);
}
