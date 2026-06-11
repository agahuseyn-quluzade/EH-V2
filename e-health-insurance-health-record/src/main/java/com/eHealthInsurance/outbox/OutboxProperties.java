package com.eHealthInsurance.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "ehi.outbox.publisher")
public class OutboxProperties {
    private boolean enabled = true;
    private int batchSize = 50;
    private long fixedDelayMs = 5000;
    private long sendTimeoutMs = 10000;
    private int maxAttempts = 10;
    private long retryDelayMs = 30000;
    private String dlqSuffix = ".dlq";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public long getFixedDelayMs() { return fixedDelayMs; }
    public void setFixedDelayMs(long fixedDelayMs) { this.fixedDelayMs = fixedDelayMs; }
    public long getSendTimeoutMs() { return sendTimeoutMs; }
    public void setSendTimeoutMs(long sendTimeoutMs) { this.sendTimeoutMs = sendTimeoutMs; }
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    public long getRetryDelayMs() { return retryDelayMs; }
    public void setRetryDelayMs(long retryDelayMs) { this.retryDelayMs = retryDelayMs; }
    public String getDlqSuffix() { return dlqSuffix; }
    public void setDlqSuffix(String dlqSuffix) { this.dlqSuffix = dlqSuffix; }

    public Duration sendTimeout() { return Duration.ofMillis(sendTimeoutMs); }
    public Duration retryDelay() { return Duration.ofMillis(retryDelayMs); }
}
