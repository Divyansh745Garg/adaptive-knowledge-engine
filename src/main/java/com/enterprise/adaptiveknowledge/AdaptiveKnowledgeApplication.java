package com.enterprise.adaptiveknowledge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import java.util.TimeZone;

@SpringBootApplication
@EnableAsync
public class AdaptiveKnowledgeApplication {
	public static void main(String[] args) {
		// Force the JVM to use UTC, bypassing the Windows "Asia/Calcutta" bug
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

		SpringApplication.run(AdaptiveKnowledgeApplication.class, args);
	}
}
