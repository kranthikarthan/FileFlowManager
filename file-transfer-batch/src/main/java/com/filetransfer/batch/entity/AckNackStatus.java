package com.filetransfer.batch.entity;

/**
 * Status of ACK/NACK files
 */
public enum AckNackStatus {
    PENDING,        // ACK/NACK file needs to be generated or processed
    GENERATED,      // ACK/NACK file has been generated (for outbound)
    SENT,          // ACK/NACK file has been sent to partner
    RECEIVED,      // ACK/NACK file has been received from partner
    PROCESSED,     // ACK/NACK file has been processed successfully
    FAILED,        // ACK/NACK file processing failed
    EXPIRED        // ACK/NACK file expired (no response within timeout)
}