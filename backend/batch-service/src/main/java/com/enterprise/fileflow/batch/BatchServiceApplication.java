package com.enterprise.fileflow.batch;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for File Flow Management Batch Service
 */
@SpringBootApplication
@EnableBatchProcessing
@EnableScheduling
@EnableAsync
@EnableFeignClients
@EnableJpaAuditing
@EntityScan(basePackages = "com.enterprise.fileflow.shared.entity")
@EnableJpaRepositories(basePackages = "com.enterprise.fileflow.batch.repository")
public class BatchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BatchServiceApplication.class, args);
    }
}