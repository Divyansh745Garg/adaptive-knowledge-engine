package com.enterprise.adaptiveknowledge.repository;

import com.enterprise.adaptiveknowledge.domain.Chunk;
import com.enterprise.adaptiveknowledge.domain.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChunkRepository extends JpaRepository<Chunk, String> {
    List<Chunk> findByVersion(DocumentVersion version);
}