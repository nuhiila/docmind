package com.nouhaila.docmind.document;

import java.time.Instant;

public record DocumentResponse(Long id, String name, long size, DocumentStatus status, Instant uploadedAt) {

    public static DocumentResponse from(Document d) {
        return new DocumentResponse(d.getId(), d.getOriginalName(), d.getSize(), d.getStatus(), d.getUploadedAt());
    }
}