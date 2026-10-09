package com.campus.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 活动表 activity
 *
 * <p>V2.0 新增 target_grade: 允许报名的年级, 逗号分隔, 空=不限</p>
 * <p>逻辑删除: deleted=1, 由 @TableLogic 自动处理</p>
 *
 * @author 勇闯天涯组
 */
@Data
@TableName("activity")
@Schema(description = "活动")
public class Activity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @Schema(description = "活动ID")
    private Long activityId;

    @Schema(description = "活动名称")
    private String title;

    @Schema(description = "活动举办时间")
    private LocalDateTime activityTime;

    @Schema(description = "地点")
    private String location;

    @Schema(description = "活动简介")
    private String description;

    @Schema(description = "报名人数上限")
    private Integer maxParticipants;

    @Schema(description = "报名截止时间")
    private LocalDateTime signupDeadline;

    @Schema(description = "发布教师账号")
    private String teacherAccount;

    @Schema(description = "允许报名的年级, 逗号分隔如 2024,2025; 空=不限")
    private String targetGrade;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

    /**
     * 逻辑删除: 0 正常 1 已删除
     * 删除活动后, 列表自动过滤; 历史报名记录保留为 INVALID
     */
    @TableLogic
    @Schema(description = "逻辑删除: 0 正常 1 已删除")
    private Integer deleted;
}
