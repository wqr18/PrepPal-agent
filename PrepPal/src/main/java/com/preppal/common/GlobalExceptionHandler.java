package com.preppal.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 登录失效返回真实 HTTP 401，其余业务异常返回 HTTP 200 + 业务码 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        HttpStatus status = e.getCode() == ResultCode.UNAUTHORIZED.getCode()
                ? HttpStatus.UNAUTHORIZED
                : HttpStatus.OK;
        return ResponseEntity.status(status).body(Result.failure(e.getCode(), e.getMessage()));
    }

    /** @Valid 校验失败（@RequestBody） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        return Result.failure(ResultCode.PARAM_ERROR, firstFieldMessage(e.getBindingResult().getFieldErrors()));
    }

    /** @Valid 校验失败（表单 / query 参数） */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException e) {
        return Result.failure(ResultCode.PARAM_ERROR, firstFieldMessage(e.getBindingResult().getFieldErrors()));
    }

    /** 方法级参数校验失败 */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .findFirst()
                .orElse(ResultCode.PARAM_ERROR.getMessage());
        return Result.failure(ResultCode.PARAM_ERROR, message);
    }

    /** 缺少必填的请求参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParameter(MissingServletRequestParameterException e) {
        return Result.failure(ResultCode.PARAM_ERROR, "缺少必填参数：" + e.getParameterName());
    }

    /** 文件超过 multipart 限制（单文件 ≤5MB） */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.warn("文件超过上传上限: {}", e.getMessage());
        return Result.failure(ResultCode.FILE_TOO_LARGE);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return Result.failure(ResultCode.PARAM_ERROR, "请求方法不支持：" + e.getMethod());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Result<Void> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        return Result.failure(ResultCode.PARAM_ERROR, "不支持的 Content-Type：" + e.getContentType());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnexpected(Exception e) {
        log.error("未预期的系统异常", e);
        return Result.failure(ResultCode.SYSTEM_ERROR);
    }

    private String firstFieldMessage(java.util.List<FieldError> fieldErrors) {
        return fieldErrors.stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse(ResultCode.PARAM_ERROR.getMessage());
    }
}
