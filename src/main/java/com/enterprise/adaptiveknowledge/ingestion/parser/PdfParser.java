package com.enterprise.adaptiveknowledge.ingestion.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class PdfParser implements DocumentParser {

    @Override
    public ParsedContent parse(byte[] fileBytes) {
        try (PDDocument document = Loader.loadPDF(fileBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String rawText = stripper.getText(document);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("pageCount", document.getNumberOfPages());
            metadata.put("isEncrypted", document.isEncrypted());

            return new ParsedContent(rawText, metadata);
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract content from incoming PDF payload", e);
        }
    }

    @Override
    public boolean supports(String fileExtension) {
        return "pdf".equalsIgnoreCase(fileExtension);
    }
}