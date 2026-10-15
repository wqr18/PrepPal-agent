package com.preppal.module.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 检索命中的资料分片 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetrievedChunk {

    private Long materialId;

    private String materialName;

    private Integer chunkIndex;

    private String content;

    /** 与提问的余弦相似度 */
    private double score;
}
