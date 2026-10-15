package com.preppal.module.ai.dto;

import com.preppal.module.ai.dto.LessonPlanDraft.ProcessStage;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/** 生成结果：解析后的草稿 + 模型原始输出 + 回引到的参考资料 */
@Data
@AllArgsConstructor
public class GeneratedLessonPlan {

    private LessonPlanDraft draft;

    private List<ProcessStage> processStages;

    private List<ReferenceSnippet> usedReferences;

    private String rawContent;
}
