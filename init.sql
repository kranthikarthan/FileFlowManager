-- Initialize database for file transfer application
CREATE DATABASE IF NOT EXISTS filetransfer;
USE filetransfer;

-- Create file_transfer_records table
CREATE TABLE IF NOT EXISTS file_transfer_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    service_type VARCHAR(100) NOT NULL,
    source_path VARCHAR(500) NOT NULL,
    target_path VARCHAR(500) NOT NULL,
    status ENUM('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED', 'WAITING_FOR_END_MARKER') NOT NULL,
    direction ENUM('INBOUND', 'OUTBOUND') NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP NULL,
    file_size BIGINT,
    checksum VARCHAR(255),
    batch_job_execution_id VARCHAR(255),
    INDEX idx_service_type (service_type),
    INDEX idx_status (status),
    INDEX idx_direction (direction),
    INDEX idx_created_at (created_at)
);

-- Insert sample data for testing
INSERT INTO file_transfer_records 
(file_name, service_type, source_path, target_path, status, direction, file_size, created_at, processed_at) 
VALUES 
('test_data_001.dat', 'service1', '/app/data/inbound/service1/test_data_001.dat', '/app/data/outbound/service1/test_data_001.dat', 'COMPLETED', 'INBOUND', 1024, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY),
('test_data_002.dat', 'service1', '/app/data/inbound/service1/test_data_002.dat', '/app/data/outbound/service1/test_data_002.dat', 'COMPLETED', 'INBOUND', 2048, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY),
('test_config.xml', 'service2', '/app/data/inbound/service2/test_config.xml', '/app/data/outbound/service2/test_config.xml', 'PENDING', 'INBOUND', 512, NOW() - INTERVAL 4 HOUR, NULL),
('failed_transfer.dat', 'service1', '/app/data/inbound/service1/failed_transfer.dat', '/app/data/outbound/service1/failed_transfer.dat', 'FAILED', 'INBOUND', 4096, NOW() - INTERVAL 6 HOUR, NOW() - INTERVAL 6 HOUR),
('large_file.xml', 'service2', '/app/data/inbound/service2/large_file.xml', '/app/data/outbound/service2/large_file.xml', 'IN_PROGRESS', 'INBOUND', 1048576, NOW() - INTERVAL 1 HOUR, NULL);