package com.campus.activity.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.activity.dto.NotificationVO;

/**
 * 站内通知 Service (V2.0 新增)
 *
 * <p>对外提供两类能力:
 * <ul>
 *   <li>事件触发写入通知 (报名成功/取消报名/候补递补/活动取消/活动信息变更)</li>
 *   <li>消息列表查询 / 已读标记 / 未读计数</li>
 * </ul>
 *
 * <p>学生收到状态通知; 教师收到本人活动名单变动通知 (报名成功/取消/递补)。</p>
 *
 * @author 勇闯天涯组
 */
public interface NotificationService {

    /**
     * 通知: 学生报名成功
     */
    void notifyEnrolled(String studentAccount, Long activityId, String activityTitle);

    /**
     * 通知: 学生取消报名 (退选)
     *  - 给学生发 CANCELLED
     *  - 给活动发布教师发 CANCELLED (名单变动)
     */
    void notifyCancelled(String studentAccount, Long activityId, String activityTitle, String teacherAccount);

    /**
     * 通知: 候补递补 (退选释放名额, 队首 WAITING → ENROLLED)
     *  - 给递补学生发 PROMOTED
     *  - 给活动发布教师发 PROMOTED (名单变动)
     */
    void notifyPromoted(String studentAccount, Long activityId, String activityTitle, String teacherAccount);

    /**
     * 通知: 活动取消 (教师删除活动)
     *  - 给所有 ENROLLED/WAITING 学生发 ACTIVITY_CANCELLED
     *  - 不给教师发 (教师本人发起的)
     */
    void notifyActivityCancelled(Long activityId, String activityTitle);

    /**
     * 通知: 活动信息变更 (教师修改活动)
     *  - 给所有 ENROLLED/WAITING 学生发 ACTIVITY_UPDATED
     */
    void notifyActivityUpdated(Long activityId, String activityTitle);

    /**
     * 当前用户未读通知数
     */
    int countUnread(String account);

    /**
     * 分页查询通知列表
     *
     * @param account    接收人账号
     * @param onlyUnread true 仅未读
     */
    IPage<NotificationVO> pageMine(String account, Boolean onlyUnread, Long current, Long size);

    /**
     * 单条标记已读
     */
    void markRead(Long id, String account);

    /**
     * 全部标记已读
     */
    void markAllRead(String account);
}
