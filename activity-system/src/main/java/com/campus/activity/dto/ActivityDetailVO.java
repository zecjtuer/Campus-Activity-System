package com.campus.activity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 活动详情 VO (学生端)
 *
 * <p>V2.0 新增:
 * <ul>
 *   <li>myStatus: 当前学生对该活动的报名状态 (ENROLLED/WAITING/none)</li>
 *   <li>myWaitOrder: 当前学生的候补序号 (若处于候补)</li>
 *   <li>targetGrade 已在父类 ActivityVO</li>
 * </ul>
 *
 * @author 勇闯天涯组
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "活动详情 (学生端)")
public class ActivityDetailVO extends ActivityVO {

    private static final long serialVersionUID = 1L;

    @Schema(description = "当前学生是否已报名 (存在 ENROLLED 记录)")
    private Boolean enrolledByMe;

    @Schema(description = "当前学生是否处于候补 (存在 WAITING 记录)")
    private Boolean waitingByMe;

    @Schema(description = "当前学生候补序号 (WAITING 时展示)")
    private Integer myWaitOrder;

    @Schema(description = "当前学生对该活动的状态: ENROLLED/WAITING/null")
    private String myStatus;
}
