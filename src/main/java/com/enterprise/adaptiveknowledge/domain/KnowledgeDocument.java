package com.enterprise.adaptiveknowledge.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String canonicalSourceId; // e.g., URL, file path, GitHub link

    private String title;
    private String sourceDomain; // e.g., "kubernetes", "hr", "dpdp_act"
    private String documentType; // e.g., "documentation", "policy", "blog"

    @CreationTimestamp
    private LocalDateTime createdAt;

    private Integer latestVersion; // Tracks the current active version number
}