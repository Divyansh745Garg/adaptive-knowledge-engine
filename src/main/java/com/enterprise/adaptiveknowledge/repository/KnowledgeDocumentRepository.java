package com.enterprise.adaptiveknowledge.repository;

import com.enterprise.adaptiveknowledge.domain.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, String> {
    Optional<KnowledgeDocument> findByCanonicalSourceId(String canonicalSourceId);
}