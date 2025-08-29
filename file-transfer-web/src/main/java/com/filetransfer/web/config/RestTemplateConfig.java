package com.filetransfer.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        
        // Set timeouts for SSO testing
        factory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
        factory.setConnectionRequestTimeout((int) Duration.ofSeconds(10).toMillis());
        
        RestTemplate restTemplate = new RestTemplate(factory);
        
        // Add error handler to prevent exceptions on 4xx/5xx responses during testing
        restTemplate.setErrorHandler(new org.springframework.web.client.DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(org.springframework.http.client.ClientHttpResponse response) throws java.io.IOException {
                // Don't treat 4xx and 5xx as errors during SSO testing
                // We want to handle these responses to understand endpoint behavior
                return false;
            }
        });
        
        return restTemplate;
    }
}