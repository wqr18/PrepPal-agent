package com.preppal.module.lesson.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 生成教案响应：status = SUCCEEDED / FAILED */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneratePlanResponse {

    private static final String SUCCEEDED = "SUCCEEDED";
    private static final String FAILED = "FAILED";

    private Long lessonPlanId;

    private String status;

    private LessonPlanVO lessonPlan;

    private String errorMessage;

    public static GeneratePlanResponse success(LessonPlanVO plan) {
        return new GeneratePlanResponse(plan.getId(), SUCCEEDED, plan, null);
    }

    public static GeneratePlanResponse failed(String errorMessage) {
        return new GeneratePlanResponse(null, FAILED, null, errorMessage);
    }
}
