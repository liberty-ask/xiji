package com.xiji.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiji.entity.domain.OperationLogs;
import com.xiji.service.OperationLogsService;
import com.xiji.mapper.OperationLogsMapper;
import org.springframework.stereotype.Service;

/**
* @description 针对表【operation_logs(操作日志表)】的数据库操作Service实现
*/
@Service
public class OperationLogsServiceImpl extends ServiceImpl<OperationLogsMapper, OperationLogs>
    implements OperationLogsService{

    private final OperationLogsMapper operationLogsMapper;

    public OperationLogsServiceImpl(OperationLogsMapper operationLogsMapper) {
        this.operationLogsMapper = operationLogsMapper;
    }

    @Override
    public void addLog(OperationLogs operationLogs) {
        operationLogsMapper.insert(operationLogs);
    }
}




