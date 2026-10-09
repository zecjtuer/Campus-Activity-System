package com.campus.activity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 活动发布/修改 DTO (教师端)
 *
 * <p>V2.0 新增 targetGrade: 允许报名的年级, 逗号分隔, 空=不限</p>
 *
 * @author 勇闯天涯组
 */
@Data
@Schema(description = "活动发布/修改请求")
public class ActivityDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "活动名称不能为空")
    @Size(max = 100, message = "活动名称长度不能超过 100")
    @Schema(description = "活动名称", example = "校园篮球赛")
    private String title;

    @NotNull(message = "活动举办时间不能为空")
    @Future(message = "活动举办时间必须晚于当前时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "活动举办时间", example = "2026-10-20 14:00:00")
    private LocalDateTime activityTime;

    @NotBlank(message = "地点不能为空")
    @Size(max = 100, message = "地点长度不能超过 100")
    @Schema(description = "地点", example = "东区体育馆")
    private String location;

    @Size(max = 500, message = "活动简介长度不能超过 500")
    @Schema(description = "活动简介")
    private String description;

    @NotNull(message = "报名人数上限不能为空")
    @Min(value = 1, message = "报名人数上限必须大于 0")
    @Schema(description = "报名人数上限", example = "30")
    private Integer maxParticipants;

    @NotNull(message = "报名截止时间不能为空")
    @Future(message = "报名截止时间必须晚于当前时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "报名截止时间", example = "2026-10-18 23:59:59")
    private LocalDateTime signupDeadline;

    @Size(max = 100, message = "目标年级长度不能超过 100")
    @Schema(description = "允许报名年级, 逗号分隔如 2024,2025; 空=不限")
    private String targetGrade;
}
