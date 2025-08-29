package com.filetransfer.web.versioning;

import java.lang.annotation.*;

/**
 * Annotation to specify API version for controllers and methods
 * Supports multiple versioning strategies and backward compatibility
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiVersion {
    
    /**
     * The API version(s) this endpoint supports
     * Can specify multiple versions for backward compatibility
     */
    String[] value();
    
    /**
     * Minimum supported version (for range-based versioning)
     */
    String since() default "";
    
    /**
     * Maximum supported version (for deprecation)
     */
    String until() default "";
    
    /**
     * Whether this version is deprecated
     */
    boolean deprecated() default false;
    
    /**
     * Deprecation message to include in response headers
     */
    String deprecationMessage() default "";
    
    /**
     * Sunset date for deprecated versions (ISO 8601 format)
     */
    String sunsetDate() default "";
    
    /**
     * Whether to include version information in response headers
     */
    boolean includeVersionHeaders() default true;
    
    /**
     * Custom versioning strategy for this endpoint
     */
    VersioningStrategy strategy() default VersioningStrategy.DEFAULT;
    
    /**
     * Whether this endpoint requires explicit version specification
     */
    boolean requiresVersion() default false;
}