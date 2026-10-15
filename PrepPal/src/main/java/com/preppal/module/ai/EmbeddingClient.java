package com.preppal.module.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/** 嵌入模型封装（BAAI/bge-m3，1024 维） */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingClient {

    private final EmbeddingModel embeddingModel;

    /** 批量嵌入；EmbeddingModel 内部按 BatchingStrategy 自动分批 */
    public List<float[]> embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }
        return embeddingModel.embed(texts);
    }

    public float[] embedOne(String text) {
        return embeddingModel.embed(text);
    }

    public int dimensions() {
        return embeddingModel.dimensions();
    }
}
