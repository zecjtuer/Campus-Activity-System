package com.campus.activity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.activity.common.ResultCode;
import com.campus.activity.dto.RegisterResultVO;
import com.campus.activity.dto.RegistrationVO;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.Registration;
import com.campus.activity.entity.SysUser;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.mapper.ActivityMapper;
import com.campus.activity.mapper.RegistrationMapper;
import com.campus.activity.mapper.SysUserMapper;
import com.campus.activity.service.NotificationService;
import com.campus.activity.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 报名 Service 实现 (V2.0)
 *
 * <p>V2.0 关键变更:
 * <ul>
 *   <li>报名: 三分支校验 (资格→时间→防重复), 通过后判名额 (ENROLLED / WAITING)</li>
 *   <li>退选: 改状态为 CANCELLED, 同事务触发候补递补 + 写通知</li>
 *   <li>派生状态: 增加 WAITING</li>
 *   <li>资格校验: target_grade 限定年级, 后端二次校验</li>
 * </ul>
 *
 * @author 勇闯天涯组
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private final RegistrationMapper registrationMapper;
    private final ActivityMapper activityMapper;
    private final SysUserMapper sysUserMapper;
    private final NotificationService notificationService;

    // ==================== 报名 (三分支) ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegisterResultVO register(Long activityId, String studentAccount) {
        // V3.0: 用 FOR UPDATE 悲观锁锁定活动行, 高并发下防止 COUNT 读到过期值导致超卖。
        // 数据依据: 模拟数据热门活动 40-50% 报名请求超 1s, 存在 capacity_conflict。
        Activity activity = activityMapper.selectByIdForUpdate(activityId);
        if (activity == null) {
            throw new BusinessException(ResultCode.ACTIVITY_NOT_FOUND);
        }

        // 校验 2: 资格 (年级) - target_grade 不为空时, 学生年级必须在列表中
        checkGrade(activity, studentAccount);

        // 校验 3: 时间 (now < signup_deadline)
        LocalDateTime now = LocalDateTime.now();
        if (!now.isBefore(activity.getSignupDeadline())) {
            throw new BusinessException(ResultCode.SIGNUP_CLOSED);
        }

        // 校验 4: 防重复 - 同一学生对同一活动只能有一条 ENROLLED 或 WAITING
        if (registrationMapper.existsActive(studentAccount, activityId)) {
            throw new BusinessException(ResultCode.DUPLICATE_SIGNUP);
        }

        // 通过校验, 判断名额
        int enrolled = registrationMapper.countEnrolledByActivity(activityId);
        Registration reg = new Registration();
        reg.setStudentAccount(studentAccount);
        reg.setActivityId(activityId);
        reg.setSignupTime(now);

        if (enrolled < activity.getMaxParticipants()) {
            // 分支 A: 名额未满 → ENROLLED
            reg.setStatus(Registration.STATUS_ENROLLED);
            try {
                registrationMapper.insert(reg);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                throw new BusinessException(ResultCode.DUPLICATE_SIGNUP);
            }
            // 写通知 (报名成功) - 学生收
            notificationService.notifyEnrolled(studentAccount, activityId, activity.getTitle());
            log.info("学生 {} 报名成功: activityId={}", studentAccount, activityId);
            RegisterResultVO vo = new RegisterResultVO();
            vo.setStatus(Registration.STATUS_ENROLLED);
            vo.setMessage("报名成功");
            return vo;
        } else {
            // 分支 B: 名额已满 → WAITING (分配 wait_order)
            int nextOrder = registrationMapper.maxWaitOrder(activityId) + 1;
            reg.setStatus(Registration.STATUS_WAITING);
            reg.setWaitOrder(nextOrder);
            try {
                registrationMapper.insert(reg);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                throw new BusinessException(ResultCode.DUPLICATE_SIGNUP);
            }
            // 候补登记不发通知 (PROMOTED 通知在递补时发)
            log.info("学生 {} 候补登记成功: activityId={}, waitOrder={}",
                    studentAccount, activityId, nextOrder);
            RegisterResultVO vo = new RegisterResultVO();
            vo.setStatus(Registration.STATUS_WAITING);
            vo.setWaitOrder(nextOrder);
            vo.setMessage("名额已满, 候补登记成功, 当前位次 " + nextOrder);
            return vo;
        }
    }

    // ==================== 退选 (含同事务递补) ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(Long activityId, String studentAccount) {
        // 1. 必须存在本人 ENROLLED 记录
        LambdaQueryWrapper<Registration> findWrapper = new LambdaQueryWrapper<Registration>()
                .eq(Registration::getActivityId, activityId)
                .eq(Registration::getStudentAccount, studentAccount)
                .eq(Registration::getStatus, Registration.STATUS_ENROLLED);
        Registration existing = registrationMapper.selectOne(findWrapper);
        if (existing == null) {
            throw new BusinessException(ResultCode.NO_VALID_REGISTRATION);
        }

        // 2. 活动存在且未结束 (now < activity_time)
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ResultCode.ACTIVITY_NOT_FOUND);
        }
        if (!LocalDateTime.now().isBefore(activity.getActivityTime())) {
            throw new BusinessException(ResultCode.ACTIVITY_ENDED);
        }

        // 3. 状态改 CANCELLED (清空 wait_order, 虽然 ENROLLED 行本来就没有)
        LambdaUpdateWrapper<Registration> updateWrapper = new LambdaUpdateWrapper<Registration>()
                .eq(Registration::getRecordId, existing.getRecordId())
                .eq(Registration::getStatus, Registration.STATUS_ENROLLED)
                .set(Registration::getStatus, Registration.STATUS_CANCELLED)
                .set(Registration::getWaitOrder, null);
        int rows = registrationMapper.update(null, updateWrapper);
        if (rows == 0) {
            throw new BusinessException(ResultCode.NO_VALID_REGISTRATION);
        }

        // 4. 写退选通知 (学生 + 教师)
        notificationService.notifyCancelled(studentAccount, activityId,
                activity.getTitle(), activity.getTeacherAccount());

        // 5. 同事务触发候补递补: 取队首 WAITING → ENROLLED
        Registration nextWaiting = registrationMapper.nextWaitingRecord(activityId);
        if (nextWaiting != null) {
            int promoted = registrationMapper.promoteToEnrolled(nextWaiting.getRecordId());
            if (promoted > 0) {
                // 写递补通知 (递补学生 + 教师)
                notificationService.notifyPromoted(nextWaiting.getStudentAccount(),
                        activityId, activity.getTitle(), activity.getTeacherAccount());
                log.info("候补递补: {} -> ENROLLED, activityId={}",
                        nextWaiting.getStudentAccount(), activityId);
            }
        }

        log.info("学生 {} 退选活动: activityId={}", studentAccount, activityId);
    }

    // ==================== 我的报名记录 ====================

    @Override
    public IPage<RegistrationVO> listMine(String studentAccount, String status, Long current, Long size) {
        Page<RegistrationVO> page = new Page<>(current, size);

        // 校验 status 枚举 (V2.0 四态)
        if (StringUtils.hasText(status)) {
            if (!Registration.STATUS_ENROLLED.equals(status)
                    && !Registration.STATUS_WAITING.equals(status)
                    && !Registration.STATUS_CANCELLED.equals(status)
                    && !Registration.STATUS_INVALID.equals(status)) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "状态参数不合法");
            }
        }

        IPage<RegistrationVO> result = registrationMapper.pageMyRegistrations(page, studentAccount, status);

        // 计算 displayStatus
        LocalDateTime now = LocalDateTime.now();
        for (RegistrationVO vo : result.getRecords()) {
            vo.setDisplayStatus(computeDisplayStatus(vo.getStatus(), vo.getActivityTime(), now));
        }

        return result;
    }

    /**
     * 计算派生展示状态:
     * <ul>
     *   <li>ENROLLED + now &lt; activityTime → ONGOING</li>
     *   <li>ENROLLED + now &gt;= activityTime → FINISHED</li>
     *   <li>WAITING / CANCELLED / INVALID → 透传</li>
     * </ul>
     */
    private String computeDisplayStatus(String status, LocalDateTime activityTime, LocalDateTime now) {
        if (Registration.STATUS_ENROLLED.equals(status)) {
            if (activityTime != null && now.isBefore(activityTime)) {
                return Registration.DISPLAY_ONGOING;
            }
            return Registration.DISPLAY_FINISHED;
        }
        return status;
    }

    // ==================== 私有: 年级资格校验 ====================

    /**
     * V2.0: target_grade 限定年级校验
     * <p>target_grade 为空 → 不限, 通过
     * <p>否则按逗号分割, 学生年级必须在列表中
     *
     * <p>学生年级从 sys_user.user_no 提取 (S2024001 → 2024)</p>
     */
    private void checkGrade(Activity activity, String studentAccount) {
        String targetGrade = activity.getTargetGrade();
        if (!StringUtils.hasText(targetGrade)) {
            return; // 不限年级
        }
        // 取学生 user_no 推导年级
        SysUser user = sysUserMapper.selectById(studentAccount);
        if (user == null || !StringUtils.hasText(user.getUserNo())) {
            throw new BusinessException(ResultCode.GRADE_NOT_ALLOWED);
        }
        String studentGrade = extractGrade(user.getUserNo());
        String[] allowed = targetGrade.split("\\s*,\\s*");
        boolean ok = Arrays.stream(allowed)
                .anyMatch(g -> g.trim().equals(studentGrade));
        if (!ok) {
            throw new BusinessException(ResultCode.GRADE_NOT_ALLOWED);
        }
    }

    /**
     * 从学号/工号提取年级: S2024001 → 2024, T2024001 → 2024
     */
    private String extractGrade(String userNo) {
        if (userNo == null || userNo.length() < 5) {
            return "";
        }
        // 取第 2~5 位 (去首字母后 4 位年份)
        return userNo.substring(1, 5);
    }
}
