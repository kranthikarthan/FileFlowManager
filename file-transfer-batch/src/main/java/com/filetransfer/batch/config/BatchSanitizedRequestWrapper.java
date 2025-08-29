package com.filetransfer.batch.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.util.StringUtils;

/**
 * Request wrapper that sanitizes input parameters
 */
public class BatchSanitizedRequestWrapper extends HttpServletRequestWrapper {

    private final BatchInputValidationService validationService;

    public BatchSanitizedRequestWrapper(HttpServletRequest request, BatchInputValidationService validationService) {
        super(request);
        this.validationService = validationService;
    }

    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        return sanitizeParameter(value);
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) {
            return null;
        }
        
        String[] sanitizedValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            sanitizedValues[i] = sanitizeParameter(values[i]);
        }
        return sanitizedValues;
    }

    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        return sanitizeParameter(value);
    }

    private String sanitizeParameter(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        
        return validationService.sanitizeInput(value);
    }
}