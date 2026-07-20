package com.enterprise.adaptiveknowledge.ingestion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.io.File;
import java.nio.file.Files;

@SpringBootTest
public class IngestionVerificationTest {

    @Autowired
    private IngestionService ingestionService;

    @Test
    public void verifyIncrementalIngestionPipeline() throws Exception {
        String docSource = "file://HR/leave-policy.pdf";

        // 1. Ingest Version 1 (15 Days)
        File v1File = PdfTestUtils.createTextPdf("v1.pdf",
                "Corporate Leave Policy",
                "All full-time employees are allocated 15 days of annual paid leave.");

        System.out.println("=== Starting Test: Ingesting Version 1 ===");
        ingestionService.ingestDocument(docSource, "Leave Policy", "HR", "Policy", "pdf",
                Files.readAllBytes(v1File.toPath())).get();

        // 2. Ingest duplicate file to test the early exit logic
        System.out.println("=== Starting Test: Re-ingesting Version 1 (Should Skip) ===");
        ingestionService.ingestDocument(docSource, "Leave Policy", "HR", "Policy", "pdf",
                Files.readAllBytes(v1File.toPath())).get();

        // 3. Ingest Version 2 (18 Days modification)
        File v2File = PdfTestUtils.createTextPdf("v2.pdf",
                "Corporate Leave Policy",
                "All full-time employees are allocated 18 days of annual paid leave.");

        System.out.println("=== Starting Test: Ingesting Version 2 (Should Mutate) ===");
        ingestionService.ingestDocument(docSource, "Leave Policy", "HR", "Policy", "pdf",
                Files.readAllBytes(v2File.toPath())).get();
    }
}