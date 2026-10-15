package com.preppal.module.course.service;

import com.preppal.module.course.dto.CourseRequest;
import com.preppal.module.course.dto.CourseVO;

import java.util.List;

public interface CourseService {

    List<CourseVO> list();

    CourseVO create(CourseRequest request);

    CourseVO detail(Long id);

    CourseVO update(Long id, CourseRequest request);

    void delete(Long id);
}
