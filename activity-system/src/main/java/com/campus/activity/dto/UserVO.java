package com.campus.activity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户列表项 VO (管理员端账号管理) - V2.0 新增
 *
 * @author 勇闯天涯组
 */
@Data
@Schema(description = "用户列表项")
public class UserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "登录账号")
    private String account;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "角色 STUDENT/TEACHER/ADMIN")
    private String role;

    @Schema(description = "学号/工号")
    private String userNo;

    @Schema(description = "1 启用 0 停用")
    private Integer status;
}
