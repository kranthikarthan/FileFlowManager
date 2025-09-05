-- Migration to add file tagging system
-- Version: V105
-- Description: Add file tagging capabilities for better organization and categorization

-- Create file_tags table
CREATE TABLE file_tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    tag_name VARCHAR(100) NOT NULL,
    tag_color VARCHAR(7) DEFAULT '#2196f3', -- Hex color code
    tag_description VARCHAR(500),
    is_system_tag BOOLEAN DEFAULT FALSE,
    usage_count INT DEFAULT 0,
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_file_tags (tenant_id, tag_name),
    INDEX idx_file_tags_tenant (tenant_id),
    INDEX idx_file_tags_system (tenant_id, is_system_tag),
    INDEX idx_file_tags_usage (tenant_id, usage_count DESC),
    INDEX idx_file_tags_created_by (tenant_id, created_by),
    INDEX idx_file_tags_name (tenant_id, tag_name)
);

-- Create file_transfer_tags table (many-to-many relationship)
CREATE TABLE file_transfer_tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_transfer_id BIGINT NOT NULL,
    file_tag_id BIGINT NOT NULL,
    tagged_by VARCHAR(100),
    tagged_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tag_reason VARCHAR(500),
    auto_tagged BOOLEAN DEFAULT FALSE,
    
    FOREIGN KEY (file_transfer_id) REFERENCES file_transfer_records(id) ON DELETE CASCADE,
    FOREIGN KEY (file_tag_id) REFERENCES file_tags(id) ON DELETE CASCADE,
    UNIQUE KEY uk_file_transfer_tags (file_transfer_id, file_tag_id),
    INDEX idx_file_transfer_tags_file (file_transfer_id),
    INDEX idx_file_transfer_tags_tag (file_tag_id),
    INDEX idx_file_transfer_tags_tagged_by (tagged_by),
    INDEX idx_file_transfer_tags_auto (auto_tagged),
    INDEX idx_file_transfer_tags_date (tagged_at)
);

-- Insert default system tags
INSERT INTO file_tags (tenant_id, tag_name, tag_color, tag_description, is_system_tag, created_by) VALUES
('default', 'high-priority', '#f44336', 'High priority files requiring immediate attention', TRUE, 'SYSTEM'),
('default', 'reviewed', '#4caf50', 'Files that have been reviewed and approved', TRUE, 'SYSTEM'),
('default', 'needs-attention', '#ff9800', 'Files requiring manual review or action', TRUE, 'SYSTEM'),
('default', 'archived', '#9e9e9e', 'Files that have been archived', TRUE, 'SYSTEM'),
('default', 'sensitive-data', '#e91e63', 'Files containing sensitive or confidential data', TRUE, 'SYSTEM'),
('default', 'large-file', '#3f51b5', 'Large files that may require special handling', TRUE, 'SYSTEM'),
('default', 'quality-issues', '#ff5722', 'Files with data quality issues detected', TRUE, 'SYSTEM'),
('default', 'processed', '#2196f3', 'Files that have been successfully processed', TRUE, 'SYSTEM'),
('default', 'customer-data', '#009688', 'Files containing customer information', TRUE, 'SYSTEM'),
('default', 'transaction-data', '#795548', 'Files containing transaction records', TRUE, 'SYSTEM'),
('default', 'log-data', '#607d8b', 'System or application log files', TRUE, 'SYSTEM'),
('default', 'backup-data', '#8bc34a', 'Backup or archive files', TRUE, 'SYSTEM');

-- Create tag usage statistics table
CREATE TABLE tag_usage_statistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    tag_id BIGINT NOT NULL,
    stat_date DATE NOT NULL,
    files_tagged BIGINT DEFAULT 0,
    files_untagged BIGINT DEFAULT 0,
    unique_users BIGINT DEFAULT 0,
    auto_tagged_files BIGINT DEFAULT 0,
    manual_tagged_files BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (tag_id) REFERENCES file_tags(id) ON DELETE CASCADE,
    UNIQUE KEY uk_tag_usage_stats (tenant_id, tag_id, stat_date),
    INDEX idx_tag_usage_stats_tenant_date (tenant_id, stat_date),
    INDEX idx_tag_usage_stats_tag_date (tag_id, stat_date)
);

