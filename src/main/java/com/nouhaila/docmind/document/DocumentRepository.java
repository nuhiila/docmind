package com.nouhaila.docmind.document;

import com.nouhaila.docmind.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByOwnerOrderByUploadedAtDesc(User owner);

    Optional<Document> findByIdAndOwner(Long id, User owner);

    @Query("select d from Document d join fetch d.owner where d.id = :id")
    Optional<Document> findWithOwnerById(@Param("id") Long id);
}