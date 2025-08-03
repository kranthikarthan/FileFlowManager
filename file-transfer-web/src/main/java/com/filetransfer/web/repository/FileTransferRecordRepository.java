package com.filetransfer.web.repository;

import com.filetransfer.web.entity.FileTransferRecord;
import com.filetransfer.web.entity.TransferStatus;
import com.filetransfer.web.entity.TransferDirection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FileTransferRecordRepository extends JpaRepository<FileTransferRecord, Long> {
    
    List<FileTransferRecord> findByServiceType(String serviceType);
    
    List<FileTransferRecord> findByStatus(TransferStatus status);
    
    List<FileTransferRecord> findByDirection(TransferDirection direction);
    
    List<FileTransferRecord> findByServiceTypeAndStatus(String serviceType, TransferStatus status);
    
    List<FileTransferRecord> findByServiceTypeAndDirection(String serviceType, TransferDirection direction);
    
    @Query("SELECT f FROM FileTransferRecord f WHERE f.createdAt BETWEEN :startDate AND :endDate")
    List<FileTransferRecord> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT f FROM FileTransferRecord f WHERE f.serviceType = :serviceType AND f.createdAt BETWEEN :startDate AND :endDate")
    List<FileTransferRecord> findByServiceTypeAndDateRange(@Param("serviceType") String serviceType,
                                                         @Param("startDate") LocalDateTime startDate,
                                                         @Param("endDate") LocalDateTime endDate);
    
    List<FileTransferRecord> findByFileName(String fileName);
    
    @Query("SELECT DISTINCT f.serviceType FROM FileTransferRecord f")
    List<String> findDistinctServiceTypes();
}