package com.preppal.module.course.dto;

import com.preppal.common.TimeUtils;
import com.preppal.module.course.entity.Course;
import lombok.Data;

@Data
public class CourseVO {

    private Long id;
    private String name;
    private String description;
    private Integer materialCount;
    private Integer planCount;
    private Long updatedAt;

    public static CourseVO from(Course course, int materialCount, int planCount) {
        CourseVO vo = new CourseVO();
        vo.setId(course.getId());
        vo.setName(course.getName());
        vo.setDescription(course.getDescription());
        vo.setMaterialCount(materialCount);
        vo.setPlanCount(planCount);
        vo.setUpdatedAt(TimeUtils.toMillis(course.getUpdatedAt()));
        return vo;
    }
}
