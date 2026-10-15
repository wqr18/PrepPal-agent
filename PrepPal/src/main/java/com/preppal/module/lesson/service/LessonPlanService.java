package com.preppal.module.lesson.service;

import com.preppal.module.lesson.dto.GeneratePlanRequest;
import com.preppal.module.lesson.dto.GeneratePlanResponse;
import com.preppal.module.lesson.dto.LessonPlanUpdateRequest;
import com.preppal.module.lesson.dto.LessonPlanVO;

import java.util.List;

public interface LessonPlanService {

    /** 生成教案草稿；失败或超时返回 status=FAILED + errorMessage，不抛异常 */
    GeneratePlanResponse generate(GeneratePlanRequest request);

    List<LessonPlanVO> list(Long courseId, String status);

    LessonPlanVO detail(Long id);

    LessonPlanVO update(Long id, LessonPlanUpdateRequest request);

    LessonPlanVO finalize(Long id);

    void delete(Long id);
}
