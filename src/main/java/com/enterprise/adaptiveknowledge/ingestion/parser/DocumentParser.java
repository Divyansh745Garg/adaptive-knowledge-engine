package com.enterprise.adaptiveknowledge.ingestion.parser;

public interface DocumentParser {
    ParsedContent parse(byte[] fileBytes);
    boolean supports(String fileExtension);
}