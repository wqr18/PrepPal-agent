package com.preppal.module.rag.service;

import com.preppal.common.BusinessException;
import com.preppal.config.RagProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentParserTest {

    private static final Charset GBK = Charset.forName("GBK");

    private RagProperties properties;
    private DocumentParser parser;

    @BeforeEach
    void setUp() {
        properties = new RagProperties();
        properties.setChunkSize(100);
        properties.setChunkOverlap(10);
        properties.setMaxTextLength(1000);
        parser = new DocumentParser(properties);
    }

    @Test
    void decodesUtf8Text() {
        String text = "牛顿第二定律：物体加速度的大小跟作用力成正比。";

        assertThat(parser.decode(text.getBytes(StandardCharsets.UTF_8))).isEqualTo(text);
    }

    @Test
    void fallsBackToGbkWhenUtf8SequenceIsInvalid() {
        String text = "牛顿第二定律：物体加速度的大小跟作用力成正比，跟物体的质量成反比。";

        // GBK 字节序列在 UTF-8 下必然出现非法字节，触发回退
        assertThat(parser.decode(text.getBytes(GBK))).isEqualTo(text);
    }

    @Test
    void stripsUtf8Bom() {
        byte[] body = "惯性定律".getBytes(StandardCharsets.UTF_8);
        byte[] withBom = new byte[body.length + 3];
        withBom[0] = (byte) 0xEF;
        withBom[1] = (byte) 0xBB;
        withBom[2] = (byte) 0xBF;
        System.arraycopy(body, 0, withBom, 3, body.length);

        assertThat(parser.decode(withBom)).isEqualTo("惯性定律");
    }

    @Test
    void rejectsBytesThatAreInvalidInBothCharsets() {
        byte[] broken = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF};

        assertThatThrownBy(() -> parser.decode(broken))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("编码无法识别");
    }

    @Test
    void rejectsEmptyFile() {
        assertThatThrownBy(() -> parser.decode(new byte[0]))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文件内容为空");
    }

    @Test
    void rejectsTextLongerThanLimit() {
        String tooLong = "物".repeat(1001);

        assertThatThrownBy(() -> parser.validateLength(tooLong))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("超过 1000 字上限");
    }

    @Test
    void acceptsTextAtLimit() {
        parser.validateLength("物".repeat(1000));
    }

    @Test
    void chunkReturnsEmptyForBlankText() {
        assertThat(parser.chunk("   \n\n  ")).isEmpty();
    }

    @Test
    void chunkPacksNeighbouringParagraphsIntoOneChunk() {
        String paragraphA = "A".repeat(40);
        String paragraphB = "B".repeat(40);
        String paragraphC = "C".repeat(40);

        List<String> chunks = parser.chunk(paragraphA + "\n\n" + paragraphB + "\n\n" + paragraphC);

        // 40 + 1 + 40 = 81 ≤ 100，前两段合成一个分片，第三段另起
        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0)).contains(paragraphA).contains(paragraphB);
        assertThat(chunks.get(1)).contains(paragraphC);
    }

    @Test
    void chunkSplitsLongParagraphAndKeepsOverlap() {
        String longParagraph = "0123456789".repeat(15); // 150 字

        List<String> chunks = parser.chunk(longParagraph);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks.get(0)).hasSize(100);
        // 步长 = chunkSize - overlap = 90，第二个分片从第 90 字开始
        assertThat(chunks.get(1)).startsWith(longParagraph.substring(90, 100));
    }

    @Test
    void chunkDoesNotExceedChunkSizeRepeatedly() {
        String text = ("段落".repeat(20) + "\n\n").repeat(8);

        List<String> chunks = parser.chunk(text);

        assertThat(chunks).isNotEmpty();
        // 允许因重叠略微超出，但不应出现整段被重复打包的极端情况
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.length()).isLessThanOrEqualTo(160));
    }

    @Test
    void countsParagraphs() {
        assertThat(parser.paragraphCount("第一段\n\n第二段\n\n\n第三段")).isEqualTo(3);
    }
}
