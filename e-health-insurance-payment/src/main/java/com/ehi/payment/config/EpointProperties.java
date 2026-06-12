package com.ehi.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "epoint")
@Data
public class EpointProperties {

    private String publicKey;
    private String privateKey;
    private String baseUrl = "https://epoint.az/api/1";
    private String successRedirectUrl;
    private String errorRedirectUrl;
    private String resultUrl;
    private String currency = "AZN";
    private String language = "az";
}
