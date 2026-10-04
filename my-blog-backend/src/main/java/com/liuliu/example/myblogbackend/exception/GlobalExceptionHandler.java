package com.liuliu.example.myblogbackend.exception;

import com.liuliu.example.myblogbackend.common.ErrorCode;
import com.liuliu.example.myblogbackend.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 参数校验失败（@Valid）
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        log.warn("参数校验失败: {}", msg);
        return Result.error(ErrorCode.PARAM_ERROR.getCode(), msg);
    }

    // 业务异常：属于可预期的业务拒绝，只记 warn，不打堆栈
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
        return Result.error(e.getErrorCode().getCode(), e.getMessage());
    }

    // 兜底：未预期异常，记录完整堆栈，对外只给笼统提示（避免泄露内部信息）
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("服务器内部错误", e);
        return Result.error(ErrorCode.ERROR.getCode(), ErrorCode.ERROR.getMessage());
    }
}
