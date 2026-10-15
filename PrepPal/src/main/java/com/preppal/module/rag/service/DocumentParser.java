package com.preppal.module.rag.service;

import com.preppal.common.BusinessException;
import com.preppal.common.ResultCode;
import com.preppal.config.RagProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 教学资料文本解析：编码识别 + 段落切片。
 * 仅支持 .txt / .md 纯文本，不涉及 PDF / OCR（属 P1）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentParser {

    private static final Charset GBK = Charset.forName("GBK");
    private static final String BOM = "\uFEFF";

    private final RagProperties properties;

    /**
     * 字节流 → 文本。
     * 先用 UTF-8 严格解码；出现非法字节序列时回退 GBK（覆盖常见的中文 GBK 讲义），
     * 两者都失败才判定为「编码无法识别」。
     */
    public String decode(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "文件内容为空");
        }
        String text;
        try {
            text = decodeStrict(bytes, StandardCharsets.UTF_8);
        } catch (CharacterCodingException utf8Error) {
            log.info("UTF-8 严格解码失败，回退 GBK 重试");
            try {
                text = decodeStrict(bytes, GBK);
            } catch (CharacterCodingException gbkError) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR,
                        "文件编码无法识别，请另存为 UTF-8 编码的 .txt 或 .md 后重新上传");
            }
        }
        if (text.startsWith(BOM)) {
            text = text.substring(1);
        }
        return text;
    }

    /** 校验解析后的纯文本长度上限 */
    public void validateLength(String text) {
        int length = text.length();
        if (length > properties.getMaxTextLength()) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR,
                    "解析后文本共 " + length + " 字，超过 " + properties.getMaxTextLength() + " 字上限，请拆分文件后重新上传");
        }
    }

    /**
     * 切片策略：优先按空行分段，再把相邻段落合并到 chunkSize 以内；
     * 单个超长段落按固定长度切分，并保留 chunkOverlap 的重叠，避免跨分片语义断裂。
     */
    public List<String> chunk(String text) {
        int chunkSize = Math.max(80, properties.getChunkSize());
        int overlap = Math.min(Math.max(0, properties.getChunkOverlap()), chunkSize / 2);

        String normalized = text.replace("\r\n", "\n").replace('\r', '\n').trim();
        if (normalized.isEmpty()) {
            return List.of();
        }

        List<String> paragraphs = new ArrayList<>();
        for (String block : normalized.split("\n\\s*\n")) {
            String trimmed = block.trim();
            if (!trimmed.isEmpty()) {
                paragraphs.addAll(splitLongParagraph(trimmed, chunkSize, overlap));
            }
        }

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String paragraph : paragraphs) {
            if (current.length() > 0 && current.length() + paragraph.length() + 1 > chunkSize) {
                String finished = current.toString().trim();
                chunks.add(finished);
                current.setLength(0);
                if (overlap > 0 && finished.length() > overlap) {
                    current.append(finished.substring(finished.length() - overlap)).append('\n');
                }
            }
            if (current.length() > 0) {
                current.append('\n');
            }
            current.append(paragraph);
        }
        if (current.length() > 0) {
            String tail = current.toString().trim();
            if (!tail.isEmpty()) {
                chunks.add(tail);
            }
        }
        return chunks;
    }

    private List<String> splitLongParagraph(String paragraph, int chunkSize, int overlap) {
        if (paragraph.length() <= chunkSize) {
            return List.of(paragraph);
        }
        List<String> parts = new ArrayList<>();
        int step = Math.max(1, chunkSize - overlap);
        for (int start = 0; start < paragraph.length(); start += step) {
            int end = Math.min(paragraph.length(), start + chunkSize);
            parts.add(paragraph.substring(start, end));
            if (end == paragraph.length()) {
                break;
            }
        }
        return parts;
    }

    private String decodeStrict(byte[] bytes, Charset charset) throws CharacterCodingException {
        return charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
    }

    /** 供单测与日志使用的段落统计 */
    public long paragraphCount(String text) {
        return Arrays.stream(text.replace("\r\n", "\n").split("\n\\s*\n"))
                .filter(block -> !block.trim().isEmpty())
                .count();
    }
}
