package com.preppal.module.lesson.controller;

import com.preppal.common.Result;
import com.preppal.module.lesson.dto.GeneratePlanRequest;
import com.preppal.module.lesson.dto.GeneratePlanResponse;
import com.preppal.module.lesson.dto.LessonPlanUpdateRequest;
import com.preppal.module.lesson.dto.LessonPlanVO;
import com.preppal.module.lesson.service.LessonPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/lesson-plans")
@RequiredArgsConstructor
public class LessonPlanController {

    private final LessonPlanService lessonPlanService;

    /**
     * 生成教案草稿（同步接口，超时阈值 60s）。
     * 失败或超时统一返回 code=0 + data.status=FAILED，由前端展示「生成超时，可重试」。
     */
    @PostMapping("/generate")
    public Result<GeneratePlanResponse> generate(@Valid @RequestBody GeneratePlanRequest request) {
        return Result.success(lessonPlanService.generate(request));
    }

    /** courseId 可省略：省略时返回当前用户的全部教案（首页统计需要） */
    @GetMapping
    public Result<List<LessonPlanVO>> list(@RequestParam(value = "courseId", required = false) Long courseId,
                                           @RequestParam(value = "status", required = false) String status) {
        return Result.success(lessonPlanService.list(courseId, status));
    }

    @GetMapping("/{id}")
    public Result<LessonPlanVO> detail(@PathVariable Long id) {
        return Result.success(lessonPlanService.detail(id));
    }

    /** 编辑保存内容，保存后状态回到 DRAFT */
    @PutMapping("/{id}")
    public Result<LessonPlanVO> update(@PathVariable Long id,
                                       @Valid @RequestBody LessonPlanUpdateRequest request) {
        return Result.success(lessonPlanService.update(id, request));
    }

    /** 标记定稿（DRAFT → FINALIZED） */
    @PostMapping("/{id}/finalize")
    public Result<LessonPlanVO> finalize(@PathVariable Long id) {
        return Result.success(lessonPlanService.finalize(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        lessonPlanService.delete(id);
        return Result.success();
    }
}
