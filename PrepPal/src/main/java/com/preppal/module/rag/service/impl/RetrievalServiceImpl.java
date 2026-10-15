package com.preppal.module.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.preppal.common.VectorCodec;
import com.preppal.config.RagProperties;
import com.preppal.module.ai.EmbeddingClient;
import com.preppal.module.rag.dto.RetrievedChunk;
import com.preppal.module.rag.entity.Material;
import com.preppal.module.rag.entity.MaterialChunk;
import com.preppal.module.rag.mapper.MaterialChunkMapper;
import com.preppal.module.rag.mapper.MaterialMapper;
import com.preppal.module.rag.service.RetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 检索实现（MVP 方案）。
 * 按用户 + 课程取出全部分片，在应用侧计算余弦相似度取 Top-K。
 * 局限：分片量级到万级后需要改为向量索引（数据库向量索引或专用向量库）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetrievalServiceImpl implements RetrievalService {

    private final MaterialMapper materialMapper;
    private final MaterialChunkMapper materialChunkMapper;
    private final EmbeddingClient embeddingClient;
    private final RagProperties properties;

    @Override
    public List<RetrievedChunk> retrieve(Long userId, Long courseId, String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        List<Material> materials = materialMapper.selectList(new LambdaQueryWrapper<Material>()
                .eq(Material::getUserId, userId)
                .eq(Material::getCourseId, courseId)
                .eq(Material::getParseStatus, Material.STATUS_SUCCESS));
        if (materials.isEmpty()) {
            log.info("课程 {} 下没有解析成功的资料，跳过检索", courseId);
            return List.of();
        }

        Map<Long, String> filenameById = new HashMap<>();
        for (Material material : materials) {
            filenameById.put(material.getId(), material.getFilename());
        }

        List<MaterialChunk> chunks = materialChunkMapper.selectList(new LambdaQueryWrapper<MaterialChunk>()
                .eq(MaterialChunk::getUserId, userId)
                .in(MaterialChunk::getMaterialId, new ArrayList<>(filenameById.keySet())));
        if (chunks.isEmpty()) {
            return List.of();
        }

        float[] queryVector = embeddingClient.embedOne(query);

        List<RetrievedChunk> hits = new ArrayList<>();
        for (MaterialChunk chunk : chunks) {
            double score = cosine(queryVector, VectorCodec.fromJson(chunk.getVector()));
            if (score >= properties.getSimilarityThreshold()) {
                hits.add(new RetrievedChunk(chunk.getMaterialId(), filenameById.get(chunk.getMaterialId()),
                        chunk.getChunkIndex(), chunk.getContent(), score));
            }
        }
        hits.sort(Comparator.comparingDouble(RetrievedChunk::getScore).reversed());
        List<RetrievedChunk> top = hits.size() > properties.getTopK()
                ? new ArrayList<>(hits.subList(0, properties.getTopK()))
                : hits;
        log.info("检索完成 courseId={}, 候选分片={}, 命中={}", courseId, chunks.size(), top.size());
        return top;
    }

    /** 余弦相似度；维度不一致或零向量返回 0 */
    private double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || a.length != b.length) {
            return 0D;
        }
        double dot = 0D;
        double normA = 0D;
        double normB = 0D;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0D || normB == 0D) {
            return 0D;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
