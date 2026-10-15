package com.preppal.module.rag.controller;

import com.preppal.common.Result;
import com.preppal.module.rag.dto.MaterialVO;
import com.preppal.module.rag.service.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    /** 上传教学资料（multipart/form-data，仅 txt/md，≤5MB），同步解析并切片入库 */
    @PostMapping
    public Result<MaterialVO> upload(@RequestParam("file") MultipartFile file,
                                     @RequestParam("courseId") Long courseId) {
        return Result.success(materialService.upload(file, courseId));
    }

    @GetMapping
    public Result<List<MaterialVO>> list(@RequestParam("courseId") Long courseId) {
        return Result.success(materialService.list(courseId));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        materialService.delete(id);
        return Result.success();
    }
}
