package com.ehi.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "epoint")
@Data
public class EpointProperties {

    private String baseUrl;
    private String publicKey;
    private String privateKey;
    private String language;
    private String currency;
    private String successRedirectUrl;
    private String errorRedirectUrl;
    private int timeoutSeconds = 15;
}
