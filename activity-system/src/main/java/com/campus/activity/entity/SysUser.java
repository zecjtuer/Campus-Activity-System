package com.campus.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户表 sys_user: 学生 / 教师 / 管理员共用, role 区分
 *
 * <p>V2.0: role 新增 ADMIN 取值</p>
 *
 * @author 勇闯天涯组
 */
@Data
@TableName("sys_user")
@Schema(description = "用户")
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 登录账号 (学号/工号), 主键, 手动输入
     */
    @TableId(type = IdType.INPUT)
    @Schema(description = "登录账号 (学号/工号)", example = "S2024001")
    private String account;

    @Schema(description = "BCrypt 加密后的密码")
    private String password;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "角色 STUDENT/TEACHER/ADMIN")
    private String role;

    @Schema(description = "学号或工号")
    private String userNo;

    @Schema(description = "1 启用 0 停用")
    private Integer status;

    // ---------- 角色常量 ----------
    public static final String ROLE_STUDENT = "STUDENT";
    public static final String ROLE_TEACHER = "TEACHER";
    public static final String ROLE_ADMIN = "ADMIN";

    // ---------- 状态常量 ----------
    public static final int STATUS_ENABLED = 1;
    public static final int STATUS_DISABLED = 0;
}
