package com.preppal.module.course.controller;

import com.preppal.common.GlobalExceptionHandler;
import com.preppal.module.course.dto.CourseVO;
import com.preppal.module.course.service.CourseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Controller 层：校验统一响应体结构与参数校验的返回格式 */
class CourseControllerTest {

    private CourseService courseService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        courseService = Mockito.mock(CourseService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CourseController(courseService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listWrapsPayloadInResultEnvelope() throws Exception {
        CourseVO vo = new CourseVO();
        vo.setId(1L);
        vo.setName("大学物理");
        vo.setMaterialCount(3);
        vo.setPlanCount(2);
        when(courseService.list()).thenReturn(List.of(vo));

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].name").value("大学物理"))
                .andExpect(jsonPath("$.data[0].materialCount").value(3))
                .andExpect(jsonPath("$.data[0].planCount").value(2));
    }

    @Test
    void createRejectsTooShortCourseNameWithUnifiedErrorBody() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"物\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("课程名称需 2–24 个字"));

        Mockito.verifyNoInteractions(courseService);
    }

    @Test
    void createReturnsCreatedCourse() throws Exception {
        CourseVO vo = new CourseVO();
        vo.setId(9L);
        vo.setName("大学物理");
        when(courseService.create(any())).thenReturn(vo);

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"大学物理\",\"description\":\"面向理工科一年级\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(9))
                .andExpect(jsonPath("$.data.name").value("大学物理"));
    }
}
