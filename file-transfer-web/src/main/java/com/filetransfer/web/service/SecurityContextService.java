package com.filetransfer.web.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SecurityContextService {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityContextService.class);
    
    /**
     * Gets the current authenticated user identifier
     * @return the user identifier or "system" as fallback
     */
    public String getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.debug("No authentication found, using system user");
                return "system";
            }
            
            // Handle different authentication types
            Object principal = authentication.getPrincipal();
            
            if (principal instanceof OAuth2User) {
                OAuth2User oauth2User = (OAuth2User) principal;
                // Try common user identifier attributes
                String userId = extractUserIdFromOAuth2User(oauth2User);
                return userId != null ? userId : "oauth2-" + oauth2User.getName();
            } else if (principal instanceof Jwt) {
                Jwt jwt = (Jwt) principal;
                // Extract user ID from JWT claims
                String userId = extractUserIdFromJwt(jwt);
                return userId != null ? userId : "jwt-" + jwt.getSubject();
            } else if (principal instanceof String) {
                return (String) principal;
            } else {
                // Fallback to authentication name
                String name = authentication.getName();
                return name != null ? name : "system";
            }
            
        } catch (Exception e) {
            logger.warn("Error getting current user ID, falling back to system: {}", e.getMessage());
            return "system";
        }
    }
    
    /**
     * Gets the current authenticated user's email
     * @return the user email or empty optional
     */
    public Optional<String> getCurrentUserEmail() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.empty();
            }
            
            Object principal = authentication.getPrincipal();
            
            if (principal instanceof OAuth2User) {
                OAuth2User oauth2User = (OAuth2User) principal;
                String email = oauth2User.getAttribute("email");
                return Optional.ofNullable(email);
            } else if (principal instanceof Jwt) {
                Jwt jwt = (Jwt) principal;
                String email = jwt.getClaimAsString("email");
                return Optional.ofNullable(email);
            }
            
            return Optional.empty();
            
        } catch (Exception e) {
            logger.warn("Error getting current user email: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Gets the current authenticated user's display name
     * @return the user display name or empty optional
     */
    public Optional<String> getCurrentUserDisplayName() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.empty();
            }
            
            Object principal = authentication.getPrincipal();
            
            if (principal instanceof OAuth2User) {
                OAuth2User oauth2User = (OAuth2User) principal;
                // Try common display name attributes
                String displayName = oauth2User.getAttribute("name");
                if (displayName == null) {
                    displayName = oauth2User.getAttribute("display_name");
                }
                if (displayName == null) {
                    displayName = oauth2User.getAttribute("given_name") + " " + oauth2User.getAttribute("family_name");
                    displayName = displayName.trim();
                    if (displayName.isEmpty()) {
                        displayName = null;
                    }
                }
                return Optional.ofNullable(displayName);
            } else if (principal instanceof Jwt) {
                Jwt jwt = (Jwt) principal;
                String name = jwt.getClaimAsString("name");
                if (name == null) {
                    name = jwt.getClaimAsString("preferred_username");
                }
                return Optional.ofNullable(name);
            }
            
            return Optional.empty();
            
        } catch (Exception e) {
            logger.warn("Error getting current user display name: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Checks if a user is currently authenticated
     * @return true if authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            return authentication != null && authentication.isAuthenticated() && 
                   !"anonymousUser".equals(authentication.getName());
        } catch (Exception e) {
            logger.warn("Error checking authentication status: {}", e.getMessage());
            return false;
        }
    }
    
    private String extractUserIdFromOAuth2User(OAuth2User oauth2User) {
        // Try different common user ID attributes based on provider
        String userId = oauth2User.getAttribute("sub"); // Standard OIDC subject
        if (userId == null) {
            userId = oauth2User.getAttribute("id"); // Common fallback
        }
        if (userId == null) {
            userId = oauth2User.getAttribute("user_id"); // Some providers use this
        }
        if (userId == null) {
            userId = oauth2User.getAttribute("email"); // Use email as fallback
        }
        return userId;
    }
    
    private String extractUserIdFromJwt(Jwt jwt) {
        // Try different JWT claims for user ID
        String userId = jwt.getSubject(); // Standard subject claim
        if (userId == null) {
            userId = jwt.getClaimAsString("user_id");
        }
        if (userId == null) {
            userId = jwt.getClaimAsString("email");
        }
        if (userId == null) {
            userId = jwt.getClaimAsString("preferred_username");
        }
        return userId;
    }
}