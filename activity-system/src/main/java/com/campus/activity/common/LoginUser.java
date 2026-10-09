package com.campus.activity.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前登录用户上下文 (放 ThreadLocal, 由 JwtInterceptor 写入)
 *
 * <p>对应需求文档 4.4 第 4 条: 解析后将 account/role 放入 ThreadLocal</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser {

    /** 登录账号 (学号/工号, 同 sys_user.account) */
    private String account;

    /** 角色 STUDENT/TEACHER */
    private String role;

    /** 姓名 */
    private String name;

    /**
     * ThreadLocal 持有者: 请求结束后由 JwtInterceptor afterCompletion 清理, 防止内存泄漏
     */
    public static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static LoginUser current() {
        return HOLDER.get();
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static void clear() {
        HOLDER.remove();
    }
}
