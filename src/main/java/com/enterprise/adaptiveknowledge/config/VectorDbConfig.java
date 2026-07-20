package com.enterprise.adaptiveknowledge.config;

import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;

@Configuration
public class VectorDbConfig {

    // 1. Simulates a Vector DB locally
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }

    // 2. Runs a real AI embedding model locally on your CPU (No API Key needed)
    @Bean
    public EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }
}