package com.campus.activity.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.activity.dto.ActivityDTO;
import com.campus.activity.dto.ActivityDetailVO;
import com.campus.activity.dto.ActivityStatsVO;
import com.campus.activity.dto.ActivityVO;
import com.campus.activity.dto.ActivityQueryDTO;
import com.campus.activity.dto.RegistrationExportVO;
import com.campus.activity.dto.WaitingQueueVO;

import java.util.List;

/**
 * 活动 Service (学生端 + 教师端共用)
 *
 * <p>V2.0 新增:
 * <ul>
 *   <li>update: 写 ACTIVITY_UPDATED 通知</li>
 *   <li>delete: 写 ACTIVITY_CANCELLED 通知</li>
 *   <li>stats: 含候补/退选人数</li>
 *   <li>pageWaitingQueue: 教师端候补队列</li>
 *   <li>listForExport: 教师端名单导出</li>
 * </ul>
 *
 * @author 勇闯天涯组
 */
public interface ActivityService {

    // ---------------- 学生端 ----------------

    /**
     * 学生端: 分页搜索活动, 按 publish_time DESC
     */
    IPage<ActivityVO> pageForStudent(ActivityQueryDTO query);

    /**
     * 学生端: 活动详情, 含已报人数/剩余名额/本人报名状态(ENROLLED/WAITING)
     */
    ActivityDetailVO getDetail(Long id, String currentAccount);

    // ---------------- 教师端 ----------------

    /**
     * 教师端: 发布活动
     */
    Long publish(ActivityDTO dto, String teacherAccount);

    /**
     * 教师端: 本人发布的活动列表, 每条带 enrolledCount/remaining/waitingCount
     */
    IPage<ActivityVO> pageMine(String teacherAccount, Long current, Long size);

    /**
     * 教师端: 修改本人活动 (越权校验 + 写 ACTIVITY_UPDATED 通知)
     */
    void update(Long id, ActivityDTO dto, String teacherAccount);

    /**
     * 教师端: 删除本人活动 (逻辑删除, 报名记录置 INVALID + 写 ACTIVITY_CANCELLED 通知)
     */
    void delete(Long id, String teacherAccount);

    /**
     * 教师端: 报名统计 (含候补/退选人数)
     */
    ActivityStatsVO stats(Long id, String teacherAccount);

    // ---------------- V2.0 教师端新增 ----------------

    /**
     * 教师端: 候补队列分页 (按 wait_order ASC)
     */
    IPage<WaitingQueueVO> pageWaitingQueue(Long activityId, String teacherAccount, Long current, Long size);

    /**
     * 教师端: 报名名单导出 (学号/姓名/年级/报名时间/状态)
     */
    List<RegistrationExportVO> listForExport(Long activityId, String teacherAccount);
}
