package com.preppal.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 教案生成专用线程池。
 * 生成接口同步等待，用它承载「60s 超时后可被取消」的 AI 调用，
 * 避免占用 ForkJoinPool.commonPool 或 Tomcat 请求线程池。
 */
@Configuration
@RequiredArgsConstructor
public class AsyncConfig {

    private final LessonProperties lessonProperties;

    @Bean(name = "lessonGenerateExecutor", destroyMethod = "shutdown")
    public ThreadPoolExecutor lessonGenerateExecutor() {
        AtomicInteger counter = new AtomicInteger(1);
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "lesson-generate-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
        return new ThreadPoolExecutor(
                lessonProperties.getExecutorCoreSize(),
                lessonProperties.getExecutorMaxSize(),
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(lessonProperties.getExecutorQueueCapacity()),
                threadFactory,
                new ThreadPoolExecutor.AbortPolicy());
    }
}
