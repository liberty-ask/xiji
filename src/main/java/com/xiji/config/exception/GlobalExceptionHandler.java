package com.xiji.config.exception;

import com.xiji.common.response.ResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;

/**
 * 全局异常处理器
 * @author liberty
 */
@Slf4j
@RestControllerAdvice
@ControllerAdvice(basePackages = "com.xiji")
public class GlobalExceptionHandler {

    /**
     * 处理参数验证失败异常（@RequestBody @Valid）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResultVo handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String errorMessage = fieldError != null 
            ? fieldError.getDefaultMessage() 
            : "参数验证失败";
        log.warn("参数验证失败，message={}", errorMessage);
        return ResultVo.error(errorMessage);
    }

    /**
     * 处理参数验证失败异常（@ModelAttribute @Valid）
     */
    @ExceptionHandler(BindException.class)
    public ResultVo handleBindException(BindException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String errorMessage = fieldError != null 
            ? fieldError.getDefaultMessage() 
            : "参数验证失败";
        log.warn("参数验证失败，message={}", errorMessage);
        return ResultVo.error(errorMessage);
    }

    /**
     * 处理参数验证失败异常（@RequestParam @Valid）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResultVo handleConstraintViolationException(ConstraintViolationException ex) {
        Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();
        String errorMessage = violations.stream()
            .map(ConstraintViolation::getMessage)
            .findFirst()
            .orElse("参数验证失败");
        log.warn("参数验证失败，message={}", errorMessage);
        return ResultVo.error(errorMessage);
    }

    // 处理权限不足，UnauthorizedException
    @ExceptionHandler(UnauthorizedException.class)
    public ResultVo handleUnauthorizedException(UnauthorizedException ex) {
        log.warn("权限不足，message={}", ex.getMessage());
        return ResultVo.error("用户没有权限访问：" + ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResultVo error(Exception e) {
        log.error("系统异常", e);
        // 生产环境不返回详细异常信息
        String message = "系统异常，请联系管理员";
        // 开发环境可以返回详细错误信息
        if (log.isDebugEnabled()) {
            message += "：" + e.getMessage();
        }
        return ResultVo.error(message);
    }

}
