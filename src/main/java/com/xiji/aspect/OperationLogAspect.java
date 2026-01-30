package com.xiji.aspect;

import com.xiji.common.annotation.OperationLog;
import com.xiji.entity.domain.OperationLogs;
import com.xiji.service.OperationLogsService;
import com.xiji.utils.IpUtils;
import jakarta.annotation.Resource;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * 操作日志切面类
 * 拦截带有`@OperationLog`注解的方法记录日志。
 */
@Aspect
@Component
public class OperationLogAspect {

    @Resource
    private OperationLogsService operationLogService;

    // 定义切入点
    @Pointcut("@annotation(com.xiji.common.annotation.OperationLog)")
    public void operationLogPointCut() {}

    // 在方法执行后记录日志
    @AfterReturning(pointcut = "operationLogPointCut()", returning = "result")
    public void afterReturning(JoinPoint joinPoint, Object result) {
        saveLog(joinPoint, null);
    }

    // 在方法抛出异常后记录日志
    @AfterThrowing(pointcut = "operationLogPointCut()", throwing = "exception")
    public void afterThrowing(JoinPoint joinPoint, Throwable exception) {
        saveLog(joinPoint, exception);
    }

    private void saveLog(JoinPoint joinPoint, Throwable exception) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        OperationLog operationLog = signature.getMethod().getAnnotation(OperationLog.class);
        String description = operationLog.description();

        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = null;
        if (attributes != null) {
            request = attributes.getRequest();
        }

        String url = null;
        if (request != null) {
            url = request.getRequestURL().toString();
        }
        String method = null;
        if (request != null) {
            method = request.getMethod();
        }
        // 获取ip地址
        String ip = IpUtils.getIpAddr(request);

        // 获取请求参数（过滤敏感信息）
        String params = getRequestParams(joinPoint);

        // 创建日志实体
        OperationLogs logEntity = new OperationLogs();
        logEntity.setDescription(description);
        logEntity.setUrl(url);
        logEntity.setMethod(method);
        logEntity.setIp(ip);
        logEntity.setParams(params);
        logEntity.setException(exception != null ? exception.getMessage() : null);
        logEntity.setCreatedAt(LocalDateTime.now());

        // 保存日志
        operationLogService.save(logEntity);
    }
    
    /**
     * 获取请求参数，过滤敏感信息
     */
    private String getRequestParams(JoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args == null || args.length == 0) {
                return null;
            }
            
            StringBuilder params = new StringBuilder();
            for (Object arg : args) {
                if (arg == null) {
                    continue;
                }
                
                // 过滤敏感字段
                String argStr = arg.toString();
                // 过滤密码相关字段
                if (argStr.toLowerCase().contains("password") || 
                    argStr.toLowerCase().contains("pwd") ||
                    argStr.toLowerCase().contains("token") ||
                    argStr.toLowerCase().contains("captcha")) {
                    params.append("***敏感信息已过滤***");
                } else {
                    // 限制参数长度，避免日志过大
                    if (argStr.length() > 500) {
                        params.append(argStr.substring(0, 500)).append("...");
                    } else {
                        params.append(argStr);
                    }
                }
                params.append("; ");
            }
            
            return params.length() > 0 ? params.toString() : null;
        } catch (Exception e) {
            return "参数获取失败";
        }
    }
}
