package com.campus.activity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 学生"我的报名记录" VO
 *
 * <p>V2.0 新增 waitOrder (候补序号) 与 WAITING 派生状态</p>
 *
 * <p>displayStatus 派生状态由 Service 计算:
 * <ul>
 *   <li>ENROLLED + now &lt; activityTime → ONGOING (进行中)</li>
 *   <li>ENROLLED + now &gt;= activityTime → FINISHED (已完成)</li>
 *   <li>WAITING   → 透传 WAITING</li>
 *   <li>CANCELLED → 透传 CANCELLED</li>
 *   <li>INVALID   → 透传 INVALID</li>
 * </ul>
 *
 * @author 勇闯天涯组
 */
@Data
@Schema(description = "学生报名记录")
public class RegistrationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "记录ID")
    private Long recordId;

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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "报名时间")
    private LocalDateTime signupTime;

    @Schema(description = "原始状态: ENROLLED/WAITING/CANCELLED/INVALID")
    private String status;

    @Schema(description = "候补序号 (WAITING 状态下展示)")
    private Integer waitOrder;

    @Schema(description = "展示状态: ONGOING/FINISHED/WAITING/CANCELLED/INVALID")
    private String displayStatus;
}
