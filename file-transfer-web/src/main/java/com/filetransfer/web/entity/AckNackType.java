package com.filetransfer.web.entity;

/**
 * Type of acknowledgment file
 */
public enum AckNackType {
    ACK,    // Positive acknowledgment - file received and processed successfully
    NACK    // Negative acknowledgment - file received but processing failed
}