-- Add tagging-related fields to file_transfer_records
ALTER TABLE file_transfer_records 
ADD COLUMN tag_count INT DEFAULT 0,
ADD COLUMN auto_tagged BOOLEAN DEFAULT FALSE,
ADD COLUMN last_tagged_at TIMESTAMP NULL,
ADD COLUMN last_tagged_by VARCHAR(100);

-- Add indexes for tagging queries
CREATE INDEX idx_file_transfer_tag_count ON file_transfer_records(tenant_id, tag_count);
CREATE INDEX idx_file_transfer_auto_tagged ON file_transfer_records(tenant_id, auto_tagged);
CREATE INDEX idx_file_transfer_last_tagged ON file_transfer_records(tenant_id, last_tagged_at);

-- Create view for file transfers with tag information
CREATE VIEW file_transfers_with_tags AS
SELECT 
    ftr.id,
    ftr.tenant_id,
    ftr.file_name,
    ftr.service_name,
    ftr.sub_service_name,
    ftr.status,
    ftr.direction,
    ftr.file_size,
    ftr.file_extension,
    ftr.created_at,
    ftr.tag_count,
    GROUP_CONCAT(ft.tag_name ORDER BY ft.tag_name SEPARATOR ', ') as tag_names,
    GROUP_CONCAT(ft.tag_color ORDER BY ft.tag_name SEPARATOR ', ') as tag_colors,
    COUNT(ft.id) as actual_tag_count
FROM file_transfer_records ftr
LEFT JOIN file_transfer_tags ftt ON ftr.id = ftt.file_transfer_id
LEFT JOIN file_tags ft ON ftt.file_tag_id = ft.id
GROUP BY ftr.id;

-- Create stored procedure to update tag usage statistics
DELIMITER //
CREATE PROCEDURE UpdateTagUsageStatistics(IN target_date DATE, IN target_tenant_id VARCHAR(100))
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_tenant_id VARCHAR(100);
    DECLARE v_tag_id BIGINT;
    
    DECLARE tag_cursor CURSOR FOR
        SELECT DISTINCT ft.tenant_id, ft.id
        FROM file_tags ft
        WHERE (target_tenant_id IS NULL OR ft.tenant_id = target_tenant_id);
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    OPEN tag_cursor;
    
    read_loop: LOOP
        FETCH tag_cursor INTO v_tenant_id, v_tag_id;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        INSERT INTO tag_usage_statistics (
            tenant_id, tag_id, stat_date,
            files_tagged, files_untagged, unique_users,
            auto_tagged_files, manual_tagged_files
        )
        SELECT 
            v_tenant_id,
            v_tag_id,
            target_date,
            COUNT(CASE WHEN ftt.tagged_at IS NOT NULL AND DATE(ftt.tagged_at) = target_date THEN 1 END) as files_tagged,
            0 as files_untagged, -- Would need separate tracking for untagging
            COUNT(DISTINCT ftt.tagged_by) as unique_users,
            COUNT(CASE WHEN ftt.auto_tagged = TRUE AND DATE(ftt.tagged_at) = target_date THEN 1 END) as auto_tagged_files,
            COUNT(CASE WHEN ftt.auto_tagged = FALSE AND DATE(ftt.tagged_at) = target_date THEN 1 END) as manual_tagged_files
        FROM file_transfer_tags ftt
        WHERE ftt.file_tag_id = v_tag_id
        AND DATE(ftt.tagged_at) = target_date
        ON DUPLICATE KEY UPDATE
            files_tagged = VALUES(files_tagged),
            unique_users = VALUES(unique_users),
            auto_tagged_files = VALUES(auto_tagged_files),
            manual_tagged_files = VALUES(manual_tagged_files),
            updated_at = CURRENT_TIMESTAMP;
            
    END LOOP;
    
    CLOSE tag_cursor;
