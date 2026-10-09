package com.campus.activity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 活动分页查询 DTO (学生端浏览/搜索)
 *
 * @author 勇闯天涯组
 */
@Data
@Schema(description = "活动分页查询参数")
public class ActivityQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "当前页", example = "1")
    private Long current = 1L;

    @Schema(description = "每页大小", example = "10")
    private Long size = 10L;

    @Schema(description = "关键词 (在 title/location/description 上做 LIKE 匹配, 空则不过滤)",
            example = "篮球")
    private String keyword;
}
