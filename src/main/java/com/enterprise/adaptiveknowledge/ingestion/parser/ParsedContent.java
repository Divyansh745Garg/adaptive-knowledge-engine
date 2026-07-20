package com.enterprise.adaptiveknowledge.ingestion.parser;

import java.util.Map;

/**
 * A canonical data record wrapping raw text alongside runtime metadata
 * discovered during document extraction.
 */
public record ParsedContent(String rawText, Map<String, Object> metadata) {}