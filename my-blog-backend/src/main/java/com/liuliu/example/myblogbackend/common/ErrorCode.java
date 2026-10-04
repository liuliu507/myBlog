package com.liuliu.example.myblogbackend.common;

import lombok.Getter;

/**
 * 业务错误码（HTTP 状态码语义，放在响应体 code 中）
 */
@Getter
public enum ErrorCode {

    PARAM_ERROR(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权操作"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),
    TOO_MANY_REQUESTS(429, "操作过于频繁"),
    ERROR(500, "服务器内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
