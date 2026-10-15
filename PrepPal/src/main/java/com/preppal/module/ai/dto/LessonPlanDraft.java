package com.preppal.module.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/** 模型输出的教案草稿（与 Prompt 中约定的 JSON 结构一一对应） */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LessonPlanDraft {

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProcessStage {
        private String stage;
        private String duration;
        private String content;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UsedReference {
        private Integer refNo;
        private String excerpt;
    }

    private String title;

    private String objectives;

    private String keyPoints;

    private String difficultPoints;

    private List<ProcessStage> teachingProcess;

    private String blackboardDesign;

    private String homework;

    private List<UsedReference> usedReferences;
}
