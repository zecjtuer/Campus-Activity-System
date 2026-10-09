package com.campus.activity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 候补队列项 VO (教师端查看) - V2.0 新增
 *
 * @author 勇闯天涯组
 */
@Data
@Schema(description = "候补队列项")
public class WaitingQueueVO implements Serializable {

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

    @Schema(description = "候补序号")
    private Integer waitOrder;
}
