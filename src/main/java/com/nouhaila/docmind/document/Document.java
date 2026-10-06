package com.nouhaila.docmind.document;

import com.nouhaila.docmind.user.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String originalName;

    @Column(nullable = false, unique = true)
    private String storedName;

    @Column(nullable = false)
    private long size;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;

    @Column(nullable = false)
    private Instant uploadedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User owner;

    public Document() {}

    public Document(String originalName, String storedName, long size, User owner) {
        this.originalName = originalName;
        this.storedName = storedName;
        this.size = size;
        this.owner = owner;
        this.status = DocumentStatus.UPLOADED;
        this.uploadedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getOriginalName() { return originalName; }
    public String getStoredName() { return storedName; }
    public long getSize() { return size; }
    public DocumentStatus getStatus() { return status; }
    public Instant getUploadedAt() { return uploadedAt; }
    public User getOwner() { return owner; }
    public void setStatus(DocumentStatus status) { this.status = status; }
}