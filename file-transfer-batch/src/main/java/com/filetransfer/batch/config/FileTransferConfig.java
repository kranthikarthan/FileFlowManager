package com.filetransfer.batch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.HashMap;

@Component
@ConfigurationProperties(prefix = "file-transfer")
public class FileTransferConfig {
    
    private Map<String, ServiceConfig> services = new HashMap<>();
    private int pollIntervalSeconds = 30;
    private boolean enabled = true;
    
    public static class ServiceConfig {
        private String inboundPath;
        private String outboundPath;
        private String startMarkerPrefix = "SOT_";
        private String endMarkerPrefix = "EOT_";
        private String dataFilePattern = "*.dat";
        private boolean enabled = true;
        private int maxRetries = 3;
        
        // Getters and Setters
        public String getInboundPath() { return inboundPath; }
        public void setInboundPath(String inboundPath) { this.inboundPath = inboundPath; }
        
        public String getOutboundPath() { return outboundPath; }
        public void setOutboundPath(String outboundPath) { this.outboundPath = outboundPath; }
        
        public String getStartMarkerPrefix() { return startMarkerPrefix; }
        public void setStartMarkerPrefix(String startMarkerPrefix) { this.startMarkerPrefix = startMarkerPrefix; }
        
        public String getEndMarkerPrefix() { return endMarkerPrefix; }
        public void setEndMarkerPrefix(String endMarkerPrefix) { this.endMarkerPrefix = endMarkerPrefix; }
        
        public String getDataFilePattern() { return dataFilePattern; }
        public void setDataFilePattern(String dataFilePattern) { this.dataFilePattern = dataFilePattern; }
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    }
    
    // Getters and Setters
    public Map<String, ServiceConfig> getServices() { return services; }
    public void setServices(Map<String, ServiceConfig> services) { this.services = services; }
    
    public int getPollIntervalSeconds() { return pollIntervalSeconds; }
    public void setPollIntervalSeconds(int pollIntervalSeconds) { this.pollIntervalSeconds = pollIntervalSeconds; }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}