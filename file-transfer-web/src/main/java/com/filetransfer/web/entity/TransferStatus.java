package com.filetransfer.web.entity;

public enum TransferStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED,
    WAITING_FOR_END_MARKER
}