package com.filetransfer.batch.versioning;

/**
 * Enumeration of supported API versioning strategies for batch application
 */
public enum VersioningStrategy {
    
    /**
     * Use the default configured versioning strategy
     */
    DEFAULT,
    
    /**
     * Version specified in URL path: /api/batch/v1/jobs
     */
    URL_PATH,
    
    /**
     * Version specified in request header: X-Batch-API-Version: 1.0
     */
    HEADER,
    
    /**
     * Version specified in query parameter: ?version=1.0
     */
    QUERY_PARAMETER,
    
    /**
     * Version specified in Accept header: Accept: application/vnd.filetransfer.batch.v1+json
     */
    MEDIA_TYPE,
    
    /**
     * Version specified in custom header: Accept-Version: 1.0
     */
    ACCEPT_HEADER,
    
    /**
     * Multiple strategies supported (fallback order)
     */
    HYBRID,
    
    /**
     * Semantic versioning with range support
     */
    SEMANTIC,
    
    /**
     * Date-based versioning
     */
    DATE_BASED,
    
    /**
     * No versioning (legacy endpoints)
     */
    NONE
}