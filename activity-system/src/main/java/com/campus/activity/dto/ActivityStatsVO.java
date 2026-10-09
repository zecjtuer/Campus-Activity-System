package com.campus.activity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 报名统计 VO (教师端)
 *
 * <p>V2.0 新增 waitingCount (候补人数)</p>
 *
 * @author 勇闯天涯组
 */
@Data
@Schema(description = "报名统计")
public class ActivityStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "活动ID")
    private Long activityId;

    @Schema(description = "活动名称")
    private String title;

    @Schema(description = "报名人数上限")
    private Integer maxParticipants;

    @Schema(description = "已报人数 (status=ENROLLED)")
    private Integer enrolledCount;

    @Schema(description = "剩余名额 (上限 - 已报)")
    private Integer remaining;

    @Schema(description = "候补人数 (status=WAITING)")
    private Integer waitingCount;

    @Schema(description = "已退选人数 (status=CANCELLED)")
    private Integer cancelledCount;
}
