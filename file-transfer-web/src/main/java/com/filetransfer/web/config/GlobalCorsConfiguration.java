package com.filetransfer.web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Global CORS configuration to ensure consistent and secure cross-origin policies
 */
@Configuration
public class GlobalCorsConfiguration {
    
    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private List<String> allowedOrigins;
    
    @Value("${spring.profiles.active:development}")
    private String activeProfile;
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Set allowed origins based on environment
        if ("production".equals(activeProfile)) {
            configuration.setAllowedOrigins(List.of(
                "https://filetransfer.company.com",
                "https://api.filetransfer.company.com"
            ));
        } else if ("staging".equals(activeProfile)) {
            configuration.setAllowedOrigins(List.of(
                "https://staging.filetransfer.company.com",
                "http://localhost:3000"
            ));
        } else {
            // Development environment
            configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:3001"
            ));
        }
        
        // Set allowed methods
        configuration.setAllowedMethods(List.of(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));
        
        // Set allowed headers
        configuration.setAllowedHeaders(List.of(
            "Authorization", 
            "Content-Type", 
            "X-Requested-With",
            "X-Tenant-ID",
            "X-Correlation-ID"
        ));
        
        // Allow credentials for authentication
        configuration.setAllowCredentials(true);
        
        // Cache preflight requests for 1 hour
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        
        return source;
    }
}