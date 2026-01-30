package com.xiji.config.exception;

/**
 * 权限不足异常类
 * @author liberty
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}