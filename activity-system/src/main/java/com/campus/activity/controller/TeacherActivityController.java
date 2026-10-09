package com.campus.activity.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.activity.common.LoginUser;
import com.campus.activity.common.Result;
import com.campus.activity.common.ResultCode;
import com.campus.activity.dto.ActivityDTO;
import com.campus.activity.dto.ActivityStatsVO;
import com.campus.activity.dto.ActivityVO;
import com.campus.activity.dto.NotificationVO;
import com.campus.activity.dto.RegistrationExportVO;
import com.campus.activity.dto.WaitingQueueVO;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.service.ActivityService;
import com.campus.activity.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 教师端 Controller (V2.0)
 *
 * <p>V2.0 新增:
 * <ul>
 *   <li>候补队列查看 GET /api/teacher/activities/{id}/waiting</li>
 *   <li>报名名单导出 GET /api/teacher/activities/{id}/export (CSV)</li>
 *   <li>站内消息: 列表/未读数/已读标记 (教师收到本人活动名单变动通知)</li>
 * </ul>
 *
 * @author 勇闯天涯组
 */
@Slf4j
@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
@Tag(name = "教师端", description = "活动发布/修改/删除/统计/候补/导出/消息")
public class TeacherActivityController {

    private final ActivityService activityService;
    private final NotificationService notificationService;

    // ---------- 活动管理 ----------

    @Operation(summary = "发布活动", description = "REQ-05; V2.0 支持 target_grade 限定年级")
    @PostMapping("/activities")
    public Result<Long> publish(@Valid @RequestBody ActivityDTO dto) {
        String account = requireTeacherLogin();
        Long id = activityService.publish(dto, account);
        return Result.ok(id);
    }

    @Operation(summary = "本人活动列表", description = "每条带 enrolledCount/remaining")
    @GetMapping("/activities")
    public Result<IPage<ActivityVO>> pageMine(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        String account = requireTeacherLogin();
        return Result.ok(activityService.pageMine(account, current, size));
    }

    @Operation(summary = "修改活动", description = "REQ-06; V2.0 写 ACTIVITY_UPDATED 通知")
    @PutMapping("/activities/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ActivityDTO dto) {
        String account = requireTeacherLogin();
        activityService.update(id, dto, account);
        return Result.ok();
    }

    @Operation(summary = "删除活动", description = "REQ-06; V2.0 写 ACTIVITY_CANCELLED 通知")
    @DeleteMapping("/activities/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        String account = requireTeacherLogin();
        activityService.delete(id, account);
        return Result.ok();
    }

    @Operation(summary = "报名统计", description = "含已报/剩余/候补/退选")
    @GetMapping("/activities/{id}/stats")
    public Result<ActivityStatsVO> stats(@PathVariable Long id) {
        String account = requireTeacherLogin();
        return Result.ok(activityService.stats(id, account));
    }

    // ---------- V2.0 新增: 候补队列 / 名单导出 ----------

    @Operation(summary = "候补队列", description = "按 wait_order ASC 分页")
    @GetMapping("/activities/{id}/waiting")
    public Result<IPage<WaitingQueueVO>> waitingQueue(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        String account = requireTeacherLogin();
        return Result.ok(activityService.pageWaitingQueue(id, account, current, size));
    }

    @Operation(summary = "导出报名名单", description = "CSV: 学号/姓名/年级/报名时间/状态/候补序号")
    @GetMapping("/activities/{id}/export")
    public void export(@PathVariable Long id, HttpServletResponse response) throws IOException {
        String account = requireTeacherLogin();
        List<RegistrationExportVO> list = activityService.listForExport(id, account);

        String fileName = "activity_" + id + "_roster.csv";
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + fileName + "\"");
        // UTF-8 BOM, 避免 Excel 打开乱码
        PrintWriter writer = new PrintWriter(response.getOutputStream(), false, StandardCharsets.UTF_8);
        writer.write("\uFEFF");
        writer.write("学号,姓名,年级,报名时间,状态,候补序号\n");
        for (RegistrationExportVO r : list) {
            writer.write(csv(r.getStudentNo()));
            writer.write(",");
            writer.write(csv(r.getStudentName()));
            writer.write(",");
            writer.write(csv(r.getGrade()));
            writer.write(",");
            writer.write(csv(r.getSignupTime() == null ? "" : r.getSignupTime().toString()));
            writer.write(",");
            writer.write(csv(r.getStatus()));
            writer.write(",");
            writer.write(r.getWaitOrder() == null ? "" : String.valueOf(r.getWaitOrder()));
            writer.write("\n");
        }
        writer.flush();
        log.info("教师 {} 导出活动名单: activityId={}, rows={}", account, id, list.size());
    }

    /** CSV 字段转义: 含逗号/引号/换行则用双引号包裹并把内部双引号转义为两个 */
    private String csv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    // ---------- V2.0 新增: 站内消息 ----------

    @Operation(summary = "未读消息数", description = "教师端消息红点")
    @GetMapping("/notifications/unread-count")
    public Result<Integer> unreadCount() {
        String account = requireTeacherLogin();
        return Result.ok(notificationService.countUnread(account));
    }

    @Operation(summary = "消息列表", description = "教师收到本人活动名单变动通知")
    @GetMapping("/notifications")
    public Result<IPage<NotificationVO>> notifications(
            @RequestParam(defaultValue = "false")
            @Parameter(description = "true 仅未读")
            Boolean onlyUnread,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        String account = requireTeacherLogin();
        return Result.ok(notificationService.pageMine(account, onlyUnread, current, size));
    }

    @Operation(summary = "单条已读")
    @PostMapping("/notifications/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        String account = requireTeacherLogin();
        notificationService.markRead(id, account);
        return Result.ok();
    }

    @Operation(summary = "全部已读")
    @PostMapping("/notifications/read-all")
    public Result<Void> markAllRead() {
        String account = requireTeacherLogin();
        notificationService.markAllRead(account);
        return Result.ok();
    }

    // ==================== 私有: 鉴权辅助 ====================

    private String requireTeacherLogin() {
        LoginUser user = LoginUser.current();
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (!"TEACHER".equals(user.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅教师可访问该接口");
        }
        return user.getAccount();
    }
}
