package com.preppal.module.rag.service;

import com.preppal.module.rag.dto.MaterialVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MaterialService {

    List<MaterialVO> list(Long courseId);

    /**
     * 上传并同步解析资料。
     * 解析失败不抛异常，而是返回 parseStatus=FAILED 的记录，便于前端展示失败原因与「重新上传」。
     * 仅扩展名不合法、文件超限、文本超长等前置校验会抛业务异常。
     */
    MaterialVO upload(MultipartFile file, Long courseId);

    void delete(Long id);
}
