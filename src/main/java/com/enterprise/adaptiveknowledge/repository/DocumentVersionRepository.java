package com.enterprise.adaptiveknowledge.repository;

import com.enterprise.adaptiveknowledge.domain.DocumentVersion;
import com.enterprise.adaptiveknowledge.domain.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, String> {
    Optional<DocumentVersion> findByDocumentAndStatus(
            KnowledgeDocument document,
            DocumentVersion.VersionStatus status
    );

    @Modifying
    @Query("UPDATE DocumentVersion dv SET dv.status = :newStatus WHERE dv.document.id = :docId AND dv.status = :oldStatus")
    void updateStatusForDocId(
            @Param("docId") String docId,
            @Param("oldStatus") DocumentVersion.VersionStatus oldStatus,
            @Param("newStatus") DocumentVersion.VersionStatus newStatus
    );
}