END//
DELIMITER ;

-- Create triggers to maintain tag counts
DELIMITER //
CREATE TRIGGER tr_file_transfer_tags_insert
    AFTER INSERT ON file_transfer_tags
    FOR EACH ROW
BEGIN
    -- Update tag usage count
    UPDATE file_tags 
    SET usage_count = usage_count + 1 
    WHERE id = NEW.file_tag_id;
    
    -- Update file transfer tag count and last tagged info
    UPDATE file_transfer_records 
    SET tag_count = tag_count + 1,
        last_tagged_at = NEW.tagged_at,
        last_tagged_by = NEW.tagged_by,
        auto_tagged = CASE 
            WHEN tag_count = 0 THEN NEW.auto_tagged 
            ELSE auto_tagged OR NEW.auto_tagged 
        END
    WHERE id = NEW.file_transfer_id;
END//

CREATE TRIGGER tr_file_transfer_tags_delete
    AFTER DELETE ON file_transfer_tags
    FOR EACH ROW
BEGIN
    -- Update tag usage count
    UPDATE file_tags 
    SET usage_count = GREATEST(0, usage_count - 1) 
    WHERE id = OLD.file_tag_id;
    
    -- Update file transfer tag count
    UPDATE file_transfer_records 
    SET tag_count = GREATEST(0, tag_count - 1)
    WHERE id = OLD.file_transfer_id;
    
    -- Update last_tagged info if this was the last tag
    UPDATE file_transfer_records ftr
    SET last_tagged_at = (
        SELECT MAX(ftt.tagged_at) 
        FROM file_transfer_tags ftt 
        WHERE ftt.file_transfer_id = ftr.id
    ),
    last_tagged_by = (
        SELECT ftt.tagged_by 
        FROM file_transfer_tags ftt 
        WHERE ftt.file_transfer_id = ftr.id 
        ORDER BY ftt.tagged_at DESC 
        LIMIT 1
    )
    WHERE id = OLD.file_transfer_id;
END//
DELIMITER ;

-- Create event to update tag statistics daily
CREATE EVENT evt_update_tag_usage_statistics
ON SCHEDULE EVERY 1 DAY
STARTS CURRENT_DATE + INTERVAL 1 DAY + INTERVAL 7 HOUR
DO
    CALL UpdateTagUsageStatistics(CURDATE() - INTERVAL 1 DAY, NULL);

-- Create indexes for better performance
CREATE INDEX idx_file_tags_color ON file_tags(tenant_id, tag_color);
CREATE INDEX idx_file_transfer_tags_reason ON file_transfer_tags(tag_reason);

-- Add comments
ALTER TABLE file_tags COMMENT = 'Custom tags for organizing and categorizing file transfers';
ALTER TABLE file_transfer_tags COMMENT = 'Many-to-many relationship between file transfers and tags';
ALTER TABLE tag_usage_statistics COMMENT = 'Daily statistics for tag usage and trends';

-- Add column comments
ALTER TABLE file_transfer_records 
MODIFY COLUMN tag_count INT DEFAULT 0 COMMENT 'Number of tags applied to this file transfer',
MODIFY COLUMN auto_tagged BOOLEAN DEFAULT FALSE COMMENT 'Whether this file has any auto-applied tags',
MODIFY COLUMN last_tagged_at TIMESTAMP NULL COMMENT 'Timestamp of the most recent tagging action',
MODIFY COLUMN last_tagged_by VARCHAR(100) COMMENT 'User who most recently tagged this file';

-- Create procedure for tag cleanup
DELIMITER //
CREATE PROCEDURE CleanupUnusedTags(IN target_tenant_id VARCHAR(100))
BEGIN
    DELETE FROM file_tags 
    WHERE tenant_id = target_tenant_id 
    AND usage_count = 0 
    AND is_system_tag = FALSE
    AND created_at < DATE_SUB(NOW(), INTERVAL 30 DAY);
    
    SELECT ROW_COUNT() as deleted_tags;
END//
DELIMITER ;

