package com.campus.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 报名记录表 registration
 *
 * <p>V2.0 状态四态:
 * <ul>
 *   <li>ENROLLED  已报名</li>
 *   <li>WAITING   候补中 (名额满时分配, 携 wait_order)</li>
 *   <li>CANCELLED 已退选 (学生主动退选)</li>
 *   <li>INVALID   已失效 (活动被删除)</li>
 * </ul>
 *
 * @author 勇闯天涯组
 */
@Data
@TableName("registration")
@Schema(description = "报名记录")
public class Registration implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @Schema(description = "记录ID")
    private Long recordId;

    @Schema(description = "学生账号")
    private String studentAccount;

    @Schema(description = "活动ID")
    private Long activityId;

    @Schema(description = "状态 ENROLLED/WAITING/CANCELLED/INVALID")
    private String status;

    @Schema(description = "候补序号 (WAITING 状态下分配, 递补后置空)")
    private Integer waitOrder;

    @Schema(description = "报名时间")
    private LocalDateTime signupTime;

    // ---------- 状态常量 ----------

    /** 已报名 */
    public static final String STATUS_ENROLLED = "ENROLLED";
    /** 候补中 */
    public static final String STATUS_WAITING = "WAITING";
    /** 已退选 */
    public static final String STATUS_CANCELLED = "CANCELLED";
    /** 已失效 (活动被删) */
    public static final String STATUS_INVALID = "INVALID";

    // ---------- 派生展示状态常量 (Service 计算, 不入库) ----------

    /** 进行中: ENROLLED 且 now &lt; activity_time */
    public static final String DISPLAY_ONGOING = "ONGOING";
    /** 已完成: ENROLLED 且 now &gt;= activity_time */
    public static final String DISPLAY_FINISHED = "FINISHED";
    /** 候补中: WAITING 状态展示 */
    public static final String DISPLAY_WAITING = "WAITING";
}
