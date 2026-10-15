package com.preppal.module.rag.service.impl;

import com.preppal.common.VectorCodec;
import com.preppal.config.RagProperties;
import com.preppal.module.ai.EmbeddingClient;
import com.preppal.module.rag.dto.RetrievedChunk;
import com.preppal.module.rag.entity.Material;
import com.preppal.module.rag.entity.MaterialChunk;
import com.preppal.module.rag.mapper.MaterialChunkMapper;
import com.preppal.module.rag.mapper.MaterialMapper;
import com.preppal.support.MybatisPlusTestSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class RetrievalServiceImplTest {

    private MaterialMapper materialMapper;
    private MaterialChunkMapper materialChunkMapper;
    private EmbeddingClient embeddingClient;
    private RagProperties properties;
    private RetrievalServiceImpl service;

    @BeforeAll
    static void initMybatisPlusMetadata() {
        MybatisPlusTestSupport.registerEntities(Material.class, MaterialChunk.class);
    }

    @BeforeEach
    void setUp() {
        materialMapper = Mockito.mock(MaterialMapper.class);
        materialChunkMapper = Mockito.mock(MaterialChunkMapper.class);
        embeddingClient = Mockito.mock(EmbeddingClient.class);
        properties = new RagProperties();
        properties.setTopK(5);
        properties.setSimilarityThreshold(0.35D);
        service = new RetrievalServiceImpl(materialMapper, materialChunkMapper, embeddingClient, properties);
    }

    @Test
    void returnsEmptyWhenQueryIsBlank() {
        assertThat(service.retrieve(1L, 1L, "   ")).isEmpty();
        Mockito.verifyNoInteractions(materialMapper);
    }

    @Test
    void returnsEmptyWhenCourseHasNoParsedMaterial() {
        when(materialMapper.selectList(any())).thenReturn(Collections.emptyList());

        assertThat(service.retrieve(1L, 1L, "牛顿第二定律")).isEmpty();
        Mockito.verifyNoInteractions(materialChunkMapper);
    }

    @Test
    void returnsEmptyWhenNoChunksExist() {
        when(materialMapper.selectList(any())).thenReturn(List.of(material(10L, "讲义.md")));
        when(materialChunkMapper.selectList(any())).thenReturn(Collections.emptyList());

        assertThat(service.retrieve(1L, 1L, "牛顿第二定律")).isEmpty();
    }

    @Test
    void ranksByCosineSimilarityAndFiltersByThreshold() {
        when(materialMapper.selectList(any())).thenReturn(List.of(material(10L, "讲义.md")));
        when(materialChunkMapper.selectList(any())).thenReturn(List.of(
                chunk(10L, 0, new float[]{1F, 0F}),
                chunk(10L, 1, new float[]{0.6F, 0.8F}),
                chunk(10L, 2, new float[]{0F, 1F})));
        when(embeddingClient.embedOne(anyString())).thenReturn(new float[]{1F, 0F});

        List<RetrievedChunk> hits = service.retrieve(1L, 1L, "牛顿第二定律");

        // [1,0] 相似度 1.0；[0.6,0.8] 相似度 0.6；[0,1] 相似度 0 被阈值过滤
        assertThat(hits).hasSize(2);
        assertThat(hits.get(0).getChunkIndex()).isZero();
        assertThat(hits.get(0).getScore()).isCloseTo(1.0D, offset(0.0001D));
        assertThat(hits.get(1).getChunkIndex()).isEqualTo(1);
        assertThat(hits.get(0).getMaterialName()).isEqualTo("讲义.md");
    }

    @Test
    void limitsResultToTopK() {
        properties.setTopK(1);
        when(materialMapper.selectList(any())).thenReturn(List.of(material(10L, "讲义.md")));
        when(materialChunkMapper.selectList(any())).thenReturn(List.of(
                chunk(10L, 0, new float[]{1F, 0F}),
                chunk(10L, 1, new float[]{1F, 0F})));
        when(embeddingClient.embedOne(anyString())).thenReturn(new float[]{1F, 0F});

        assertThat(service.retrieve(1L, 1L, "牛顿第二定律")).hasSize(1);
    }

    @Test
    void skipsChunksWithUnparsableVector() {
        when(materialMapper.selectList(any())).thenReturn(List.of(material(10L, "讲义.md")));
        MaterialChunk broken = chunk(10L, 0, new float[]{1F, 0F});
        broken.setVector("not-a-json");
        when(materialChunkMapper.selectList(any())).thenReturn(List.of(broken));
        when(embeddingClient.embedOne(anyString())).thenReturn(new float[]{1F, 0F});

        assertThat(service.retrieve(1L, 1L, "牛顿第二定律")).isEmpty();
    }

    private Material material(Long id, String filename) {
        Material material = new Material();
        material.setId(id);
        material.setFilename(filename);
        material.setParseStatus(Material.STATUS_SUCCESS);
        return material;
    }

    private MaterialChunk chunk(Long materialId, int index, float[] vector) {
        MaterialChunk chunk = new MaterialChunk();
        chunk.setId((long) index + 1);
        chunk.setMaterialId(materialId);
        chunk.setChunkIndex(index);
        chunk.setContent("分片内容 " + index);
        chunk.setVector(VectorCodec.toJson(vector));
        return chunk;
    }
}
