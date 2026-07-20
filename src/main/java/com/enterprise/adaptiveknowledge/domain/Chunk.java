package com.enterprise.adaptiveknowledge.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.Map;

@Entity
@Table(name = "chunks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chunk {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    private DocumentVersion version;

    // Enables Parent-Child chunking retrieval
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_chunk_id")
    private Chunk parentChunk;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    private String heading; // Preserves section context (e.g., "Section 4.1: Data Retention")
    private Integer chunkOrder; // Allows sequential reconstruction of the document
    private Integer tokenCount;

    private String vectorDbId; // The pointer ID to the dense vector in Pinecone/Milvus

    // Hibernate 6 handles serialization of the flexible JSON payload
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;
}