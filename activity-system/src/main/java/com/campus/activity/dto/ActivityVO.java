package com.campus.activity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 活动列表项 VO (学生端 / 教师端共用)
 *
 * <p>V2.0 新增 targetGrade</p>
 *
 * @author 勇闯天涯组
 */
@Data
@Schema(description = "活动列表项")
public class ActivityVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "活动ID")
    private Long activityId;

    @Schema(description = "活动名称")
    private String title;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "活动举办时间")
    private LocalDateTime activityTime;

    @Schema(description = "地点")
    private String location;

    @Schema(description = "活动简介")
    private String description;

    @Schema(description = "报名人数上限")
    private Integer maxParticipants;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "报名截止时间")
    private LocalDateTime signupDeadline;

    @Schema(description = "发布教师账号")
    private String teacherAccount;

    @Schema(description = "允许报名的年级, 逗号分隔, 空=不限")
    private String targetGrade;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

    @Schema(description = "已报人数")
    private Integer enrolledCount;

    @Schema(description = "剩余名额")
    private Integer remaining;
}
