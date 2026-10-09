package com.campus.activity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 站内通知 VO (学生/教师端消息列表) - V2.0 新增
 *
 * @author 勇闯天涯组
 */
@Data
@Schema(description = "站内通知")
public class NotificationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "通知ID")
    private Long id;

    @Schema(description = "关联活动ID")
    private Long activityId;

    @Schema(description = "关联活动标题 (联表查询)")
    private String activityTitle;

    @Schema(description = "通知类型: ENROLLED/CANCELLED/PROMOTED/ACTIVITY_CANCELLED/ACTIVITY_UPDATED")
    private String type;

    @Schema(description = "通知内容")
    private String content;

    @Schema(description = "0 未读 1 已读")
    private Integer isRead;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "生成时间")
    private LocalDateTime createdAt;
}
