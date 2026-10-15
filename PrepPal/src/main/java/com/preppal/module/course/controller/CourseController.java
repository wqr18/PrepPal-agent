package com.preppal.module.course.controller;

import com.preppal.common.Result;
import com.preppal.module.course.dto.CourseRequest;
import com.preppal.module.course.dto.CourseVO;
import com.preppal.module.course.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    public Result<List<CourseVO>> list() {
        return Result.success(courseService.list());
    }

    @PostMapping
    public Result<CourseVO> create(@Valid @RequestBody CourseRequest request) {
        return Result.success(courseService.create(request));
    }

    @GetMapping("/{id}")
    public Result<CourseVO> detail(@PathVariable Long id) {
        return Result.success(courseService.detail(id));
    }

    /** 编辑课程信息（AI-CONTEXT 的 MVP 接口清单未列出，前端已在使用） */
    @PutMapping("/{id}")
    public Result<CourseVO> update(@PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        return Result.success(courseService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        courseService.delete(id);
        return Result.success();
    }
}
