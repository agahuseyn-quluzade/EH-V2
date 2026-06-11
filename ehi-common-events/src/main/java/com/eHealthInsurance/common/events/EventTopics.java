package com.eHealthInsurance.common.events;

import java.util.List;

public final class EventTopics {
    public static final String PAYMENT_INITIATED = "payment.initiated";
    public static final String PAYMENT_SUCCEEDED = "payment.succeeded";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String PAYMENT_REFUNDED = "payment.refunded";
    public static final String POLICY_PURCHASE_REQUESTED = "policy.purchase.requested";
    public static final String POLICY_ACTIVATED = "policy.activated";
    public static final String POLICY_CANCELLED = "policy.cancelled";
    public static final String CLAIM_SUBMITTED = "claim.submitted";
    public static final String CLAIM_AI_SCORED = "claim.ai.scored";
    public static final String CLAIM_REVIEW_REQUIRED = "claim.review.required";
    public static final String CLAIM_APPROVED = "claim.approved";
    public static final String CLAIM_REJECTED = "claim.rejected";
    public static final String NOTIFICATION_EMAIL_REQUESTED = "notification.email.requested";
    public static final String HEALTH_RECORD_CREATED = "health-record.created";
    public static final String HEALTH_RECORD_UPDATED = "health-record.updated";
    public static final String AUDIT_EVENT_CREATED = "audit.event.created";

    private static final List<String> ALL = List.of(
            PAYMENT_INITIATED,
            PAYMENT_SUCCEEDED,
            PAYMENT_FAILED,
            PAYMENT_REFUNDED,
            POLICY_PURCHASE_REQUESTED,
            POLICY_ACTIVATED,
            POLICY_CANCELLED,
            CLAIM_SUBMITTED,
            CLAIM_AI_SCORED,
            CLAIM_REVIEW_REQUIRED,
            CLAIM_APPROVED,
            CLAIM_REJECTED,
            NOTIFICATION_EMAIL_REQUESTED,
            HEALTH_RECORD_CREATED,
            HEALTH_RECORD_UPDATED,
            AUDIT_EVENT_CREATED
    );

    private EventTopics() {
    }

    public static List<String> all() {
        return ALL;
    }
}
