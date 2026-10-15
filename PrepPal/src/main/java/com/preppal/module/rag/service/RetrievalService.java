package com.preppal.module.rag.service;

import com.preppal.module.rag.dto.RetrievedChunk;

import java.util.List;

public interface RetrievalService {

    /**
     * 按用户 + 课程检索与 query 最相关的资料分片。
     * 返回空列表表示「未检索到相关资料」，调用方据此设置 disclaimer。
     */
    List<RetrievedChunk> retrieve(Long userId, Long courseId, String query);
}
