package com.campus.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 站内通知表 notifications (V2.0 新增)
 *
 * <p>5 类事件自动写入:
 * <ul>
 *   <li>ENROLLED          报名成功 (学生收)</li>
 *   <li>CANCELLED         取消报名 (学生收)</li>
 *   <li>PROMOTED          候补递补 (学生收)</li>
 *   <li>ACTIVITY_CANCELLED 活动取消 (学生+教师收)</li>
 *   <li>ACTIVITY_UPDATED  活动信息变更 (学生收)</li>
 * </ul>
 *
 * <p>学生收到状态通知; 教师收到本人活动名单变动通知 (报名成功/取消/递补)</p>
 *
 * @author 勇闯天涯组
 */
@Data
@TableName("notifications")
@Schema(description = "站内通知")
public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @Schema(description = "通知ID")
    private Long id;

    @Schema(description = "接收人账号")
    private String receiverAccount;

    @Schema(description = "接收人角色 STUDENT/TEACHER/ADMIN")
    private String receiverRole;

    @Schema(description = "关联活动ID (可空)")
    private Long activityId;

    @Schema(description = "通知类型: ENROLLED/CANCELLED/PROMOTED/ACTIVITY_CANCELLED/ACTIVITY_UPDATED")
    private String type;

    @Schema(description = "通知内容")
    private String content;

    @Schema(description = "0 未读 1 已读")
    private Integer isRead;

    @Schema(description = "生成时间")
    private LocalDateTime createdAt;

    // ---------- 通知类型常量 ----------
    /** 报名成功 */
    public static final String TYPE_ENROLLED = "ENROLLED";
    /** 取消报名 */
    public static final String TYPE_CANCELLED = "CANCELLED";
    /** 候补递补 */
    public static final String TYPE_PROMOTED = "PROMOTED";
    /** 活动取消 */
    public static final String TYPE_ACTIVITY_CANCELLED = "ACTIVITY_CANCELLED";
    /** 活动信息变更 */
    public static final String TYPE_ACTIVITY_UPDATED = "ACTIVITY_UPDATED";

    // ---------- 已读状态常量 ----------
    public static final int READ_NO = 0;
    public static final int READ_YES = 1;
}
