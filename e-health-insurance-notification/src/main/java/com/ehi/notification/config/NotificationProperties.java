package com.ehi.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "notification")
@Data
public class NotificationProperties {

    private Email email = new Email();

    @Data
    public static class Email {
        private String from;
    }
}
