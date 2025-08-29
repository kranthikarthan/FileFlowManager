package com.filetransfer.batch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for batch application
 * Implements comprehensive security including rate limiting, input validation, and secure headers
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class BatchSecurityConfig {

    @Value("${batch.security.cors.allowed-origins:http://localhost:3000,http://localhost:8080}")
    private String[] allowedOrigins;

    @Value("${batch.security.jwt.secret:batch-secret-key-2023}")
    private String jwtSecret;

    @Value("${batch.security.rate-limiting.enabled:true}")
    private boolean rateLimitingEnabled;

    /**
     * Main security filter chain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF configuration
            .csrf(csrf -> csrf
                .csrfTokenRepository(org.springframework.security.web.csrf.CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/api/batch/public/**", "/actuator/**")
            )
            
            // CORS configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Session management
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Security headers
            .headers(headers -> headers
                .frameOptions().deny()
                .contentTypeOptions().and()
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubdomains(true)
                )
                .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                .and()
                .headers(headersConfig -> headersConfig
                    .addHeaderWriter((request, response) -> {
                        response.setHeader("X-Content-Type-Options", "nosniff");
                        response.setHeader("X-XSS-Protection", "1; mode=block");
                        response.setHeader("X-Frame-Options", "DENY");
                        response.setHeader("Content-Security-Policy", 
                            "default-src 'self'; " +
                            "script-src 'self' 'unsafe-inline'; " +
                            "style-src 'self' 'unsafe-inline'; " +
                            "img-src 'self' data:; " +
                            "font-src 'self'; " +
                            "connect-src 'self'; " +
                            "frame-ancestors 'none'");
                    })
                )
            )
            
            // Authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/api/batch/public/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                
                // Monitoring endpoints (admin only)
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                
                // Job management endpoints
                .requestMatchers("/api/batch/scalability/jobs/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/api/batch/scalability/performance/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/api/batch/scalability/statistics/**").hasAnyRole("ADMIN", "MANAGER", "USER")
                
                // All other requests require authentication
                .anyRequest().authenticated()
            );

        // Add custom filters
        if (rateLimitingEnabled) {
            http.addFilterBefore(batchRateLimitingFilter(), UsernamePasswordAuthenticationFilter.class);
        }
        
        http.addFilterBefore(batchInputValidationFilter(), UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(batchSecurityHeadersFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS configuration
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    /**
     * Password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Rate limiting filter for batch endpoints
     */
    @Bean
    public BatchRateLimitingFilter batchRateLimitingFilter() {
        return new BatchRateLimitingFilter();
    }

    /**
     * Input validation filter
     */
    @Bean
    public BatchInputValidationFilter batchInputValidationFilter() {
        return new BatchInputValidationFilter();
    }

    /**
     * Security headers filter
     */
    @Bean
    public BatchSecurityHeadersFilter batchSecurityHeadersFilter() {
        return new BatchSecurityHeadersFilter();
    }

    /**
     * Encryption service for sensitive data
     */
    @Bean
    public BatchEncryptionService batchEncryptionService() {
        return new BatchEncryptionService(jwtSecret);
    }

    /**
     * Input validation service
     */
    @Bean
    public BatchInputValidationService batchInputValidationService() {
        return new BatchInputValidationService();
    }
}