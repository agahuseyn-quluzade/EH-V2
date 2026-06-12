package com.ehi.infra.config;

public final class KafkaTopics {

    public static final String USER_REGISTERED = "user.registered";
    public static final String POLICY_CREATED = "policy.created";
    public static final String PAYMENT_COMPLETED = "payment.completed";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String CLAIM_SUBMITTED = "claim.submitted";
    public static final String CLAIM_DECISION = "claim.decision";
    public static final String FRAUD_DETECTED = "fraud.detected";
    public static final String NOTIFICATION_SEND = "notification.send";

    private KafkaTopics() {
    }
}
