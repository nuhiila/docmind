package com.nouhaila.docmind.document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class TextChunker {

    private TextChunker() {}

    public static List<String> chunk(String text, int chunkWords, int overlapWords) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }
        String[] words = text.trim().split("\\s+");
        int step = chunkWords - overlapWords;
        for (int start = 0; start < words.length; start += step) {
            int end = Math.min(start + chunkWords, words.length);
            chunks.add(String.join(" ", Arrays.copyOfRange(words, start, end)));
            if (end == words.length) {
                break;
            }
        }
        return chunks;
    }
}