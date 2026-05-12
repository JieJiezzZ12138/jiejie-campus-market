package com.jiejie.order.exception;

import com.jiejie.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result handleMissingParam(MissingServletRequestParameterException ex) {
        log.warn("Missing request parameter: {}", ex.getParameterName(), ex);
        return Result.error("缺少请求参数：" + ex.getParameterName());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result handleValidation(MethodArgumentNotValidException ex) {
        log.warn("Request validation failed", ex);
        return Result.error("请求参数校验失败");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal request argument", ex);
        return Result.error(ex.getMessage() != null ? ex.getMessage() : "请求参数不合法");
    }

    @ExceptionHandler(Exception.class)
    public Result handleException(Exception ex) {
        log.error("Unhandled server exception", ex);
        return Result.error("服务器内部错误，请稍后重试");
    }
}
