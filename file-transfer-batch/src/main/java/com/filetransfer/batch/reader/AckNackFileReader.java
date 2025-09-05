package com.filetransfer.batch.reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.stream.Stream;

/**
 * Batch reader for ACK/NACK files from the incoming directory
 */
@Component
public class AckNackFileReader implements ItemReader<Path> {
    
    private static final Logger logger = LoggerFactory.getLogger(AckNackFileReader.class);
    
    @Value("${file-transfer.ack-nack.incoming-path:/data/incoming/ack-nack}")
    private String incomingAckNackPath;
    
    private Iterator<Path> fileIterator;
    private boolean initialized = false;
    
    @Override
    public Path read() throws Exception {
        if (!initialized) {
            initialize();
        }
        
        if (fileIterator != null && fileIterator.hasNext()) {
            Path nextFile = fileIterator.next();
            logger.debug("Reading ACK/NACK file: {}", nextFile);
            return nextFile;
        }
        
        return null; // End of data
    }
    
    private void initialize() throws IOException {
        Path incomingDir = Paths.get(incomingAckNackPath);
        
        if (!Files.exists(incomingDir)) {
            logger.warn("ACK/NACK incoming directory does not exist: {}", incomingDir);
            Files.createDirectories(incomingDir);
            fileIterator = null;
            initialized = true;
            return;
        }
        
        try (Stream<Path> fileStream = Files.walk(incomingDir)) {
            fileIterator = fileStream
                .filter(Files::isRegularFile)
                .filter(this::isAckNackFile)
                .iterator();
        }
        
        initialized = true;
        logger.info("Initialized ACK/NACK file reader for directory: {}", incomingDir);
    }
    
    private boolean isAckNackFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.endsWith(".ack") || fileName.endsWith(".nack");
    }
    
    /**
     * Reset the reader state for a new job execution
     */
    public void reset() {
        initialized = false;
        fileIterator = null;
    }
}