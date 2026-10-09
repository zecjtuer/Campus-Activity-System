package com.campus.activity.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.activity.common.LoginUser;
import com.campus.activity.common.Result;
import com.campus.activity.common.ResultCode;
import com.campus.activity.dto.ActivityDetailVO;
import com.campus.activity.dto.ActivityQueryDTO;
import com.campus.activity.dto.ActivityVO;
import com.campus.activity.dto.NotificationVO;
import com.campus.activity.dto.RegisterResultVO;
import com.campus.activity.dto.RegistrationVO;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.service.ActivityService;
import com.campus.activity.service.NotificationService;
import com.campus.activity.service.RegistrationService;
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
 * 学生端 Controller (V2.0)
 *
 * <p>V2.0 变更:
 * <ul>
 *   <li>报名: 三分支 (ENROLLED / WAITING 候补登记)</li>
 *   <li>退选: 同事务触发候补递补</li>
 *   <li>新增: 站内消息列表 (未读标记)</li>
 * </ul>
 *
 * @author 勇闯天涯组
 */
@Slf4j
@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@Tag(name = "学生端", description = "活动浏览/报名/退选/候补/我的记录/消息")
public class StudentActivityController {

    private final ActivityService activityService;
    private final RegistrationService registrationService;
    private final NotificationService notificationService;

    // ---------- 活动浏览 ----------

    @Operation(summary = "活动分页搜索")
    @GetMapping("/activities")
    public Result<IPage<ActivityVO>> pageActivities(
            @Parameter(description = "分页与关键词") ActivityQueryDTO query) {
        requireStudentLogin();
        return Result.ok(activityService.pageForStudent(query));
    }

    @Operation(summary = "活动详情", description = "含 enrolledCount/remaining/本人状态(ENROLLED/WAITING/候补序号)")
    @GetMapping("/activities/{id}")
    public Result<ActivityDetailVO> getDetail(@PathVariable Long id) {
        String account = requireStudentLogin();
        return Result.ok(activityService.getDetail(id, account));
    }

    // ---------- 报名 / 退选 (三分支) ----------

    @Operation(summary = "报名活动", description = "V2.0 三分支: 资格→时间→防重复; 名额满则候补 WAITING")
    @PostMapping("/activities/{id}/register")
    public Result<RegisterResultVO> register(@PathVariable Long id) {
        String account = requireStudentLogin();
        return Result.ok(registrationService.register(id, account));
    }

    @Operation(summary = "退选活动", description = "V2.0: 改 CANCELLED + 同事务候补递补 + 写通知")
    @PostMapping("/activities/{id}/withdraw")
    public Result<Void> withdraw(@PathVariable Long id) {
        String account = requireStudentLogin();
        registrationService.withdraw(id, account);
        return Result.ok();
    }

    // ---------- 我的报名记录 ----------

    @Operation(summary = "我的报名记录", description = "状态四态 ENROLLED/WAITING/CANCELLED/INVALID")
    @GetMapping("/registrations")
    public Result<IPage<RegistrationVO>> myRegistrations(
            @RequestParam(required = false)
            @Parameter(description = "状态: ENROLLED/WAITING/CANCELLED/INVALID, 不传返回全部")
            String status,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        String account = requireStudentLogin();
        return Result.ok(registrationService.listMine(account, status, current, size));
    }

    // ---------- V2.0 新增: 站内消息 ----------

    @Operation(summary = "未读消息数", description = "学生端消息红点")
    @GetMapping("/notifications/unread-count")
    public Result<Integer> unreadCount() {
        String account = requireStudentLogin();
        return Result.ok(notificationService.countUnread(account));
    }

    @Operation(summary = "消息列表", description = "报名成功/取消/递补/活动取消/活动变更")
    @GetMapping("/notifications")
    public Result<IPage<NotificationVO>> notifications(
            @RequestParam(defaultValue = "false")
            @Parameter(description = "true 仅未读")
            Boolean onlyUnread,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        String account = requireStudentLogin();
        return Result.ok(notificationService.pageMine(account, onlyUnread, current, size));
    }

    @Operation(summary = "单条已读")
    @PostMapping("/notifications/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        String account = requireStudentLogin();
        notificationService.markRead(id, account);
        return Result.ok();
    }

    @Operation(summary = "全部已读")
    @PostMapping("/notifications/read-all")
    public Result<Void> markAllRead() {
        String account = requireStudentLogin();
        notificationService.markAllRead(account);
        return Result.ok();
    }

    // ==================== 私有: 鉴权辅助 ====================

    private String requireStudentLogin() {
        LoginUser user = LoginUser.current();
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (!"STUDENT".equals(user.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅学生可访问该接口");
        }
        return user.getAccount();
    }
}
