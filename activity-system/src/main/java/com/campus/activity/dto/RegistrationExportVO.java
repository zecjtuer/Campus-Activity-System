package com.campus.activity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 报名名单导出 VO (教师端) - V2.0 新增
 *
 * <p>导出字段: 学号 / 姓名 / 年级 / 报名时间 / 状态</p>
 *
 * @author 勇闯天涯组
 */
@Data
@Schema(description = "报名名单导出项")
public class RegistrationExportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "记录ID")
    private Long recordId;

    @Schema(description = "学生账号")
    private String studentAccount;

    @Schema(description = "学号")
    private String studentNo;

    @Schema(description = "学生姓名")
    private String studentName;

    @Schema(description = "年级")
    private String grade;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "报名时间")
    private LocalDateTime signupTime;

    @Schema(description = "状态: ENROLLED/WAITING/CANCELLED/INVALID")
    private String status;

    @Schema(description = "候补序号 (WAITING 时展示)")
    private Integer waitOrder;
}
