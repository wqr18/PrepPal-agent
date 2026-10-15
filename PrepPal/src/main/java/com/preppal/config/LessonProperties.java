package com.preppal.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 教案生成参数（preppal.lesson.*） */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "preppal.lesson")
public class LessonProperties {

    /** 生成超时阈值（秒），超时返回 status=FAILED 由前端提示可重试 */
    private int generateTimeoutSeconds = 60;

    /** 生成线程池核心线程数 */
    private int executorCoreSize = 4;

    /** 生成线程池最大线程数 */
    private int executorMaxSize = 8;

    /** 生成任务排队上限，超出直接拒绝，避免请求堆积 */
    private int executorQueueCapacity = 50;
}
