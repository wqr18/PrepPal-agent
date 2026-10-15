package com.preppal.module.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 资料分片（向量持久化，应用侧余弦检索） */
@Data
@TableName("material_chunk")
public class MaterialChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long materialId;

    /** 归属用户（冗余，便于按用户检索） */
    private Long userId;

    /** 分片序号，从 0 开始 */
    private Integer chunkIndex;

    private String content;

    /** bge-m3 向量（1024 维）的 JSON 数组文本 */
    private String vector;

    private LocalDateTime createdAt;
}
