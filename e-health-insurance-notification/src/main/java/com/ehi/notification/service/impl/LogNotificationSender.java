package com.ehi.notification.service.impl;

import com.ehi.notification.entity.Notification;
import com.ehi.notification.service.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "aws", name = "enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
public class LogNotificationSender implements NotificationSender {

    @Override
    public boolean send(Notification notification) {
        log.info("Sending {} notification via {} to {} | subject='{}' | body='{}'",
                notification.getType(), notification.getChannel(), notification.getRecipient(),
                notification.getSubject(), notification.getBody());
        return true;
    }
}