-- Create procedure for bulk tagging
DELIMITER //
CREATE PROCEDURE BulkTagFiles(
    IN p_tenant_id VARCHAR(100),
    IN p_service_name VARCHAR(100),
    IN p_tag_name VARCHAR(100),
    IN p_tagged_by VARCHAR(100),
    IN p_reason VARCHAR(500)
)
BEGIN
    DECLARE v_tag_id BIGINT;
    DECLARE files_tagged INT DEFAULT 0;
    
    -- Get tag ID
    SELECT id INTO v_tag_id 
    FROM file_tags 
    WHERE tenant_id = p_tenant_id AND tag_name = p_tag_name;
    
    IF v_tag_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Tag not found';
    END IF;
    
    -- Insert tag relationships for files that don't already have this tag
    INSERT INTO file_transfer_tags (file_transfer_id, file_tag_id, tagged_by, tag_reason, auto_tagged)
    SELECT 
        ftr.id,
        v_tag_id,
        p_tagged_by,
        p_reason,
        TRUE
    FROM file_transfer_records ftr
    LEFT JOIN file_transfer_tags existing ON ftr.id = existing.file_transfer_id AND existing.file_tag_id = v_tag_id
    WHERE ftr.tenant_id = p_tenant_id 
    AND ftr.service_name = p_service_name
    AND existing.id IS NULL;
    
    SET files_tagged = ROW_COUNT();
    
    SELECT files_tagged as tagged_count;
END//
DELIMITER ;

-- Sample data for demonstration
INSERT INTO file_tags (tenant_id, tag_name, tag_color, tag_description, is_system_tag, created_by) VALUES
('demo', 'urgent', '#d32f2f', 'Urgent files requiring immediate processing', FALSE, 'admin'),
('demo', 'validated', '#388e3c', 'Files that have passed validation checks', FALSE, 'admin'),
('demo', 'test-data', '#1976d2', 'Test or sample data files', FALSE, 'admin'),
('demo', 'production', '#7b1fa2', 'Production data files', FALSE, 'admin'),
('demo', 'encrypted', '#455a64', 'Encrypted files requiring special handling', FALSE, 'admin');

-- Create materialized view for tag analytics
CREATE TABLE tag_analytics_cache AS
SELECT 
    ft.tenant_id,
    ft.id as tag_id,
    ft.tag_name,
    ft.tag_color,
    ft.usage_count,
    COUNT(ftt.id) as current_usage,
    COUNT(DISTINCT ftt.tagged_by) as unique_users,
    COUNT(CASE WHEN ftt.auto_tagged = TRUE THEN 1 END) as auto_tagged_count,
    COUNT(CASE WHEN ftt.auto_tagged = FALSE THEN 1 END) as manual_tagged_count,
    MAX(ftt.tagged_at) as last_used_at,
    DATE(NOW()) as cache_date
FROM file_tags ft
LEFT JOIN file_transfer_tags ftt ON ft.id = ftt.file_tag_id
GROUP BY ft.id;

-- Add indexes to cache table
ALTER TABLE tag_analytics_cache 
ADD COLUMN id BIGINT AUTO_INCREMENT PRIMARY KEY FIRST,
ADD INDEX idx_tag_analytics_tenant (tenant_id),
ADD INDEX idx_tag_analytics_usage (tenant_id, current_usage DESC),
ADD INDEX idx_tag_analytics_date (cache_date);

-- Create procedure to refresh tag analytics cache
DELIMITER //
CREATE PROCEDURE RefreshTagAnalyticsCache()
BEGIN
    TRUNCATE TABLE tag_analytics_cache;
    
    INSERT INTO tag_analytics_cache (
        tenant_id, tag_id, tag_name, tag_color, usage_count,
        current_usage, unique_users, auto_tagged_count, manual_tagged_count,
        last_used_at, cache_date
    )
    SELECT 
        ft.tenant_id,
        ft.id,
        ft.tag_name,
        ft.tag_color,
        ft.usage_count,
        COUNT(ftt.id) as current_usage,
        COUNT(DISTINCT ftt.tagged_by) as unique_users,
        COUNT(CASE WHEN ftt.auto_tagged = TRUE THEN 1 END) as auto_tagged_count,
        COUNT(CASE WHEN ftt.auto_tagged = FALSE THEN 1 END) as manual_tagged_count,
        MAX(ftt.tagged_at) as last_used_at,
        DATE(NOW()) as cache_date
    FROM file_tags ft
    LEFT JOIN file_transfer_tags ftt ON ft.id = ftt.file_tag_id
    GROUP BY ft.id;
