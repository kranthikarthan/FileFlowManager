package com.enterprise.fileflow.webapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Main application class for File Flow Management Web API Service
 */
@SpringBootApplication
@EnableFeignClients
@EnableJpaAuditing
@EntityScan(basePackages = "com.enterprise.fileflow.shared.entity")
@EnableJpaRepositories(basePackages = "com.enterprise.fileflow.webapi.repository")
public class WebApiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebApiServiceApplication.class, args);
    }
}