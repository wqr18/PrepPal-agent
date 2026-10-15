package com.preppal.module.ai.dto;

import lombok.Data;

/** 参考资料片段（在 Prompt 中以「引用编号」标识，便于模型回引） */
@Data
public class ReferenceSnippet {

    /** 引用编号，从 1 开始，在本次请求内唯一 */
    private int refNo;

    private Long materialId;

    private String materialName;

    private Integer chunkIndex;

    private String content;
}
