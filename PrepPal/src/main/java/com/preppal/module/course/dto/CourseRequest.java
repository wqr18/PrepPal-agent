package com.preppal.module.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 新建 / 编辑课程请求 */
@Data
public class CourseRequest {

    @NotBlank(message = "请填写课程名称")
    @Size(min = 2, max = 24, message = "课程名称需 2–24 个字")
    private String name;

    @Size(max = 120, message = "课程简介不超过 120 个字")
    private String description;
}
