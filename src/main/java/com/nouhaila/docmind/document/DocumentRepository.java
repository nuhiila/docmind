package com.nouhaila.docmind.document;

import com.nouhaila.docmind.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByOwnerOrderByUploadedAtDesc(User owner);
}