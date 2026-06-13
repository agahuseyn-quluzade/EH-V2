package com.ehi.notification.service.impl;

import com.ehi.infra.enums.NotificationChannel;
import com.ehi.notification.entity.Notification;
import com.ehi.notification.service.EmailProvider;
import com.ehi.notification.service.NotificationSender;
import com.ehi.notification.service.SmsProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChannelNotificationSender implements NotificationSender {

    private final SmsProvider smsProvider;
    private final EmailProvider emailProvider;

    @Override
    public boolean send(Notification n) {
        try {
            if (n.getChannel() == NotificationChannel.SMS) {
                smsProvider.send(n.getRecipient(), n.getBody());
            } else {
                emailProvider.send(n.getRecipient(), n.getSubject(), n.getBody());
            }
            return true;
        } catch (Exception e) {
            log.warn("Send failed: type={} channel={} recipient={}: {}",
                    n.getType(), n.getChannel(), n.getRecipient(), e.getMessage());
            return false;
        }
    }
}
