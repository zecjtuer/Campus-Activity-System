package com.campus.activity.exception;

import com.campus.activity.common.Result;
import com.campus.activity.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理
 *
 * <p>对应需求文档 4.4 第 2 条:
 * <ul>
 *   <li>BusinessException 用其自身 code 返回</li>
 *   <li>MethodArgumentNotValidException 返回 400 + 第一条校验错误信息</li>
 *   <li>DuplicateKeyException 返回 1006 "重复报名"</li>
 *   <li>其它异常返回 500</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * @Valid 参数校验失败 (请求体 JSON)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleArgNotValid(MethodArgumentNotValidException e) {
        FieldError first = e.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String msg = first != null ? first.getDefaultMessage() : ResultCode.PARAM_INVALID.getMessage();
        log.warn("参数校验失败: {}", msg);
        return Result.fail(ResultCode.PARAM_INVALID, msg);
    }

    /**
     * 表单形式绑定失败
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBind(BindException e) {
        FieldError first = e.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String msg = first != null ? first.getDefaultMessage() : ResultCode.PARAM_INVALID.getMessage();
        log.warn("参数绑定失败: {}", msg);
        return Result.fail(ResultCode.PARAM_INVALID, msg);
    }

    /**
     * 请求体不可读 (JSON 格式错误 / 缺 body)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBodyNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体不可读: {}", e.getMessage());
        return Result.fail(ResultCode.PARAM_INVALID, "请求体格式错误或缺失");
    }

    /**
     * 唯一索引冲突 (重复报名兜底拦截)
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("唯一索引冲突: {}", e.getMessage());
        return Result.fail(ResultCode.DUPLICATE_SIGNUP);
    }

    /**
     * 兜底: 其它所有未捕获异常返回 500
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("未捕获异常", e);
        return Result.fail(ResultCode.SERVER_ERROR);
    }
}
