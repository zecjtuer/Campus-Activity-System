package com.campus.activity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.activity.dto.NotificationVO;
import com.campus.activity.entity.Notification;
import com.campus.activity.entity.Registration;
import com.campus.activity.mapper.NotificationMapper;
import com.campus.activity.mapper.RegistrationMapper;
import com.campus.activity.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 站内通知 Service 实现 (V2.0 新增)
 *
 * <p>5 类事件自动写入:
 * <ul>
 *   <li>ENROLLED          报名成功 (学生)</li>
 *   <li>CANCELLED         取消报名 (学生 + 教师)</li>
 *   <li>PROMOTED          候补递补 (学生 + 教师)</li>
 *   <li>ACTIVITY_CANCELLED 活动取消 (所有 ENROLLED/WAITING 学生)</li>
 *   <li>ACTIVITY_UPDATED  活动信息变更 (所有 ENROLLED/WAITING 学生)</li>
 * </ul>
 *
 * @author 勇闯天涯组
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final RegistrationMapper registrationMapper;

    // ==================== 事件触发写入通知 ====================

    @Override
    public void notifyEnrolled(String studentAccount, Long activityId, String activityTitle) {
        save(studentAccount, com.campus.activity.entity.SysUser.ROLE_STUDENT,
                activityId, Notification.TYPE_ENROLLED,
                "您已成功报名活动: " + activityTitle);
    }

    @Override
    public void notifyCancelled(String studentAccount, Long activityId, String activityTitle, String teacherAccount) {
        // 学生收到状态通知
        save(studentAccount, com.campus.activity.entity.SysUser.ROLE_STUDENT,
                activityId, Notification.TYPE_CANCELLED,
                "您已取消报名活动: " + activityTitle);
        // 教师收到名单变动通知
        save(teacherAccount, com.campus.activity.entity.SysUser.ROLE_TEACHER,
                activityId, Notification.TYPE_CANCELLED,
                "学生 " + studentAccount + " 退选了您的活动: " + activityTitle);
    }

    @Override
    public void notifyPromoted(String studentAccount, Long activityId, String activityTitle, String teacherAccount) {
        // 学生收到候补递补通知
        save(studentAccount, com.campus.activity.entity.SysUser.ROLE_STUDENT,
                activityId, Notification.TYPE_PROMOTED,
                "您已候补递补成功, 已转为正式报名: " + activityTitle);
        // 教师收到名单变动通知
        save(teacherAccount, com.campus.activity.entity.SysUser.ROLE_TEACHER,
                activityId, Notification.TYPE_PROMOTED,
                "学生 " + studentAccount + " 候补递补为正式报名, 您的活动: " + activityTitle);
    }

    @Override
    public void notifyActivityCancelled(Long activityId, String activityTitle) {
        // 给所有 ENROLLED / WAITING 学生发活动取消通知
        List<String> accounts = listActiveStudents(activityId);
        for (String account : accounts) {
            save(account, com.campus.activity.entity.SysUser.ROLE_STUDENT,
                    activityId, Notification.TYPE_ACTIVITY_CANCELLED,
                    "活动已取消: " + activityTitle);
        }
        log.info("活动取消通知已发送: activityId={}, recipients={}", activityId, accounts.size());
    }

    @Override
    public void notifyActivityUpdated(Long activityId, String activityTitle) {
        // 给所有 ENROLLED / WAITING 学生发活动信息变更通知
        List<String> accounts = listActiveStudents(activityId);
        for (String account : accounts) {
            save(account, com.campus.activity.entity.SysUser.ROLE_STUDENT,
                    activityId, Notification.TYPE_ACTIVITY_UPDATED,
                    "活动信息已变更: " + activityTitle);
        }
        log.info("活动信息变更通知已发送: activityId={}, recipients={}", activityId, accounts.size());
    }

    // ==================== 消息列表 / 已读标记 ====================

    @Override
    public int countUnread(String account) {
        return notificationMapper.countUnread(account);
    }

    @Override
    public IPage<NotificationVO> pageMine(String account, Boolean onlyUnread, Long current, Long size) {
        Page<NotificationVO> page = new Page<>(current, size);
        return notificationMapper.pageByReceiver(page, account, onlyUnread);
    }

    @Override
    public void markRead(Long id, String account) {
        int rows = notificationMapper.markRead(id, account);
        if (rows == 0) {
            log.warn("标记已读失败或越权: id={}, account={}", id, account);
        }
    }

    @Override
    public void markAllRead(String account) {
        int rows = notificationMapper.markAllRead(account);
        log.info("批量标记已读: account={}, rows={}", account, rows);
    }

    // ==================== 私有辅助 ====================

    /**
     * 写入一条通知
     */
    private void save(String receiverAccount, String receiverRole, Long activityId, String type, String content) {
        Notification n = new Notification();
        n.setReceiverAccount(receiverAccount);
        n.setReceiverRole(receiverRole);
        n.setActivityId(activityId);
        n.setType(type);
        n.setContent(content);
        n.setIsRead(Notification.READ_NO);
        n.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(n);
    }

    /**
     * 取某活动所有 ENROLLED / WAITING 学生账号 (用于活动取消/变更群发)
     */
    private List<String> listActiveStudents(Long activityId) {
        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<Registration>()
                .eq(Registration::getActivityId, activityId)
                .in(Registration::getStatus,
                        Registration.STATUS_ENROLLED, Registration.STATUS_WAITING);
        return registrationMapper.selectList(wrapper).stream()
                .map(Registration::getStudentAccount)
                .distinct()
                .toList();
    }
}
