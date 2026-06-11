package com.eHealthInsurance.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("ehi.security.gateway-header")
public class GatewayHeaderSecurityProperties {

    private boolean enabled = true;
    private String secret;
    private long maxSkewSeconds = 60;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getMaxSkewSeconds() {
        return maxSkewSeconds;
    }

    public void setMaxSkewSeconds(long maxSkewSeconds) {
        this.maxSkewSeconds = maxSkewSeconds;
    }
}
