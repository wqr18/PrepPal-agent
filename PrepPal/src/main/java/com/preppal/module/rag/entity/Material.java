package com.preppal.module.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 教学资料（RAG 知识库） */
@Data
@TableName("material")
public class Material {

    /** 解析状态：等待解析（异步流程预留）/ 解析成功 / 解析失败 */
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long courseId;

    private String filename;

    private Long fileSize;

    private String mimeType;

    /** 解析后的全文文本 */
    private String content;

    /** 内容 SHA-256，用于同一课程下去重 */
    private String contentHash;

    private String parseStatus;

    private String errorMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
