package com.ehi.notification.service.impl;

import com.ehi.notification.entity.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationSender {

    public boolean send(Notification notification) {
        log.info("Sending {} notification via {} to {} | subject='{}' | body='{}'",
                notification.getType(), notification.getChannel(), notification.getRecipient(),
                notification.getSubject(), notification.getBody());
        return true;
    }
}
