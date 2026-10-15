package com.preppal.module.lesson.service.impl;

import com.preppal.common.BusinessException;
import com.preppal.common.UserContext;
import com.preppal.config.LessonProperties;
import com.preppal.module.ai.LessonPlanGenerator;
import com.preppal.module.ai.LlmClient;
import com.preppal.module.ai.dto.GeneratedLessonPlan;
import com.preppal.module.ai.dto.LessonPlanDraft;
import com.preppal.module.ai.dto.ReferenceSnippet;
import com.preppal.module.course.entity.Course;
import com.preppal.module.course.mapper.CourseMapper;
import com.preppal.module.lesson.dto.GeneratePlanRequest;
import com.preppal.module.lesson.dto.GeneratePlanResponse;
import com.preppal.module.lesson.entity.LessonPlan;
import com.preppal.module.lesson.mapper.LessonPlanMapper;
import com.preppal.module.rag.dto.RetrievedChunk;
import com.preppal.module.rag.service.RetrievalService;
import com.preppal.support.MybatisPlusTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class LessonPlanServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long COURSE_ID = 7L;

    private LessonPlanMapper lessonPlanMapper;
    private CourseMapper courseMapper;
    private RetrievalService retrievalService;
    private LessonPlanGenerator lessonPlanGenerator;
    private LessonProperties lessonProperties;
    private ThreadPoolExecutor executor;
    private LessonPlanServiceImpl service;

    private final AtomicReference<LessonPlan> inserted = new AtomicReference<>();

    @BeforeAll
    static void initMybatisPlusMetadata() {
        MybatisPlusTestSupport.registerEntities(LessonPlan.class, Course.class);
    }

    @BeforeEach
    void setUp() {
        lessonPlanMapper = Mockito.mock(LessonPlanMapper.class);
        courseMapper = Mockito.mock(CourseMapper.class);
        retrievalService = Mockito.mock(RetrievalService.class);
        lessonPlanGenerator = Mockito.mock(LessonPlanGenerator.class);
        LlmClient llmClient = Mockito.mock(LlmClient.class);

        lessonProperties = new LessonProperties();
        lessonProperties.setGenerateTimeoutSeconds(2);
        executor = new ThreadPoolExecutor(1, 2, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(8));

        service = new LessonPlanServiceImpl(lessonPlanMapper, courseMapper, retrievalService,
                lessonPlanGenerator, llmClient, lessonProperties, executor);

        when(llmClient.modelName()).thenReturn("deepseek-ai/DeepSeek-V3");
        when(courseMapper.selectById(COURSE_ID)).thenReturn(course(COURSE_ID, USER_ID));
        when(lessonPlanMapper.insert(any(LessonPlan.class))).thenAnswer(invocation -> {
            LessonPlan plan = invocation.getArgument(0);
            plan.setId(100L);
            inserted.set(plan);
            return 1;
        });
        when(lessonPlanMapper.selectById(anyLong())).thenAnswer(invocation -> inserted.get());

        UserContext.set(USER_ID);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
        executor.shutdownNow();
    }

    @Test
    void reusesExistingPlanWhenIdempotencyKeyHits() {
        LessonPlan existing = new LessonPlan();
        existing.setId(55L);
        existing.setUserId(USER_ID);
        existing.setCourseId(COURSE_ID);
        when(lessonPlanMapper.selectOne(any())).thenReturn(existing);

        GeneratePlanResponse response = service.generate(request("key-1"));

        assertThat(response.getStatus()).isEqualTo("SUCCEEDED");
        assertThat(response.getLessonPlanId()).isEqualTo(55L);
        assertThat(inserted.get()).isNull();
        Mockito.verifyNoInteractions(lessonPlanGenerator);
    }

    @Test
    void generatesDraftAndAttachesSourceReferences() {
        when(lessonPlanMapper.selectOne(any())).thenReturn(null);
        when(retrievalService.retrieve(eq(USER_ID), eq(COURSE_ID), anyString())).thenReturn(List.of(
                new RetrievedChunk(10L, "大学物理第一章.md", 3, "加速度与合外力成正比", 0.91D)));
        when(lessonPlanGenerator.generate(any())).thenReturn(generated(List.of(snippet(1, 10L, "大学物理第一章.md", 3))));

        GeneratePlanResponse response = service.generate(request("key-2"));

        assertThat(response.getStatus()).isEqualTo("SUCCEEDED");
        assertThat(response.getLessonPlanId()).isEqualTo(100L);

        LessonPlan saved = inserted.get();
        assertThat(saved).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(LessonPlan.STATUS_DRAFT);
        assertThat(saved.getDisclaimer()).isEqualTo(LessonPlanServiceImpl.DISCLAIMER_WITH_REFERENCES);
        assertThat(saved.getSourceReferences()).contains("大学物理第一章.md").contains("\"chunkIndex\":3");
        assertThat(saved.getSourceMaterialIds()).isEqualTo("10");
        // duration "5" 应被规范化为 "5min"，与 AI-CONTEXT 约定的 wire 格式一致
        assertThat(saved.getTeachingProcess()).contains("\"duration\":\"5min\"");
        assertThat(saved.getModelName()).isEqualTo("deepseek-ai/DeepSeek-V3");
    }

    @Test
    void marksDisclaimerWhenNoReferenceRetrieved() {
        when(lessonPlanMapper.selectOne(any())).thenReturn(null);
        when(retrievalService.retrieve(eq(USER_ID), eq(COURSE_ID), anyString())).thenReturn(List.of());
        when(lessonPlanGenerator.generate(any())).thenReturn(generated(List.of()));

        GeneratePlanResponse response = service.generate(request("key-3"));

        assertThat(response.getStatus()).isEqualTo("SUCCEEDED");
        LessonPlan saved = inserted.get();
        assertThat(saved.getDisclaimer()).isEqualTo(LessonPlanServiceImpl.DISCLAIMER_NO_REFERENCE);
        assertThat(saved.getSourceReferences()).isEqualTo("[]");
        assertThat(saved.getSourceMaterialIds()).isEmpty();
    }

    @Test
    void returnsFailedWhenGenerationTimesOut() {
        lessonProperties.setGenerateTimeoutSeconds(1);
        when(lessonPlanMapper.selectOne(any())).thenReturn(null);
        when(retrievalService.retrieve(anyLong(), anyLong(), anyString())).thenReturn(List.of());
        when(lessonPlanGenerator.generate(any())).thenAnswer(invocation -> {
            Thread.sleep(5000L);
            return generated(List.of());
        });

        GeneratePlanResponse response = service.generate(request("key-4"));

        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getLessonPlanId()).isNull();
        assertThat(response.getErrorMessage()).contains("超时");
        assertThat(inserted.get()).isNull();
    }

    @Test
    void rejectsCourseOwnedByAnotherUser() {
        when(courseMapper.selectById(COURSE_ID)).thenReturn(course(COURSE_ID, 999L));

        assertThatThrownBy(() -> service.generate(request("key-5")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("课程不存在");
    }

    @Test
    void returnsFailedWithModelMessageWhenGeneratorThrows() {
        when(lessonPlanMapper.selectOne(any())).thenReturn(null);
        when(retrievalService.retrieve(anyLong(), anyLong(), anyString())).thenReturn(List.of());
        when(lessonPlanGenerator.generate(any())).thenThrow(new BusinessException("模型输出格式异常，请重试"));

        GeneratePlanResponse response = service.generate(request("key-6"));

        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getErrorMessage()).isEqualTo("模型输出格式异常，请重试");
    }

    private GeneratePlanRequest request(String idempotencyKey) {
        GeneratePlanRequest request = new GeneratePlanRequest();
        request.setCourseId(COURSE_ID);
        request.setTopic("牛顿第二定律");
        request.setTargetAudience("大学一年级理工科");
        request.setClassDuration(45);
        request.setTeachingType("理论");
        request.setIdempotencyKey(idempotencyKey);
        return request;
    }

    private Course course(Long id, Long userId) {
        Course course = new Course();
        course.setId(id);
        course.setUserId(userId);
        course.setName("大学物理");
        return course;
    }

    private ReferenceSnippet snippet(int refNo, Long materialId, String name, int chunkIndex) {
        ReferenceSnippet snippet = new ReferenceSnippet();
        snippet.setRefNo(refNo);
        snippet.setMaterialId(materialId);
        snippet.setMaterialName(name);
        snippet.setChunkIndex(chunkIndex);
        snippet.setContent("加速度与合外力成正比");
        return snippet;
    }

    private GeneratedLessonPlan generated(List<ReferenceSnippet> references) {
        LessonPlanDraft draft = new LessonPlanDraft();
        draft.setTitle("牛顿第二定律教案");
        draft.setObjectives("知识：理解 F = ma\n能力：能列方程求解\n素养：以证据支持结论");
        draft.setKeyPoints("牛顿第二定律的内容与表达式");
        draft.setDifficultPoints("合力的正交分解");
        LessonPlanDraft.ProcessStage stage = new LessonPlanDraft.ProcessStage();
        stage.setStage("导入");
        stage.setDuration("5");
        stage.setContent("情境导入");
        draft.setTeachingProcess(List.of(stage));
        draft.setBlackboardDesign("左侧主板书 F = ma");
        draft.setHomework("教材第 2 章习题 2、5、7");
        return new GeneratedLessonPlan(draft, draft.getTeachingProcess(), references, "{\"title\":\"牛顿第二定律教案\"}");
    }
}
