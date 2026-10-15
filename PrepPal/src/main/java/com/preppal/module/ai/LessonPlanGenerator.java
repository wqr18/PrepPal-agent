package com.preppal.module.ai;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.preppal.common.BusinessException;
import com.preppal.common.ResultCode;
import com.preppal.module.ai.dto.GeneratedLessonPlan;
import com.preppal.module.ai.dto.LessonGenerationInput;
import com.preppal.module.ai.dto.LessonPlanDraft;
import com.preppal.module.ai.dto.ReferenceSnippet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 教案生成：拼装 Prompt → 调用模型 → 解析 JSON → 还原引用来源 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LessonPlanGenerator {

    private static final String NO_REFERENCE_BLOCK = "（本次未检索到可用的上传资料，请基于通用学科知识生成）";
    private static final String REFERENCE_START = "<<<UNTRUSTED_REFERENCE_START>>>";
    private static final String REFERENCE_END = "<<<UNTRUSTED_REFERENCE_END>>>";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final LlmClient llmClient;
    private final LessonPlanPrompts prompts;

    public GeneratedLessonPlan generate(LessonGenerationInput input) {
        List<ReferenceSnippet> references = input.getReferences() == null
                ? Collections.emptyList()
                : input.getReferences();

        String userPrompt = prompts.user(buildVariables(input, references));
        String raw = llmClient.complete(prompts.system(), userPrompt);
        LessonPlanDraft draft = parse(raw);

        List<LessonPlanDraft.ProcessStage> stages = draft.getTeachingProcess() == null
                ? Collections.emptyList()
                : draft.getTeachingProcess();

        return new GeneratedLessonPlan(draft, stages, resolveUsedReferences(draft, references), raw);
    }

    private Map<String, String> buildVariables(LessonGenerationInput input, List<ReferenceSnippet> references) {
        Map<String, String> variables = new HashMap<>();
        variables.put("courseName", input.getCourseName());
        variables.put("topic", input.getTopic());
        variables.put("targetAudience", input.getTargetAudience());
        variables.put("classDuration", input.getClassDuration() == null ? "" : String.valueOf(input.getClassDuration()));
        variables.put("teachingType", input.getTeachingType());
        variables.put("customObjectives", input.getCustomObjectives());
        variables.put("referenceStatus", references.isEmpty()
                ? "未检索到与本次主题相关的上传资料。"
                : "共检索到 " + references.size() + " 段相关资料，见下方不可信参考资料区段。");
        variables.put("referenceBlock", renderReferences(references));
        return variables;
    }

    private String renderReferences(List<ReferenceSnippet> references) {
        if (references.isEmpty()) {
            return NO_REFERENCE_BLOCK;
        }
        StringBuilder builder = new StringBuilder();
        builder.append(REFERENCE_START).append('\n');
        for (ReferenceSnippet snippet : references) {
            builder.append("[引用编号 ").append(snippet.getRefNo()).append("] ")
                    .append("文件：").append(snippet.getMaterialName())
                    .append("（分片 ").append(snippet.getChunkIndex()).append("）\n")
                    .append("内容：").append(snippet.getContent()).append('\n')
                    .append("---\n");
        }
        builder.append(REFERENCE_END);
        return builder.toString();
    }

    /** 把模型回引的「引用编号」还原成真实的资料 ID 与分片序号 */
    private List<ReferenceSnippet> resolveUsedReferences(LessonPlanDraft draft, List<ReferenceSnippet> references) {
        if (references.isEmpty()) {
            return Collections.emptyList();
        }
        List<LessonPlanDraft.UsedReference> used = draft.getUsedReferences();
        if (used == null || used.isEmpty()) {
            // 模型未回引编号时按「全部资料参与生成」处理，避免引用区为空
            log.info("模型未返回 usedReferences，按全部 {} 段资料标注引用", references.size());
            return new ArrayList<>(references);
        }
        Map<Integer, ReferenceSnippet> byRefNo = new HashMap<>();
        for (ReferenceSnippet snippet : references) {
            byRefNo.put(snippet.getRefNo(), snippet);
        }
        List<ReferenceSnippet> resolved = new ArrayList<>();
        for (LessonPlanDraft.UsedReference item : used) {
            if (item == null || item.getRefNo() == null) {
                continue;
            }
            ReferenceSnippet snippet = byRefNo.get(item.getRefNo());
            if (snippet == null) {
                log.warn("模型返回了不存在的引用编号 {}，已忽略", item.getRefNo());
                continue;
            }
            ReferenceSnippet copy = new ReferenceSnippet();
            copy.setRefNo(snippet.getRefNo());
            copy.setMaterialId(snippet.getMaterialId());
            copy.setMaterialName(snippet.getMaterialName());
            copy.setChunkIndex(snippet.getChunkIndex());
            copy.setContent(item.getExcerpt() == null || item.getExcerpt().isBlank()
                    ? snippet.getContent()
                    : item.getExcerpt());
            resolved.add(copy);
        }
        if (resolved.isEmpty()) {
            log.warn("模型回引的编号均无法匹配，按全部资料标注引用");
            return new ArrayList<>(references);
        }
        return resolved;
    }

    private LessonPlanDraft parse(String raw) {
        String json = extractJson(raw);
        try {
            LessonPlanDraft draft = MAPPER.readValue(json, LessonPlanDraft.class);
            if (draft == null) {
                throw new BusinessException(ResultCode.AI_ERROR, "模型未返回教案内容，请重试");
            }
            return draft;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("模型输出不是合法 JSON，前 200 字：{}", preview(raw));
            throw new BusinessException(ResultCode.AI_ERROR, "模型输出格式异常，请重试");
        }
    }

    /** 容忍模型把 JSON 包在 ```json 围栏里或前后带解释文字 */
    private String extractJson(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException(ResultCode.AI_ERROR, "模型返回为空，请重试");
        }
        String text = raw.trim();
        if (text.startsWith("```")) {
            int firstLineBreak = text.indexOf('\n');
            if (firstLineBreak > 0) {
                text = text.substring(firstLineBreak + 1);
            }
            int fence = text.lastIndexOf("```");
            if (fence >= 0) {
                text = text.substring(0, fence);
            }
            text = text.trim();
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new BusinessException(ResultCode.AI_ERROR, "模型输出中未找到 JSON 内容，请重试");
        }
        return text.substring(start, end + 1);
    }

    private String preview(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.length() <= 200 ? raw : raw.substring(0, 200);
    }
}
