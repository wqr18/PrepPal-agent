package com.preppal.module.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/** 对话模型封装（硅基流动 DeepSeek-V3） */
@Slf4j
@Component
public class LlmClient {

    private final ChatModel chatModel;
    private final String modelName;

    public LlmClient(ChatModel chatModel,
                     @Value("${spring.ai.openai.chat.options.model:unknown}") String modelName) {
        this.chatModel = chatModel;
        this.modelName = modelName;
    }

    public String modelName() {
        return modelName;
    }

    /**
     * 单轮对话。
     * 系统指令与用户内容作为独立消息传入，参考资料被包在用户消息的不可信区段内，
     * 从结构上隔离「系统指令」与「用户上传内容」（防提示词注入）。
     */
    public String complete(String systemPrompt, String userPrompt) {
        Prompt prompt = new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt)));
        ChatResponse response = chatModel.call(prompt);
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            throw new IllegalStateException("模型返回为空");
        }
        String text = response.getResult().getOutput().getText();
        log.debug("LLM 调用完成 model={}, 输出长度={}", modelName, text == null ? 0 : text.length());
        return text;
    }
}
