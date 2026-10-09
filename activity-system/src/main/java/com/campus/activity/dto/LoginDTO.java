package com.campus.activity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 登录请求 DTO
 *
 * @author 勇闯天涯组
 */
@Data
@Schema(description = "登录请求")
public class LoginDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "账号不能为空")
    @Schema(description = "账号 (学号/工号)", example = "S2024001", required = true)
    private String account;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "明文密码", example = "123456", required = true)
    private String password;
}
