package com.enterprise.fileflow.batch.monitor;

import com.enterprise.fileflow.shared.entity.Service;
import com.enterprise.fileflow.shared.entity.SubService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Service for monitoring file system directories for new files
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FileMonitorService {
    
    private final FileEventHandler fileEventHandler;
    
    @Value("${fileflow.monitoring.thread-pool-size:10}")
    private int threadPoolSize;
    
    private ExecutorService executorService;
    private final Map<String, WatchService> watchServices = new ConcurrentHashMap<>();
    private final Map<String, Future<?>> monitoringTasks = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
        log.info("File monitoring service initialized with {} threads", threadPoolSize);
    }
    
    @PreDestroy
    public void destroy() {
        stopAllMonitoring();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
    
    /**
     * Start monitoring a directory for a specific service
     */
    public void startMonitoring(Service service) {
        if (service.getInputFolderPath() == null) {
            log.warn("No input folder configured for service: {}", service.getCode());
            return;
        }
        
        String serviceKey = "service_" + service.getId();
        startDirectoryMonitoring(serviceKey, service.getInputFolderPath(), service, null);
    }
    
    /**
     * Start monitoring a directory for a specific sub-service
     */
    public void startMonitoring(SubService subService) {
        String inputFolder = subService.getEffectiveInputFolderPath();
        if (inputFolder == null) {
            log.warn("No input folder configured for sub-service: {}", subService.getCode());
            return;
        }
        
        String serviceKey = "subservice_" + subService.getId();
        startDirectoryMonitoring(serviceKey, inputFolder, subService.getService(), subService);
    }
    
    /**
     * Stop monitoring for a service
     */
    public void stopMonitoring(String serviceKey) {
        Future<?> task = monitoringTasks.remove(serviceKey);
        if (task != null) {
            task.cancel(true);
        }
        
        WatchService watchService = watchServices.remove(serviceKey);
        if (watchService != null) {
            try {
                watchService.close();
                log.info("Stopped monitoring for service: {}", serviceKey);
            } catch (IOException e) {
                log.error("Error closing watch service for {}", serviceKey, e);
            }
        }
    }
    
    /**
     * Stop all monitoring activities
     */
    public void stopAllMonitoring() {
        log.info("Stopping all file monitoring activities");
        monitoringTasks.keySet().forEach(this::stopMonitoring);
    }
    
    private void startDirectoryMonitoring(String serviceKey, String directoryPath, Service service, SubService subService) {
        try {
            Path path = Paths.get(directoryPath);
            if (!Files.exists(path)) {
                log.warn("Directory does not exist: {}", directoryPath);
                return;
            }
            
            WatchService watchService = FileSystems.getDefault().newWatchService();
            path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
            
            watchServices.put(serviceKey, watchService);
            
            Future<?> task = executorService.submit(() -> monitorDirectory(serviceKey, watchService, path, service, subService));
            monitoringTasks.put(serviceKey, task);
            
            log.info("Started monitoring directory: {} for service: {}", directoryPath, serviceKey);
            
        } catch (IOException e) {
            log.error("Failed to start monitoring directory: {} for service: {}", directoryPath, serviceKey, e);
        }
    }
    
    private void monitorDirectory(String serviceKey, WatchService watchService, Path directory, Service service, SubService subService) {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key = watchService.take();
                
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path fileName = ev.context();
                    Path fullPath = directory.resolve(fileName);
                    
                    if (kind == StandardWatchEventKinds.ENTRY_CREATE || kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        handleFileEvent(fullPath, service, subService);
                    }
                }
                
                boolean valid = key.reset();
                if (!valid) {
                    log.warn("Directory key no longer valid for service: {}", serviceKey);
                    break;
                }
            }
        } catch (InterruptedException e) {
            log.info("Monitoring interrupted for service: {}", serviceKey);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Error monitoring directory for service: {}", serviceKey, e);
        }
    }
    
    private void handleFileEvent(Path filePath, Service service, SubService subService) {
        String fileName = filePath.getFileName().toString();
        
        // Check if file matches service/sub-service patterns
        if (shouldProcessFile(fileName, service, subService)) {
            log.debug("File detected for processing: {}", fileName);
            fileEventHandler.handleFileEvent(filePath, service, subService);
        }
    }
    
    private boolean shouldProcessFile(String fileName, Service service, SubService subService) {
        // Get effective patterns (sub-service overrides service)
        String fileNamePrefix = subService != null ? subService.getEffectiveFileNamePrefix() : service.getFileNamePrefix();
        String sotPattern = subService != null ? subService.getEffectiveSotPattern() : service.getSotPattern();
        String eotPattern = subService != null ? subService.getEffectiveEotPattern() : service.getEotPattern();
        String dataFilePattern = subService != null ? subService.getEffectiveDataFilePattern() : service.getDataFilePattern();
        
        // Check prefix match
        if (fileNamePrefix != null && !fileName.startsWith(fileNamePrefix)) {
            return false;
        }
        
        // Check if file matches any of the patterns
        return matchesPattern(fileName, sotPattern) || 
               matchesPattern(fileName, eotPattern) || 
               matchesPattern(fileName, dataFilePattern);
    }
    
    private boolean matchesPattern(String fileName, String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            return false;
        }
        
        try {
            return Pattern.matches(pattern, fileName);
        } catch (Exception e) {
            log.warn("Invalid regex pattern: {} for file: {}", pattern, fileName);
            return false;
        }
    }
}