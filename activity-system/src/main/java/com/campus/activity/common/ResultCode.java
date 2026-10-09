package com.campus.activity.common;

import lombok.Getter;

/**
 * 业务返回码枚举
 *
 * <p>V2.0 新增: 1010~1016 报名/退选/账号管理相关错误码</p>
 *
 * <p>对应需求文档第七章状态字典与错误码约定</p>
 */
@Getter
public enum ResultCode {

    // 通用成功
    SUCCESS(200, "成功"),

    // 通用错误
    PARAM_INVALID(400, "参数校验失败"),
    UNAUTHORIZED(401, "未登录或Token失效"),
    FORBIDDEN(403, "无权限或越权"),
    SERVER_ERROR(500, "服务器内部错误"),

    // 业务错误 (1xxx)
    ACCOUNT_OR_PASSWORD_ERROR(1001, "账号或密码错误"),
    ACCOUNT_DISABLED(1002, "账号已停用"),
    ACTIVITY_NOT_FOUND(1003, "活动不存在"),
    SIGNUP_CLOSED(1004, "报名已截止"),
    WITHDRAW_CLOSED(1005, "报名截止后不可退选"),
    DUPLICATE_SIGNUP(1006, "请勿重复报名"),
    QUOTA_FULL(1007, "名额已满"),
    NO_VALID_REGISTRATION(1008, "未找到有效报名记录"),
    MAX_LESS_THAN_ENROLLED(1009, "人数上限不能小于已报名人数"),

    // V2.0 新增业务错误
    GRADE_NOT_ALLOWED(1010, "年级不符, 不在允许报名范围内"),
    ACTIVITY_ENDED(1011, "活动已结束, 不可退选"),
    USER_NOT_FOUND(1012, "用户不存在"),
    ALREADY_DISABLED(1013, "账号已处于停用状态"),
    ALREADY_ENABLED(1014, "账号已处于启用状态"),
    CANNOT_DISABLE_ADMIN(1015, "管理员账号不可停用"),
    NOTIFICATION_NOT_FOUND(1016, "通知不存在");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
