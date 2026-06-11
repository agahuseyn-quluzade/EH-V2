package com.eHealthInsurance.outbox;

public enum OutboxEventStatus {
    PENDING,
    FAILED,
    SENT,
    DLQ_PENDING,
    DLQ_SENT
}
