package com.preppal.module.ai;

import com.preppal.common.BusinessException;
import com.preppal.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 教案生成 Prompt 模板。
 * 采用 {{name}} 手动替换而非 Spring AI 的 PromptTemplate，
 * 因为模板内包含 JSON 示例（大量花括号），会被 PromptTemplate 的占位符解析误伤。
 */
@Slf4j
@Component
public class LessonPlanPrompts {

    public static final String VERSION = "v1";

    private static final String SYSTEM_TEMPLATE = "classpath:prompts/lesson-plan-system.md";
    private static final String USER_TEMPLATE = "classpath:prompts/lesson-plan-user.md";

    private final String systemTemplate;
    private final String userTemplate;

    public LessonPlanPrompts(ResourceLoader resourceLoader) {
        this.systemTemplate = read(resourceLoader, SYSTEM_TEMPLATE);
        this.userTemplate = read(resourceLoader, USER_TEMPLATE);
    }

    public String system() {
        return systemTemplate;
    }

    public String user(Map<String, String> values) {
        String result = userTemplate;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue();
            result = result.replace("{{" + entry.getKey() + "}}", value);
        }
        return result;
    }

    private String read(ResourceLoader resourceLoader, String location) {
        Resource resource = resourceLoader.getResource(location);
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("加载 Prompt 模板失败: {}", location, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "加载 Prompt 模板失败：" + location);
        }
    }
}
