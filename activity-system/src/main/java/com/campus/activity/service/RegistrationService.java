package com.campus.activity.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.activity.dto.RegisterResultVO;
import com.campus.activity.dto.RegistrationVO;

/**
 * 报名 Service (学生端)
 *
 * <p>V2.0 变更:
 * <ul>
 *   <li>register: 三分支报名 (资格→时间→防重复; 通过后判名额 ENROLLED/WAITING)</li>
 *   <li>withdraw: 退选 + 同事务触发候补递补 + 写通知</li>
 *   <li>listMine: 增加 WAITING 派生状态</li>
 * </ul>
 *
 * @author 勇闯天涯组
 */
public interface RegistrationService {

    /**
     * 报名活动 (REQ-01)
     *
     * <p>校验链顺序: 资格(年级) → 时间(报名截止) → 防重复。
     * 通过后判断名额: 未满 → ENROLLED; 已满 → WAITING (分配 wait_order)</p>
     *
     * @return 报名结果 (区分 ENROLLED / WAITING, 前端按此提示)
     */
    RegisterResultVO register(Long activityId, String studentAccount);

    /**
     * 退选活动 (REQ-02)
     *
     * <p>校验本人 + ENROLLED + 活动未结束 → CANCELLED
     * 同一事务内触发递补: 取队首 WAITING → ENROLLED, 写学生/教师通知</p>
     */
    void withdraw(Long activityId, String studentAccount);

    /**
     * 我的报名记录 (REQ-03)
     *
     * @param studentAccount 学生账号
     * @param status         状态过滤 (ENROLLED/WAITING/CANCELLED/INVALID), null 返回全部
     * @param current        当前页
     * @param size           每页大小
     */
    IPage<RegistrationVO> listMine(String studentAccount, String status, Long current, Long size);
}
