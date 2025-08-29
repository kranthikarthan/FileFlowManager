package com.filetransfer.web.config;

import brave.Tracing;
import brave.context.slf4j.MDCScopeDecorator;
import brave.propagation.ThreadLocalCurrentTraceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.okhttp3.OkHttpSender;

/**
 * Configuration for distributed tracing
 * Sets up Zipkin for trace collection and correlation
 */
@Configuration
public class TracingConfig {

    @Value("${management.zipkin.tracing.endpoint:http://localhost:9411/api/v2/spans}")
    private String zipkinEndpoint;

    @Value("${spring.application.name:file-transfer-web}")
    private String serviceName;

    /**
     * Configure Zipkin sender for trace export
     */
    @Bean
    public OkHttpSender zipkinSender() {
        return OkHttpSender.create(zipkinEndpoint);
    }

    /**
     * Configure async reporter for better performance
     */
    @Bean
    public AsyncReporter<zipkin2.Span> zipkinAsyncReporter(OkHttpSender sender) {
        return AsyncReporter.create(sender);
    }

    /**
     * Configure tracing with correlation ID support
     */
    @Bean
    public Tracing tracing(AsyncReporter<zipkin2.Span> spanReporter) {
        return Tracing.newBuilder()
            .localServiceName(serviceName)
            .spanReporter(spanReporter)
            .currentTraceContext(
                ThreadLocalCurrentTraceContext.newBuilder()
                    .addScopeDecorator(MDCScopeDecorator.get()) // Add traceId to MDC
                    .build()
            )
            .build();
    }
}