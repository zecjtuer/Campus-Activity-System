package com.campus.activity.controller;

import com.campus.activity.common.Result;
import com.campus.activity.dto.LoginDTO;
import com.campus.activity.dto.LoginVO;
import com.campus.activity.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 鉴权 Controller (登录接口 - 公开, 不需要 JWT)
 *
 * <p>对应需求文档 接口 1: POST /api/auth/login</p>
 *
 * @author 勇闯天涯组
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "鉴权", description = "登录接口 (公开, 不需要 JWT)")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "登录", description = "账号密码登录, 返回 token + 角色")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(authService.login(dto));
    }
}
