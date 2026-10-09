package com.campus.activity.exception;

import com.campus.activity.common.ResultCode;
import lombok.Getter;

/**
 * 业务异常: 携带 ResultCode, 由全局异常处理拦截并返回统一格式
 *
 * @author 勇闯天涯组
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String customMessage) {
        super(customMessage);
        this.code = resultCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
