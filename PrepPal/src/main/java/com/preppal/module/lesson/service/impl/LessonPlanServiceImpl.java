package com.preppal.module.lesson.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.preppal.common.BusinessException;
import com.preppal.common.ResultCode;
import com.preppal.common.UserContext;
import com.preppal.config.LessonProperties;
import com.preppal.module.ai.LessonPlanGenerator;
import com.preppal.module.ai.LessonPlanPrompts;
import com.preppal.module.ai.LlmClient;
import com.preppal.module.ai.dto.GeneratedLessonPlan;
import com.preppal.module.ai.dto.LessonGenerationInput;
import com.preppal.module.ai.dto.LessonPlanDraft;
import com.preppal.module.ai.dto.ReferenceSnippet;
import com.preppal.module.course.entity.Course;
import com.preppal.module.course.mapper.CourseMapper;
import com.preppal.module.lesson.dto.GeneratePlanRequest;
import com.preppal.module.lesson.dto.GeneratePlanResponse;
import com.preppal.module.lesson.dto.LessonPlanUpdateRequest;
import com.preppal.module.lesson.dto.LessonPlanVO;
import com.preppal.module.lesson.entity.LessonPlan;
import com.preppal.module.lesson.mapper.LessonPlanMapper;
import com.preppal.module.lesson.service.LessonPlanService;
import com.preppal.module.rag.dto.RetrievedChunk;
import com.preppal.module.rag.service.RetrievalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LessonPlanServiceImpl implements LessonPlanService {

    static final String DISCLAIMER_WITH_REFERENCES = "以下内容基于上传资料生成，引用位置已标注。";
    static final String DISCLAIMER_NO_REFERENCE = "以下内容基于通用知识生成，未参考上传资料。";

    private static final int TITLE_MAX_LENGTH = 256;
    private static final int SOURCE_MATERIAL_IDS_MAX_LENGTH = 512;

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final LessonPlanMapper lessonPlanMapper;
    private final CourseMapper courseMapper;
    private final RetrievalService retrievalService;
    private final LessonPlanGenerator lessonPlanGenerator;
    private final LlmClient llmClient;
    private final LessonProperties lessonProperties;
    private final ThreadPoolExecutor lessonGenerateExecutor;

    public LessonPlanServiceImpl(LessonPlanMapper lessonPlanMapper,
                                 CourseMapper courseMapper,
                                 RetrievalService retrievalService,
                                 LessonPlanGenerator lessonPlanGenerator,
                                 LlmClient llmClient,
                                 LessonProperties lessonProperties,
                                 @Qualifier("lessonGenerateExecutor") ThreadPoolExecutor lessonGenerateExecutor) {
        this.lessonPlanMapper = lessonPlanMapper;
        this.courseMapper = courseMapper;
        this.retrievalService = retrievalService;
        this.lessonPlanGenerator = lessonPlanGenerator;
        this.llmClient = llmClient;
        this.lessonProperties = lessonProperties;
        this.lessonGenerateExecutor = lessonGenerateExecutor;
    }

    @Override
    public GeneratePlanResponse generate(GeneratePlanRequest request) {
        Long userId = UserContext.require();
        Course course = courseMapper.selectById(request.getCourseId());
        if (course == null || !userId.equals(course.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "课程不存在");
        }

        // 幂等：同一用户同一幂等键重复提交（含超时后的「重新生成」）直接复用已有结果
        LessonPlan existed = findByIdempotencyKey(userId, request.getIdempotencyKey());
        if (existed != null) {
            log.info("命中幂等键，复用已有教案 id={}", existed.getId());
            return GeneratePlanResponse.success(LessonPlanVO.from(existed, course.getName()));
        }

        Future<GeneratePlanResponse> future = null;
        try {
            future = lessonGenerateExecutor.submit(() -> doGenerate(userId, course, request));
            return future.get(lessonProperties.getGenerateTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("生成教案超时：courseId={}, topic={}", request.getCourseId(), request.getTopic());
            cancelQuietly(future);
            return GeneratePlanResponse.failed(ResultCode.AI_TIMEOUT.getMessage());
        } catch (RejectedExecutionException e) {
            log.warn("生成任务被拒绝，线程池已满");
            return GeneratePlanResponse.failed("当前生成任务较多，请稍后重试");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            cancelQuietly(future);
            return GeneratePlanResponse.failed("生成被中断，请重试");
        } catch (ExecutionException e) {
            log.error("生成教案失败", e.getCause());
            return GeneratePlanResponse.failed(messageOf(e.getCause()));
        }
    }

    /** 在生成线程池中执行：不依赖 UserContext（ThreadLocal 无法跨线程），userId 由入参传入 */
    private GeneratePlanResponse doGenerate(Long userId, Course course, GeneratePlanRequest request) {
        List<RetrievedChunk> chunks = retrievalService.retrieve(userId, course.getId(), buildRetrievalQuery(request));
        List<ReferenceSnippet> references = toSnippets(chunks);

        GeneratedLessonPlan generated = lessonPlanGenerator.generate(LessonGenerationInput.builder()
                .courseName(course.getName())
                .topic(request.getTopic())
                .targetAudience(request.getTargetAudience())
                .classDuration(request.getClassDuration())
                .teachingType(request.getTeachingType())
                .customObjectives(request.getCustomObjectives())
                .references(references)
                .build());

        if (Thread.currentThread().isInterrupted()) {
            // 已被超时取消，不再落库，避免超时后又插入一份教案
            log.warn("生成任务已被取消，放弃落库：topic={}", request.getTopic());
            throw new BusinessException(ResultCode.AI_TIMEOUT, ResultCode.AI_TIMEOUT.getMessage());
        }

        LessonPlan plan = buildLessonPlan(userId, course, request, generated, references);
        try {
            lessonPlanMapper.insert(plan);
        } catch (DuplicateKeyException e) {
            // 并发提交同一幂等键：唯一索引兜底，返回已存在的那一份
            LessonPlan concurrent = findByIdempotencyKey(userId, request.getIdempotencyKey());
            if (concurrent != null) {
                log.info("并发命中幂等键，复用已有教案 id={}", concurrent.getId());
                return GeneratePlanResponse.success(LessonPlanVO.from(concurrent, course.getName()));
            }
            throw e;
        }
        touchCourse(course.getId());
        log.info("教案生成成功 id={}, 引用资料 {} 段", plan.getId(), references.size());
        return GeneratePlanResponse.success(LessonPlanVO.from(lessonPlanMapper.selectById(plan.getId()), course.getName()));
    }

    private LessonPlan buildLessonPlan(Long userId, Course course, GeneratePlanRequest request,
                                       GeneratedLessonPlan generated, List<ReferenceSnippet> references) {
        LessonPlanDraft draft = generated.getDraft();
        LessonPlan plan = new LessonPlan();
        plan.setUserId(userId);
        plan.setCourseId(course.getId());
        plan.setTitle(truncate(StringUtils.hasText(draft.getTitle())
                ? draft.getTitle().trim()
                : request.getTopic() + "教案", TITLE_MAX_LENGTH));
        plan.setTopic(request.getTopic());
        plan.setTargetAudience(request.getTargetAudience());
        plan.setClassDuration(request.getClassDuration());
        plan.setTeachingType(request.getTeachingType());
        plan.setObjectives(nullToEmpty(draft.getObjectives()));
        plan.setKeyPoints(nullToEmpty(draft.getKeyPoints()));
        plan.setDifficultPoints(nullToEmpty(draft.getDifficultPoints()));
        plan.setTeachingProcess(writeJson(toWireStages(generated.getProcessStages())));
        plan.setBlackboardDesign(nullToEmpty(draft.getBlackboardDesign()));
        plan.setHomework(nullToEmpty(draft.getHomework()));
        plan.setSourceReferences(writeJson(toSourceReferences(references)));
        plan.setDisclaimer(references.isEmpty() ? DISCLAIMER_NO_REFERENCE : DISCLAIMER_WITH_REFERENCES);
        plan.setStatus(LessonPlan.STATUS_DRAFT);
        plan.setInputSnapshot(writeJson(request));
        plan.setModelName(llmClient.modelName());
        plan.setPromptVersion(LessonPlanPrompts.VERSION);
        plan.setSourceMaterialIds(joinMaterialIds(references));
        plan.setIdempotencyKey(request.getIdempotencyKey());
        plan.setRawContent(generated.getRawContent());
        return plan;
    }

    @Override
    public List<LessonPlanVO> list(Long courseId, String status) {
        Long userId = UserContext.require();
        LambdaQueryWrapper<LessonPlan> wrapper = new LambdaQueryWrapper<LessonPlan>()
                .eq(LessonPlan::getUserId, userId)
                .orderByDesc(LessonPlan::getUpdatedAt);
        if (courseId != null) {
            requireOwnedCourse(courseId, userId);
            wrapper.eq(LessonPlan::getCourseId, courseId);
        }
        if (StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status.trim())) {
            wrapper.eq(LessonPlan::getStatus, normalizeStatus(status));
        }
        return toVOs(lessonPlanMapper.selectList(wrapper), userId);
    }

    @Override
    public LessonPlanVO detail(Long id) {
        Long userId = UserContext.require();
        LessonPlan plan = requireOwnedPlan(id, userId);
        return LessonPlanVO.from(plan, courseNameOf(plan.getCourseId()));
    }

    /** 保存编辑内容：状态一律回到 DRAFT，定稿请走 /finalize */
    @Override
    public LessonPlanVO update(Long id, LessonPlanUpdateRequest request) {
        Long userId = UserContext.require();
        LessonPlan plan = requireOwnedPlan(id, userId);

        if (StringUtils.hasText(request.getTitle())) {
            plan.setTitle(truncate(request.getTitle().trim(), TITLE_MAX_LENGTH));
        }
        if (StringUtils.hasText(request.getTopic())) {
            plan.setTopic(request.getTopic().trim());
        }
        if (StringUtils.hasText(request.getTargetAudience())) {
            plan.setTargetAudience(request.getTargetAudience().trim());
        }
        if (request.getClassDuration() != null) {
            plan.setClassDuration(request.getClassDuration());
        }
        if (StringUtils.hasText(request.getTeachingType())) {
            plan.setTeachingType(request.getTeachingType().trim());
        }
        if (request.getObjectives() != null) {
            plan.setObjectives(request.getObjectives());
        }
        if (request.getKeyPoints() != null) {
            plan.setKeyPoints(request.getKeyPoints());
        }
        if (request.getDifficultPoints() != null) {
            plan.setDifficultPoints(request.getDifficultPoints());
        }
        if (request.getTeachingProcess() != null) {
            plan.setTeachingProcess(request.getTeachingProcess());
        }
        if (request.getBlackboardDesign() != null) {
            plan.setBlackboardDesign(request.getBlackboardDesign());
        }
        if (request.getHomework() != null) {
            plan.setHomework(request.getHomework());
        }
        plan.setStatus(LessonPlan.STATUS_DRAFT);
        plan.setUpdatedAt(LocalDateTime.now());
        lessonPlanMapper.updateById(plan);

        touchCourse(plan.getCourseId());
        return LessonPlanVO.from(lessonPlanMapper.selectById(id), courseNameOf(plan.getCourseId()));
    }

    @Override
    public LessonPlanVO finalize(Long id) {
        Long userId = UserContext.require();
        LessonPlan plan = requireOwnedPlan(id, userId);
        plan.setStatus(LessonPlan.STATUS_FINALIZED);
        plan.setUpdatedAt(LocalDateTime.now());
        lessonPlanMapper.updateById(plan);
        touchCourse(plan.getCourseId());
        return LessonPlanVO.from(lessonPlanMapper.selectById(id), courseNameOf(plan.getCourseId()));
    }

    @Override
    public void delete(Long id) {
        Long userId = UserContext.require();
        LessonPlan plan = requireOwnedPlan(id, userId);
        lessonPlanMapper.deleteById(id);
        touchCourse(plan.getCourseId());
    }

    /* ---------------- 内部辅助 ---------------- */

    private LessonPlan findByIdempotencyKey(Long userId, String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)) {
            return null;
        }
        return lessonPlanMapper.selectOne(new LambdaQueryWrapper<LessonPlan>()
                .eq(LessonPlan::getUserId, userId)
                .eq(LessonPlan::getIdempotencyKey, idempotencyKey.trim())
                .last("LIMIT 1"));
    }

    private LessonPlan requireOwnedPlan(Long id, Long userId) {
        LessonPlan plan = lessonPlanMapper.selectById(id);
        if (plan == null || !userId.equals(plan.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "教案不存在");
        }
        return plan;
    }

    private void requireOwnedCourse(Long courseId, Long userId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null || !userId.equals(course.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "课程不存在");
        }
    }

    private String normalizeStatus(String status) {
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!LessonPlan.STATUS_DRAFT.equals(normalized) && !LessonPlan.STATUS_FINALIZED.equals(normalized)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "status 仅支持 DRAFT / FINALIZED");
        }
        return normalized;
    }

    private List<LessonPlanVO> toVOs(List<LessonPlan> plans, Long userId) {
        if (plans == null || plans.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> courseIds = new LinkedHashSet<>();
        for (LessonPlan plan : plans) {
            courseIds.add(plan.getCourseId());
        }
        List<Course> courses = courseIds.isEmpty()
                ? Collections.emptyList()
                : courseMapper.selectByIds(new ArrayList<>(courseIds));
        Map<Long, String> courseNames = new HashMap<>();
        for (Course course : courses) {
            if (userId.equals(course.getUserId())) {
                courseNames.put(course.getId(), course.getName());
            }
        }
        return plans.stream()
                .map(plan -> LessonPlanVO.from(plan, courseNames.get(plan.getCourseId())))
                .collect(Collectors.toList());
    }

    private String courseNameOf(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        return course == null ? null : course.getName();
    }

    private void touchCourse(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course != null) {
            course.setUpdatedAt(LocalDateTime.now());
            courseMapper.updateById(course);
        }
    }

    private String buildRetrievalQuery(GeneratePlanRequest request) {
        StringBuilder builder = new StringBuilder(request.getTopic());
        if (StringUtils.hasText(request.getTargetAudience())) {
            builder.append(' ').append(request.getTargetAudience());
        }
        if (StringUtils.hasText(request.getCustomObjectives())) {
            String objectives = request.getCustomObjectives().trim();
            builder.append(' ').append(objectives.length() > 60 ? objectives.substring(0, 60) : objectives);
        }
        return builder.toString();
    }

    private List<ReferenceSnippet> toSnippets(List<RetrievedChunk> chunks) {
        List<ReferenceSnippet> snippets = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            RetrievedChunk chunk = chunks.get(i);
            ReferenceSnippet snippet = new ReferenceSnippet();
            snippet.setRefNo(i + 1);
            snippet.setMaterialId(chunk.getMaterialId());
            snippet.setMaterialName(chunk.getMaterialName());
            snippet.setChunkIndex(chunk.getChunkIndex());
            snippet.setContent(chunk.getContent());
            snippets.add(snippet);
        }
        return snippets;
    }

    /** 草稿环节转换成入库格式：duration 由数字分钟补成 "5min" */
    private List<Map<String, String>> toWireStages(List<LessonPlanDraft.ProcessStage> stages) {
        List<Map<String, String>> result = new ArrayList<>();
        for (LessonPlanDraft.ProcessStage stage : stages) {
            if (stage == null) {
                continue;
            }
            Map<String, String> item = new LinkedHashMap<>();
            item.put("stage", nullToEmpty(stage.getStage()));
            item.put("duration", normalizeDuration(stage.getDuration()));
            item.put("content", nullToEmpty(stage.getContent()));
            result.add(item);
        }
        return result;
    }

    private String normalizeDuration(String duration) {
        if (!StringUtils.hasText(duration)) {
            return "";
        }
        String trimmed = duration.trim();
        return trimmed.matches("\\d+") ? trimmed + "min" : trimmed;
    }

    private List<Map<String, Object>> toSourceReferences(List<ReferenceSnippet> references) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ReferenceSnippet snippet : references) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("materialId", snippet.getMaterialId());
            item.put("materialName", snippet.getMaterialName());
            item.put("chunkIndex", snippet.getChunkIndex());
            item.put("excerpt", snippet.getContent());
            result.add(item);
        }
        return result;
    }

    private String joinMaterialIds(List<ReferenceSnippet> references) {
        Set<Long> ids = new LinkedHashSet<>();
        for (ReferenceSnippet snippet : references) {
            if (snippet.getMaterialId() != null) {
                ids.add(snippet.getMaterialId());
            }
        }
        String joined = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        return truncate(joined, SOURCE_MATERIAL_IDS_MAX_LENGTH);
    }

    private void cancelQuietly(Future<?> future) {
        if (future != null) {
            future.cancel(true);
        }
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            log.error("序列化失败", e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "序列化失败");
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String messageOf(Throwable throwable) {
        if (throwable == null) {
            return "生成失败，请重试";
        }
        if (throwable instanceof BusinessException) {
            String message = throwable.getMessage();
            return StringUtils.hasText(message) ? message : "生成失败，请重试";
        }
        String message = throwable.getMessage();
        if (!StringUtils.hasText(message)) {
            return "AI 服务调用失败，请稍后重试";
        }
        return message.length() > 200 ? message.substring(0, 200) : message;
    }
}
