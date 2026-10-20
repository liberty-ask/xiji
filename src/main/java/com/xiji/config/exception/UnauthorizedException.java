package com.xiji.config.exception;

import lombok.Getter;

/**
 * 权限不足异常类
 * status=401 未登录类（token缺失/无效/过期），status=403 权限不足类
 * @author liberty
 */
@Getter
public class UnauthorizedException extends RuntimeException {

    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;

    private final int status;

    public UnauthorizedException(String message) {
        this(UNAUTHORIZED, message);
    }

    public UnauthorizedException(int status, String message) {
        super(message);
        this.status = status;
    }
}
