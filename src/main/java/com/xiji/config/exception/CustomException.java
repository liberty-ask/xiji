package com.xiji.config.exception;

import lombok.Setter;

/**
 * 自定义异常类
 * @author liberty
 */
@Setter
public class CustomException extends Exception {
    private String msg;

    public CustomException(String msg) {
        this.msg = msg;
    }

}
