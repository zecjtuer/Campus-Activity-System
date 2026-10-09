package com.campus.activity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.activity.common.ResultCode;
import com.campus.activity.dto.ActivityDTO;
import com.campus.activity.dto.ActivityDetailVO;
import com.campus.activity.dto.ActivityQueryDTO;
import com.campus.activity.dto.ActivityStatsVO;
import com.campus.activity.dto.ActivityVO;
import com.campus.activity.dto.RegistrationExportVO;
import com.campus.activity.dto.WaitingQueueVO;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.Registration;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.mapper.ActivityMapper;
import com.campus.activity.mapper.RegistrationMapper;
import com.campus.activity.service.ActivityService;
import com.campus.activity.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 活动 Service 实现 (学生端 + 教师端) V2.0
 *
 * <p>V2.0 关键变更:
 * <ul>
 *   <li>update: 写 ACTIVITY_UPDATED 通知给所有 ENROLLED/WAITING 学生</li>
 *   <li>delete: 写 ACTIVITY_CANCELLED 通知给所有 ENROLLED/WAITING 学生</li>
 *   <li>stats: 含候补/退选人数</li>
 *   <li>pageMine: 列表项含 waitingCount</li>
 *   <li>pageWaitingQueue / listForExport: 候补队列 + 名单导出</li>
 * </ul>
 *
 * @author 勇闯天涯组
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final ActivityMapper activityMapper;
    private final RegistrationMapper registrationMapper;
    private final NotificationService notificationService;

    // ==================== 学生端 ====================

    @Override
    public IPage<ActivityVO> pageForStudent(ActivityQueryDTO query) {
        Page<Activity> page = new Page<>(query.getCurrent(), query.getSize());

        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<Activity>()
                .orderByDesc(Activity::getPublishTime);

        String keyword = query.getKeyword();
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = "%" + keyword.trim() + "%";
            wrapper.and(w -> w.like(Activity::getTitle, kw)
                    .or().like(Activity::getLocation, kw)
                    .or().like(Activity::getDescription, kw));
        }

        IPage<Activity> result = activityMapper.selectPage(page, wrapper);
        return result.convert(this::toActivityVO);
    }

    @Override
    public ActivityDetailVO getDetail(Long id, String currentAccount) {
        Activity activity = loadActivityOrThrow(id);
        ActivityDetailVO vo = new ActivityDetailVO();
        BeanUtils.copyProperties(activity, vo);

        int enrolled = registrationMapper.countEnrolledByActivity(id);
        vo.setEnrolledCount(enrolled);
        vo.setRemaining(Math.max(0, activity.getMaxParticipants() - enrolled));

        // V2.0: 本人报名状态 (ENROLLED / WAITING / null)
        LambdaQueryWrapper<Registration> myWrapper = new LambdaQueryWrapper<Registration>()
                .eq(Registration::getActivityId, id)
                .eq(Registration::getStudentAccount, currentAccount)
                .in(Registration::getStatus,
                        Registration.STATUS_ENROLLED, Registration.STATUS_WAITING);
        Registration myReg = registrationMapper.selectOne(myWrapper);
        if (myReg != null) {
            vo.setMyStatus(myReg.getStatus());
            vo.setEnrolledByMe(Registration.STATUS_ENROLLED.equals(myReg.getStatus()));
            vo.setWaitingByMe(Registration.STATUS_WAITING.equals(myReg.getStatus()));
            vo.setMyWaitOrder(myReg.getWaitOrder());
        } else {
            vo.setMyStatus(null);
            vo.setEnrolledByMe(false);
            vo.setWaitingByMe(false);
            vo.setMyWaitOrder(null);
        }
        return vo;
    }

    // ==================== 教师端 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publish(ActivityDTO dto, String teacherAccount) {
        validateActivityTime(dto);

        Activity activity = new Activity();
        BeanUtils.copyProperties(dto, activity);
        activity.setTeacherAccount(teacherAccount);
        activity.setPublishTime(LocalDateTime.now());
        activity.setDeleted(0);

        activityMapper.insert(activity);
        log.info("教师 {} 发布活动成功: id={}, title={}",
                teacherAccount, activity.getActivityId(), activity.getTitle());
        return activity.getActivityId();
    }

    @Override
    public IPage<ActivityVO> pageMine(String teacherAccount, Long current, Long size) {
        Page<Activity> page = new Page<>(current, size);

        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<Activity>()
                .eq(Activity::getTeacherAccount, teacherAccount)
                .orderByDesc(Activity::getPublishTime);

        IPage<Activity> result = activityMapper.selectPage(page, wrapper);

        // 批量查 enrolledCount / waitingCount, 避免 N+1
        List<Map<String, Object>> statsList = activityMapper.countEnrolledByTeacher(teacherAccount);
        Map<Long, int[]> statsMap = new HashMap<>();
        for (Map<String, Object> row : statsList) {
            Object idVal = row.get("activityId");
            Object enrolledVal = row.get("enrolledCount");
            Object waitingVal = row.get("waitingCount");
            if (idVal != null) {
                int enrolled = enrolledVal != null ? ((Number) enrolledVal).intValue() : 0;
                int waiting = waitingVal != null ? ((Number) waitingVal).intValue() : 0;
                statsMap.put(((Number) idVal).longValue(), new int[]{enrolled, waiting});
            }
        }

        return result.convert(a -> {
            ActivityVO vo = new ActivityVO();
            BeanUtils.copyProperties(a, vo);
            int[] arr = statsMap.getOrDefault(a.getActivityId(), new int[]{0, 0});
            vo.setEnrolledCount(arr[0]);
            vo.setRemaining(Math.max(0, a.getMaxParticipants() - arr[0]));
            return vo;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ActivityDTO dto, String teacherAccount) {
        // 1. 越权校验
        Activity activity = loadActivityOrThrow(id);
        checkOwner(activity, teacherAccount);

        // 2. 业务规则校验
        validateActivityTime(dto);

        // 3. 若修改了 maxParticipants, 新上限不得小于当前 enrolledCount
        int enrolled = registrationMapper.countEnrolledByActivity(id);
        if (dto.getMaxParticipants() != null && dto.getMaxParticipants() < enrolled) {
            throw new BusinessException(ResultCode.MAX_LESS_THAN_ENROLLED);
        }

        // 4. 落库
        BeanUtils.copyProperties(dto, activity);
        activity.setActivityId(id);
        activity.setTeacherAccount(teacherAccount);
        activityMapper.updateById(activity);

        // 5. V2.0: 写 ACTIVITY_UPDATED 通知给所有 ENROLLED/WAITING 学生
        notificationService.notifyActivityUpdated(id, activity.getTitle());

        log.info("教师 {} 修改活动: id={}", teacherAccount, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, String teacherAccount) {
        // 1. 越权校验
        Activity activity = loadActivityOrThrow(id);
        checkOwner(activity, teacherAccount);

        // 2. V2.0: 写 ACTIVITY_CANCELLED 通知给所有 ENROLLED/WAITING 学生 (在置 INVALID 之前)
        notificationService.notifyActivityCancelled(id, activity.getTitle());

        // 3. 报名记录批量置 INVALID (ENROLLED + WAITING 都置失效)
        Registration template = new Registration();
        template.setStatus(Registration.STATUS_INVALID);

        LambdaQueryWrapper<Registration> updateWrapper = new LambdaQueryWrapper<Registration>()
                .eq(Registration::getActivityId, id)
                .in(Registration::getStatus,
                        Registration.STATUS_ENROLLED, Registration.STATUS_WAITING);
        registrationMapper.update(template, updateWrapper);

        // 4. 活动逻辑删除 (@TableLogic 自动置 deleted=1)
        activityMapper.deleteById(id);
        log.info("教师 {} 删除活动: id={}, 报名记录已批量置 INVALID", teacherAccount, id);
    }

    @Override
    public ActivityStatsVO stats(Long id, String teacherAccount) {
        Activity activity = loadActivityOrThrow(id);
        checkOwner(activity, teacherAccount);

        int enrolled = registrationMapper.countEnrolledByActivity(id);
        int waiting = registrationMapper.countWaitingByActivity(id);

        ActivityStatsVO vo = new ActivityStatsVO();
        vo.setActivityId(activity.getActivityId());
        vo.setTitle(activity.getTitle());
        vo.setMaxParticipants(activity.getMaxParticipants());
        vo.setEnrolledCount(enrolled);
        vo.setRemaining(Math.max(0, activity.getMaxParticipants() - enrolled));
        vo.setWaitingCount(waiting);

        // 退选人数
        LambdaQueryWrapper<Registration> cancelledWrapper = new LambdaQueryWrapper<Registration>()
                .eq(Registration::getActivityId, id)
                .eq(Registration::getStatus, Registration.STATUS_CANCELLED);
        vo.setCancelledCount(Math.toIntExact(registrationMapper.selectCount(cancelledWrapper)));

        return vo;
    }

    // ==================== V2.0 教师端新增 ====================

    @Override
    public IPage<WaitingQueueVO> pageWaitingQueue(Long activityId, String teacherAccount, Long current, Long size) {
        // 越权校验
        Activity activity = loadActivityOrThrow(activityId);
        checkOwner(activity, teacherAccount);

        Page<WaitingQueueVO> page = new Page<>(current, size);
        return registrationMapper.pageWaitingQueue(page, activityId);
    }

    @Override
    public List<RegistrationExportVO> listForExport(Long activityId, String teacherAccount) {
        // 越权校验
        Activity activity = loadActivityOrThrow(activityId);
        checkOwner(activity, teacherAccount);

        return registrationMapper.listForExport(activityId);
    }

    // ==================== 私有辅助方法 ====================

    private Activity loadActivityOrThrow(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ResultCode.ACTIVITY_NOT_FOUND);
        }
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException(ResultCode.ACTIVITY_NOT_FOUND);
        }
        return activity;
    }

    private void checkOwner(Activity activity, String currentAccount) {
        if (activity.getTeacherAccount() == null
                || !activity.getTeacherAccount().equals(currentAccount)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作他人活动");
        }
    }

    private void validateActivityTime(ActivityDTO dto) {
        if (dto.getSignupDeadline() != null && dto.getActivityTime() != null
                && !dto.getSignupDeadline().isBefore(dto.getActivityTime())) {
            throw new BusinessException(ResultCode.PARAM_INVALID,
                    "报名截止时间必须早于活动举办时间");
        }
    }

    private ActivityVO toActivityVO(Activity a) {
        ActivityVO vo = new ActivityVO();
        BeanUtils.copyProperties(a, vo);
        int enrolled = registrationMapper.countEnrolledByActivity(a.getActivityId());
        vo.setEnrolledCount(enrolled);
        vo.setRemaining(Math.max(0, a.getMaxParticipants() - enrolled));
        return vo;
    }
}
