package com.preppal.module.lesson.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 编辑保存教案内容。
 * 所有字段选填，只更新非空字段；保存后教案状态一律回到 DRAFT
 * （定稿请调用 POST /api/lesson-plans/{id}/finalize）。
 */
@Data
public class LessonPlanUpdateRequest {

    @Size(max = 256, message = "教案标题过长")
    private String title;

    @Size(max = 256, message = "教学主题过长")
    private String topic;

    @Size(max = 128, message = "授课对象过长")
    private String targetAudience;

    @Min(value = 10, message = "课时长需在 10–180 分钟之间")
    @Max(value = 180, message = "课时长需在 10–180 分钟之间")
    private Integer classDuration;

    @Size(max = 16, message = "教学类型不合法")
    private String teachingType;

    private String objectives;

    private String keyPoints;

    private String difficultPoints;

    /** JSON 字符串 */
    private String teachingProcess;

    private String blackboardDesign;

    private String homework;
}
