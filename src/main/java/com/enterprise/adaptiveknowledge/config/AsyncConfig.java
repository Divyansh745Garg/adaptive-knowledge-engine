package com.enterprise.adaptiveknowledge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "virtualThreadExecutor")
    public Executor virtualThreadExecutor() {
        // Leverages Java 21 lightweight virtual threads natively
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}