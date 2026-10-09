package com.campus.activity.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.activity.common.LoginUser;
import com.campus.activity.common.Result;
import com.campus.activity.common.ResultCode;
import com.campus.activity.dto.UserVO;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员 Controller (V2.0 新增)
 *
 * <p>对应需求文档:
 * <ul>
 *   <li>账号停用/启用 (管理员不可介入报名业务)</li>
 *   <li>用户列表查询 (支持 role/status 筛选)</li>
 * </ul>
 *
 * <p>角色边界: 仅 ADMIN 可访问。</p>
 *
 * @author 勇闯天涯组
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "管理员端", description = "账号停用/启用、用户列表")
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "用户列表", description = "分页查询, 支持 role/status 筛选")
    @GetMapping("/users")
    public Result<IPage<UserVO>> pageUsers(
            @RequestParam(required = false)
            @Parameter(description = "角色 STUDENT/TEACHER/ADMIN, 不传返回全部")
            String role,
            @RequestParam(required = false)
            @Parameter(description = "状态 0停用/1启用, 不传返回全部")
            Integer status,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        requireAdminLogin();
        return Result.ok(adminService.pageUsers(role, status, current, size));
    }

    @Operation(summary = "停用账号", description = "逻辑停用 status=0; 管理员账号不可停用")
    @PostMapping("/users/{account}/disable")
    public Result<Void> disable(@PathVariable String account) {
        requireAdminLogin();
        adminService.disable(account);
        return Result.ok();
    }

    @Operation(summary = "启用账号", description = "启用 status=1")
    @PostMapping("/users/{account}/enable")
    public Result<Void> enable(@PathVariable String account) {
        requireAdminLogin();
        adminService.enable(account);
        return Result.ok();
    }

    // ==================== 私有: 鉴权辅助 ====================

    /**
     * 必须是管理员登录; 学生/教师越权访问抛 403
     */
    private String requireAdminLogin() {
        LoginUser user = LoginUser.current();
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (!"ADMIN".equals(user.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅管理员可访问该接口");
        }
        return user.getAccount();
    }
}
