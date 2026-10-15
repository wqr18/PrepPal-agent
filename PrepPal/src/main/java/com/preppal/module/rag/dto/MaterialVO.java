package com.preppal.module.rag.dto;

import com.preppal.common.TimeUtils;
import com.preppal.module.rag.entity.Material;
import lombok.Data;

@Data
public class MaterialVO {

    private Long id;
    private Long courseId;
    private String filename;
    private Long fileSize;
    private String mimeType;
    private String parseStatus;
    private String errorMessage;
    private Long createdAt;

    /** 内容与已有资料重复（用于前端「该资料已存在，直接复用」提示） */
    private Boolean duplicate;

    public static MaterialVO from(Material material) {
        return from(material, false);
    }

    public static MaterialVO from(Material material, boolean duplicate) {
        MaterialVO vo = new MaterialVO();
        vo.setId(material.getId());
        vo.setCourseId(material.getCourseId());
        vo.setFilename(material.getFilename());
        vo.setFileSize(material.getFileSize());
        vo.setMimeType(material.getMimeType());
        vo.setParseStatus(material.getParseStatus());
        vo.setErrorMessage(material.getErrorMessage());
        vo.setCreatedAt(TimeUtils.toMillis(material.getCreatedAt()));
        vo.setDuplicate(duplicate);
        return vo;
    }
}
