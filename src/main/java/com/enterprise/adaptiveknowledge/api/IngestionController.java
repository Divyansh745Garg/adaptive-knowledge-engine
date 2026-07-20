package com.enterprise.adaptiveknowledge.api;

import com.enterprise.adaptiveknowledge.ingestion.IngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ingestion")
@RequiredArgsConstructor
public class IngestionController {

    private final IngestionService ingestionService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("domain") String domain,
            @RequestParam("type") String type) {

        try {
            // Use the original filename or build a distinct unique identifier asset path
            String sourceId = "file://" + domain + "/" + file.getOriginalFilename();
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf(".") + 1)
                    : "pdf";

            byte[] fileBytes = file.getBytes();

            // Hand execution off completely to Loom virtual threads asynchronously
            ingestionService.ingestDocument(sourceId, title, domain, type, extension, fileBytes);

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                    "status", "QUEUED",
                    "trackingId", UUID.randomUUID().toString(),
                    "message", "Document submitted for background transformation and vectorization."
            ));

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "FAILED",
                    "error", "Could not process file bytes: " + e.getMessage()
            ));
        }
    }
}