package com.preppal.module.course.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.preppal.common.BusinessException;
import com.preppal.common.ResultCode;
import com.preppal.common.UserContext;
import com.preppal.module.course.dto.CourseRequest;
import com.preppal.module.course.dto.CourseVO;
import com.preppal.module.course.entity.Course;
import com.preppal.module.course.mapper.CourseMapper;
import com.preppal.module.course.service.CourseService;
import com.preppal.module.lesson.entity.LessonPlan;
import com.preppal.module.lesson.mapper.LessonPlanMapper;
import com.preppal.module.rag.entity.Material;
import com.preppal.module.rag.entity.MaterialChunk;
import com.preppal.module.rag.mapper.MaterialChunkMapper;
import com.preppal.module.rag.mapper.MaterialMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 课程服务。
 * 说明：为了统计「资料数 / 教案数」与级联删除，本模块直接读取 rag / lesson 模块的 Mapper
 * （模块间只通过 Mapper 做只读访问与级联清理，不存在 Service 层循环依赖）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseMapper courseMapper;
    private final MaterialMapper materialMapper;
    private final MaterialChunkMapper materialChunkMapper;
    private final LessonPlanMapper lessonPlanMapper;

    @Override
    public List<CourseVO> list() {
        Long userId = UserContext.require();
        List<Course> courses = courseMapper.selectList(
                new LambdaQueryWrapper<Course>()
                        .eq(Course::getUserId, userId)
                        .orderByDesc(Course::getUpdatedAt));
        if (courses.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> courseIds = courses.stream().map(Course::getId).collect(Collectors.toList());
        Map<Long, Integer> materialCounts = countMaterials(userId, courseIds);
        Map<Long, Integer> planCounts = countPlans(userId, courseIds);
        return courses.stream()
                .map(course -> CourseVO.from(course,
                        materialCounts.getOrDefault(course.getId(), 0),
                        planCounts.getOrDefault(course.getId(), 0)))
                .collect(Collectors.toList());
    }

    @Override
    public CourseVO create(CourseRequest request) {
        Long userId = UserContext.require();
        Course course = new Course();
        course.setUserId(userId);
        course.setName(request.getName().trim());
        course.setDescription(trimToNull(request.getDescription()));
        courseMapper.insert(course);
        return CourseVO.from(courseMapper.selectById(course.getId()), 0, 0);
    }

    @Override
    public CourseVO detail(Long id) {
        Course course = requireOwnedCourse(id);
        Long userId = UserContext.require();
        return CourseVO.from(course,
                countMaterials(userId, List.of(id)).getOrDefault(id, 0),
                countPlans(userId, List.of(id)).getOrDefault(id, 0));
    }

    @Override
    public CourseVO update(Long id, CourseRequest request) {
        Course course = requireOwnedCourse(id);
        course.setName(request.getName().trim());
        course.setDescription(trimToNull(request.getDescription()));
        courseMapper.updateById(course);
        return detail(id);
    }

    /** 删除课程时一并清理其下的资料、资料分片与教案（数据库层无外键，需应用层保证一致） */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Course course = requireOwnedCourse(id);
        List<Material> materials = materialMapper.selectList(
                new LambdaQueryWrapper<Material>().eq(Material::getCourseId, id));
        List<Long> materialIds = materials.stream().map(Material::getId).collect(Collectors.toList());
        if (!materialIds.isEmpty()) {
            materialChunkMapper.delete(new LambdaQueryWrapper<MaterialChunk>()
                    .in(MaterialChunk::getMaterialId, materialIds));
        }
        materialMapper.delete(new LambdaQueryWrapper<Material>().eq(Material::getCourseId, id));
        lessonPlanMapper.delete(new LambdaQueryWrapper<LessonPlan>().eq(LessonPlan::getCourseId, id));
        courseMapper.deleteById(course.getId());
    }

    private Course requireOwnedCourse(Long id) {
        Long userId = UserContext.require();
        Course course = courseMapper.selectById(id);
        if (course == null || !userId.equals(course.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "课程不存在");
        }
        return course;
    }

    private Map<Long, Integer> countMaterials(Long userId, List<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        QueryWrapper<Material> wrapper = new QueryWrapper<>();
        wrapper.select("course_id AS courseId", "COUNT(*) AS cnt")
                .eq("user_id", userId)
                .in("course_id", courseIds)
                .groupBy("course_id");
        return toCountMap(materialMapper.selectMaps(wrapper));
    }

    private Map<Long, Integer> countPlans(Long userId, List<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        QueryWrapper<LessonPlan> wrapper = new QueryWrapper<>();
        wrapper.select("course_id AS courseId", "COUNT(*) AS cnt")
                .eq("user_id", userId)
                .in("course_id", courseIds)
                .groupBy("course_id");
        return toCountMap(lessonPlanMapper.selectMaps(wrapper));
    }

    private Map<Long, Integer> toCountMap(List<Map<String, Object>> rows) {
        Map<Long, Integer> result = new HashMap<>();
        if (rows == null) {
            return result;
        }
        for (Map<String, Object> row : new ArrayList<>(rows)) {
            Object courseId = row.get("courseId");
            Object count = row.get("cnt");
            if (courseId instanceof Number && count instanceof Number) {
                result.put(((Number) courseId).longValue(), ((Number) count).intValue());
            }
        }
        return result;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
