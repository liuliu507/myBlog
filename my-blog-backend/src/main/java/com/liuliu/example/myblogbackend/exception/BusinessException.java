package com.liuliu.example.myblogbackend.exception;

import com.liuliu.example.myblogbackend.common.ErrorCode;
import lombok.Getter;

/**
 * 业务异常：携带错误码，由全局异常处理器统一转换
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
