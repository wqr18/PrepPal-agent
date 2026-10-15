package com.preppal.common;

import com.fasterxml.jackson.databind.ObjectMapper;

/** 向量 float[] 与 material_chunk.vector（JSON 列）之间的编解码 */
public final class VectorCodec {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private VectorCodec() {
    }

    public static String toJson(float[] vector) {
        if (vector == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(vector);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "向量序列化失败");
        }
    }

    /** 解析失败或格式异常返回 null，由调用方跳过该分片 */
    public static float[] fromJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, float[].class);
        } catch (Exception e) {
            return null;
        }
    }
}
