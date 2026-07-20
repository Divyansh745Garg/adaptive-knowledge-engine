package com.enterprise.adaptiveknowledge.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_versions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private KnowledgeDocument document;

    @Column(nullable = false)
    private Integer versionNumber;

    @Column(nullable = false)
    private String contentChecksum; // MD5/SHA-256 for fast change detection

    private LocalDate effectiveDate; // Crucial for tracking policy/compliance timelines

    @Enumerated(EnumType.STRING)
    private VersionStatus status; // ACTIVE, SUPERSEDED

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum VersionStatus {
        ACTIVE, SUPERSEDED
    }
}