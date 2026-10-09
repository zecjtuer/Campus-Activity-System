package com.campus.activity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 报名结果 VO (V2.0) - 区分 ENROLLED / WAITING 给前端不同提示
 *
 * @author 勇闯天涯组
 */
@Data
@Schema(description = "报名结果")
public class RegisterResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "结果状态: ENROLLED 报名成功 / WAITING 候补登记")
    private String status;

    @Schema(description = "候补序号 (WAITING 时返回)")
    private Integer waitOrder;

    @Schema(description = "提示信息")
    private String message;
}
