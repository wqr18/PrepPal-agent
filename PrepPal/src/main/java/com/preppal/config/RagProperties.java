package com.preppal.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** RAG 检索与切片参数（preppal.rag.*） */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "preppal.rag")
public class RagProperties {

    /** 单个分片的目标字数 */
    private int chunkSize = 500;

    /** 相邻分片的重叠字数，保证跨分片的语义连续 */
    private int chunkOverlap = 50;

    /** 解析后纯文本上限（字） */
    private int maxTextLength = 50000;

    /** 检索返回的最大分片数 */
    private int topK = 5;

    /** 余弦相似度阈值，低于该值视为「未检索到相关资料」 */
    private double similarityThreshold = 0.35D;
}
