package com.ehi.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "aws")
@Data
public class AwsProperties {

    private boolean enabled;
    private String region;
    private Ses ses = new Ses();

    @Data
    public static class Ses {
        private String from;
    }
}
