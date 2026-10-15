package com.preppal.module.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** 生成教案的输入上下文 */
@Data
@Builder
public class LessonGenerationInput {

    private String courseName;

    private String topic;

    private String targetAudience;

    private Integer classDuration;

    private String teachingType;

    private String customObjectives;

    private List<ReferenceSnippet> references;
}
