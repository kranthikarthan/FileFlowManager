package com.filetransfer.batch.config;

import com.filetransfer.batch.entity.AckNackRecord;
import com.filetransfer.batch.processor.AckNackFileProcessor;
import com.filetransfer.batch.reader.AckNackFileReader;
import com.filetransfer.batch.writer.AckNackFileWriter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.nio.file.Path;

/**
 * Batch configuration for processing ACK/NACK files
 */
@Configuration
public class AckNackBatchConfig {
    
    @Autowired
    private JobRepository jobRepository;
    
    @Autowired
    private PlatformTransactionManager transactionManager;
    
    @Bean
    public ItemReader<Path> ackNackFileReader() {
        return new AckNackFileReader();
    }
    
    @Bean
    public ItemProcessor<Path, AckNackRecord> ackNackFileProcessor() {
        return new AckNackFileProcessor();
    }
    
    @Bean
    public ItemWriter<AckNackRecord> ackNackFileWriter() {
        return new AckNackFileWriter();
    }
    
    @Bean
    public Step processAckNackFilesStep() {
        return new StepBuilder("processAckNackFilesStep", jobRepository)
                .<Path, AckNackRecord>chunk(10, transactionManager)
                .reader(ackNackFileReader())
                .processor(ackNackFileProcessor())
                .writer(ackNackFileWriter())
                .build();
    }
    
    @Bean
    public Job processAckNackFilesJob() {
        return new JobBuilder("processAckNackFilesJob", jobRepository)
                .start(processAckNackFilesStep())
                .build();
    }
}