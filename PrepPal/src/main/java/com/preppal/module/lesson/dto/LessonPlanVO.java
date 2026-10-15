package com.preppal.module.lesson.dto;

import com.preppal.common.TimeUtils;
import com.preppal.module.lesson.entity.LessonPlan;
import lombok.Data;

/**
 * 教案展示结构。
 * 注意：teachingProcess 与 sourceReferences 按 AI-CONTEXT 的列语义返回 JSON 字符串，
 * 与前端的写入载荷保持一致（前端 utils/mapper.js 负责解析）。
 */
@Data
public class LessonPlanVO {

    private Long id;
    private Long courseId;
    private String courseName;
    private String title;
    private String topic;
    private String targetAudience;
    private Integer classDuration;
    private String teachingType;
    private String objectives;
    private String keyPoints;
    private String difficultPoints;
    private String teachingProcess;
    private String blackboardDesign;
    private String homework;
    private String sourceReferences;
    private String disclaimer;
    private String status;
    private String modelName;
    private String promptVersion;
    private Long createdAt;
    private Long updatedAt;

    public static LessonPlanVO from(LessonPlan plan, String courseName) {
        LessonPlanVO vo = new LessonPlanVO();
        vo.setId(plan.getId());
        vo.setCourseId(plan.getCourseId());
        vo.setCourseName(courseName);
        vo.setTitle(plan.getTitle());
        vo.setTopic(plan.getTopic());
        vo.setTargetAudience(plan.getTargetAudience());
        vo.setClassDuration(plan.getClassDuration());
        vo.setTeachingType(plan.getTeachingType());
        vo.setObjectives(plan.getObjectives());
        vo.setKeyPoints(plan.getKeyPoints());
        vo.setDifficultPoints(plan.getDifficultPoints());
        vo.setTeachingProcess(plan.getTeachingProcess());
        vo.setBlackboardDesign(plan.getBlackboardDesign());
        vo.setHomework(plan.getHomework());
        vo.setSourceReferences(plan.getSourceReferences());
        vo.setDisclaimer(plan.getDisclaimer());
        vo.setStatus(plan.getStatus());
        vo.setModelName(plan.getModelName());
        vo.setPromptVersion(plan.getPromptVersion());
        vo.setCreatedAt(TimeUtils.toMillis(plan.getCreatedAt()));
        vo.setUpdatedAt(TimeUtils.toMillis(plan.getUpdatedAt()));
        return vo;
    }
}
