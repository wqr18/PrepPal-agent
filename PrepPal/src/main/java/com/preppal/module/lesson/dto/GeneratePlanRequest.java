package com.preppal.module.lesson.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 生成教案请求 */
@Data
public class GeneratePlanRequest {

    @NotNull(message = "缺少 courseId")
    private Long courseId;

    @NotBlank(message = "请填写教学主题")
    @Size(max = 40, message = "教学主题不超过 40 个字")
    private String topic;

    @NotBlank(message = "请填写授课对象")
    @Size(max = 30, message = "授课对象不超过 30 个字")
    private String targetAudience;

    @NotNull(message = "请填写课时长")
    @Min(value = 10, message = "课时长需在 10–180 分钟之间")
    @Max(value = 180, message = "课时长需在 10–180 分钟之间")
    private Integer classDuration;

    @NotBlank(message = "请选择教学类型")
    @Size(max = 16, message = "教学类型不合法")
    private String teachingType;

    @Size(max = 200, message = "自定义教学目标不超过 200 个字")
    private String customObjectives;

    /** 由前端生成的 UUID，用于防止重复点击造成重复生成 */
    @Size(max = 64, message = "幂等键过长")
    private String idempotencyKey;
}