END//
DELIMITER ;

-- Create event to refresh tag analytics cache every 4 hours
CREATE EVENT evt_refresh_tag_analytics_cache
ON SCHEDULE EVERY 4 HOUR
DO
    CALL RefreshTagAnalyticsCache();

-- Create additional indexes for performance
CREATE INDEX idx_file_transfer_tags_tenant_tag ON file_transfer_tags(file_tag_id, tagged_at);
CREATE INDEX idx_file_tags_tenant_usage ON file_tags(tenant_id, usage_count DESC, tag_name);

-- Create procedure for tag recommendations based on file characteristics
DELIMITER //
CREATE PROCEDURE GetTagRecommendations(IN p_file_transfer_id BIGINT)
BEGIN
    DECLARE v_tenant_id VARCHAR(100);
    DECLARE v_service_name VARCHAR(100);
    DECLARE v_file_size BIGINT;
    DECLARE v_file_extension VARCHAR(20);
    DECLARE v_status VARCHAR(20);
    
    -- Get file transfer details
    SELECT tenant_id, service_name, file_size, file_extension, status
    INTO v_tenant_id, v_service_name, v_file_size, v_file_extension, v_status
    FROM file_transfer_records 
    WHERE id = p_file_transfer_id;
    
    -- Return recommended tags based on characteristics
    SELECT DISTINCT ft.id, ft.tag_name, ft.tag_color, 
           CASE 
               WHEN ft.tag_name = 'large-file' AND v_file_size > 104857600 THEN 'Large file detected'
               WHEN ft.tag_name = 'sensitive-data' AND v_service_name LIKE '%CUSTOMER%' THEN 'Customer data detected'
               WHEN ft.tag_name = 'processed' AND v_status = 'COMPLETED' THEN 'File processed successfully'
               WHEN ft.tag_name = 'needs-attention' AND v_status = 'FAILED' THEN 'Processing failed'
               ELSE 'Recommended based on file characteristics'
           END as recommendation_reason,
           CASE 
               WHEN ft.tag_name = 'large-file' AND v_file_size > 104857600 THEN 90
               WHEN ft.tag_name = 'sensitive-data' AND v_service_name LIKE '%CUSTOMER%' THEN 85
               WHEN ft.tag_name = 'processed' AND v_status = 'COMPLETED' THEN 95
               WHEN ft.tag_name = 'needs-attention' AND v_status = 'FAILED' THEN 95
               ELSE 50
           END as recommendation_confidence
    FROM file_tags ft
    WHERE ft.tenant_id = v_tenant_id
    AND ft.is_system_tag = TRUE
    AND (
        (ft.tag_name = 'large-file' AND v_file_size > 104857600) OR
        (ft.tag_name = 'sensitive-data' AND v_service_name LIKE '%CUSTOMER%') OR
        (ft.tag_name = 'processed' AND v_status = 'COMPLETED') OR
        (ft.tag_name = 'needs-attention' AND v_status = 'FAILED') OR
        (ft.tag_name = 'log-data' AND v_file_extension = '.log') OR
        (ft.tag_name = 'backup-data' AND v_file_extension IN ('.zip', '.gz', '.tar'))
    )
    AND NOT EXISTS (
        SELECT 1 FROM file_transfer_tags ftt 
        WHERE ftt.file_transfer_id = p_file_transfer_id 
        AND ftt.file_tag_id = ft.id
    )
    ORDER BY recommendation_confidence DESC;
END//
DELIMITER ;