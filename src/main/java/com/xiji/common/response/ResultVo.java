package com.xiji.common.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结果类
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ResultVo {
    private int code;
    private String message;
    private Object data;


    public static ResultVo success() {
        ResultVo resultVo = new ResultVo();
        resultVo.setCode(200);
        resultVo.setMessage("请求成功");
        return resultVo;
    }

    public static ResultVo success(String msg) {
        ResultVo resultVo = new ResultVo();
        resultVo.setCode(200);
        resultVo.setMessage(msg);
        return resultVo;
    }

    public static ResultVo success(Object data) {
        ResultVo resultVo = success();
        resultVo.setData(data);
        return resultVo;
    }

    public static ResultVo success(String msg, Object data) {
        ResultVo resultVo = success(msg);
        resultVo.setData(data);
        return resultVo;
    }

    public static ResultVo error() {
        ResultVo resultVo = new ResultVo();
        resultVo.setCode(500);
        resultVo.setMessage("请求失败");
        return resultVo;
    }

    public static ResultVo error(String msg) {
        ResultVo resultVo = new ResultVo();
        resultVo.setCode(500);
        resultVo.setMessage(msg);
        return resultVo;
    }

}

