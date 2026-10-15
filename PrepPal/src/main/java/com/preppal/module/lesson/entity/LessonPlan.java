package com.preppal.module.lesson.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("lesson_plan")
public class LessonPlan {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_FINALIZED = "FINALIZED";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;

    private Long userId;

    private String title;

    private String topic;

    private String targetAudience;

    private Integer classDuration;

    private String teachingType;

    /** 教学目标（纯文本，按「知识：/能力：/素养：」分行的约定格式） */
    private String objectives;

    private String keyPoints;

    private String difficultPoints;

    /** 教学过程（JSON 字符串：[{stage,duration,content}]） */
    private String teachingProcess;

    private String blackboardDesign;

    private String homework;

    /** 参考来源（JSON 字符串：[{materialId,materialName,chunkIndex,excerpt}]） */
    private String sourceReferences;

    /** 资料不足提示 */
    private String disclaimer;

    private String status;

    /** 生成时的输入参数快照（JSON） */
    private String inputSnapshot;

    private String modelName;

    private String promptVersion;

    /** 参与生成的资料 ID 列表（逗号分隔） */
    private String sourceMaterialIds;

    private String idempotencyKey;

    /** 模型原始输出，便于回溯与排查 */
    private String rawContent;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
