package com.filetransfer.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableBatchProcessing
@EnableScheduling
public class FileTransferBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileTransferBatchApplication.class, args);
    }
}