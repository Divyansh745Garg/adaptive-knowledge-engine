package com.enterprise.adaptiveknowledge.ingestion;

import com.enterprise.adaptiveknowledge.domain.*;
import com.enterprise.adaptiveknowledge.repository.*;
import com.enterprise.adaptiveknowledge.ingestion.parser.*;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class IngestionService {

    private final KnowledgeDocumentRepository docRepository;
    private final DocumentVersionRepository versionRepository;
    private final ChunkRepository chunkRepository;
    private final List<DocumentParser> parsers;

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<dev.langchain4j.data.segment.TextSegment> embeddingStore;

    /**
     * Executes asynchronous processing of an incoming document utilizing
     * Java 21 Virtual Threads to ensure non-blocking operation.
     */
    @Async("virtualThreadExecutor")
    @Transactional
    public CompletableFuture<Void> ingestDocument(
            String sourceId,
            String title,
            String domain,
            String type,
            String extension,
            byte[] fileBytes) {

        log.info("Processing ingestion task for source: {} via virtual thread: {}", sourceId, Thread.currentThread());

        // 1. Resolve appropriate file parser
        DocumentParser parser = parsers.stream()
                .filter(p -> p.supports(extension))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported file type: " + extension));

        ParsedContent parsedContent = parser.parse(fileBytes);

        // 2. Performance Safeguard: Compute checksum to verify content modifications
        String newChecksum = DigestUtils.md5DigestAsHex(parsedContent.rawText().getBytes());

        // 3. Query existing state within relational document registry
        KnowledgeDocument document = docRepository.findByCanonicalSourceId(sourceId)
                .orElseGet(() -> docRepository.save(KnowledgeDocument.builder()
                        .canonicalSourceId(sourceId)
                        .title(title)
                        .sourceDomain(domain)
                        .documentType(type)
                        .latestVersion(0)
                        .build()));

        Optional<DocumentVersion> activeVersionOpt = versionRepository
                .findByDocumentAndStatus(document, DocumentVersion.VersionStatus.ACTIVE);

        if (activeVersionOpt.isPresent() && activeVersionOpt.get().getContentChecksum().equals(newChecksum)) {
            log.info("Document tracking checksum matches current index instance. Ingestion skipped for: {}", sourceId);
            return CompletableFuture.completedFuture(null);
        }

        // 4. Implement structural version rollover
        int nextVersionNumber = document.getLatestVersion() + 1;

        if (activeVersionOpt.isPresent()) {
            DocumentVersion oldVersion = activeVersionOpt.get();
            oldVersion.setStatus(DocumentVersion.VersionStatus.SUPERSEDED);
            versionRepository.save(oldVersion);

            // Clean up vectors from previous active state to ensure index cleanliness
            List<Chunk> oldChunks = chunkRepository.findByVersion(oldVersion);
            for (Chunk oldChunk : oldChunks) {
                if (oldChunk.getVectorDbId() != null) {
                    embeddingStore.remove(oldChunk.getVectorDbId());
                }
            }
            log.info("Evicted old vector allocations for document: {}, version: {}", sourceId, oldVersion.getVersionNumber());
        }

        // 5. Instantiating tracking record for new active version
        DocumentVersion newVersion = versionRepository.save(DocumentVersion.builder()
                .document(document)
                .versionNumber(nextVersionNumber)
                .contentChecksum(newChecksum)
                .effectiveDate(LocalDate.now())
                .status(DocumentVersion.VersionStatus.ACTIVE)
                .build());

        document.setLatestVersion(nextVersionNumber);
        docRepository.save(document);

        // 6. Execution of chunk parsing, vector token allocation, and index storage
        List<String> rawChunks = simpleSlidingWindowSplitter(parsedContent.rawText(), 500, 100);
        int chunkOrderCounter = 0;

        for (String chunkText : rawChunks) {
            //String vectorId = UUID.randomUUID().toString();

            // Compute dense semantic embedding vectors asynchronously
            Embedding embedding = embeddingModel.embed(chunkText).content();

            // Append target structural metadata payload for custom vector routing operations
            dev.langchain4j.data.segment.TextSegment segment = dev.langchain4j.data.segment.TextSegment.from(
                    chunkText,
                    dev.langchain4j.data.document.Metadata.from(Map.of(
                            "docId", document.getId(),
                            "version", nextVersionNumber,
                            "domain", domain
                    ))
            );

            String vectorId = embeddingStore.add(embedding, segment);

            // Sync structured state back to PostgreSQL registry instances
            Map<String, Object> combinedMetadata = new HashMap<>(parsedContent.metadata());
            combinedMetadata.put("processedByVirtualThreads", true);

            chunkRepository.save(Chunk.builder()
                    .version(newVersion)
                    .text(chunkText)
                    .chunkOrder(chunkOrderCounter++)
                    .tokenCount(chunkText.length() / 4) // Approximate token footprint representation
                    .vectorDbId(vectorId)
                    .metadata(combinedMetadata)
                    .build());
        }

        log.info("Successfully indexed version {} of document {} with {} chunks.", nextVersionNumber, sourceId, chunkOrderCounter);
        return CompletableFuture.completedFuture(null);
    }

    private List<String> simpleSlidingWindowSplitter(String text, int limit, int overlap) {
        List<String> textChunks = new ArrayList<>();
        int index = 0;
        while (index < text.length()) {
            int end = Math.min(index + limit, text.length());
            textChunks.add(text.substring(index, end));
            if (end == text.length()) break;
            index += (limit - overlap);
        }
        return textChunks;
    }
}