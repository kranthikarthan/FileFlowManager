package com.filetransfer.web.config;

import com.filetransfer.web.versioning.ApiVersionInterceptor;
import com.filetransfer.web.versioning.ApiVersionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for API versioning system
 */
@Configuration
@EnableScheduling
public class ApiVersioningConfig implements WebMvcConfigurer {
    
    @Autowired
    private ApiVersionInterceptor apiVersionInterceptor;
    
    @Autowired
    private ApiVersionManager apiVersionManager;
    
    /**
     * Register API version interceptor
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiVersionInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                    "/api/versions/health",
                    "/api/health",
                    "/api/actuator/**",
                    "/api/swagger-ui/**",
                    "/api/v3/api-docs/**"
                );
    }
    
    /**
     * Initialize version manager after application startup
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeVersionManager() {
        apiVersionManager.initialize();
    }
    
    /**
     * Scheduled task to check version lifecycle
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void checkVersionLifecycle() {
        apiVersionManager.checkVersionLifecycle();
    }
}