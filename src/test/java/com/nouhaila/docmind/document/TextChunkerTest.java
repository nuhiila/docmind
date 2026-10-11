package com.nouhaila.docmind.document;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class TextChunkerTest {

    private static String words(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= count; i++) {
            sb.append("w").append(i).append(' ');
        }
        return sb.toString().trim();
    }

    @Test
    void returnsNoChunksForNullOrBlankText() {
        assertThat(TextChunker.chunk(null, 150, 30)).isEmpty();
        assertThat(TextChunker.chunk("   ", 150, 30)).isEmpty();
    }

    @Test
    void shortTextProducesASingleChunk() {
        assertThat(TextChunker.chunk(words(10), 150, 30)).hasSize(1);
    }

    @Test
    void textExactlyAsLongAsAChunkProducesASingleChunk() {
        assertThat(TextChunker.chunk(words(100), 100, 20)).hasSize(1);
    }

    @Test
    void longTextIsSplitWithOverlap() {
        List<String> chunks = TextChunker.chunk(words(300), 100, 20);

        assertThat(chunks).hasSize(4);

        String[] first = chunks.get(0).split(" ");
        String[] second = chunks.get(1).split(" ");
        assertThat(first).hasSize(100);
        assertThat(second[0]).isEqualTo("w81");

        // the last 20 words of a chunk are the first 20 words of the next one
        assertThat(Arrays.copyOfRange(first, 80, 100))
                .containsExactly(Arrays.copyOfRange(second, 0, 20));

        // nothing is lost at the end
        assertThat(chunks.get(3)).endsWith("w300");
    }
}