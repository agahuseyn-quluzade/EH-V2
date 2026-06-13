package com.ehi.notification.service.impl;

import com.ehi.infra.enums.NotificationChannel;
import com.ehi.infra.enums.NotificationType;
import com.ehi.notification.entity.Notification;
import com.ehi.notification.enums.NotificationStatus;
import com.ehi.notification.service.EmailProvider;
import com.ehi.notification.service.SmsProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ChannelNotificationSenderTest {

    @Mock SmsProvider smsProvider;
    @Mock EmailProvider emailProvider;

    @InjectMocks ChannelNotificationSender channelNotificationSender;

    private Notification notification(NotificationChannel channel, String recipient) {
        return Notification.builder()
                .id(UUID.randomUUID()).userId(UUID.randomUUID())
                .type(NotificationType.WELCOME).channel(channel)
                .recipient(recipient).subject("Welcome").body("Hi there")
                .status(NotificationStatus.PENDING).retryCount(0).build();
    }

    @Test
    void send_callsSmsProvider_whenChannelIsSms() {
        Notification n = notification(NotificationChannel.SMS, "+994501234567");

        boolean result = channelNotificationSender.send(n);

        assertThat(result).isTrue();
        verify(smsProvider).send("+994501234567", "Hi there");
        verifyNoInteractions(emailProvider);
    }

    @Test
    void send_callsEmailProvider_whenChannelIsEmail() {
        Notification n = notification(NotificationChannel.EMAIL, "user@example.com");

        boolean result = channelNotificationSender.send(n);

        assertThat(result).isTrue();
        verify(emailProvider).send("user@example.com", "Welcome", "Hi there");
        verifyNoInteractions(smsProvider);
    }

    @Test
    void send_returnsFalse_whenProviderThrows() {
        Notification n = notification(NotificationChannel.EMAIL, "user@example.com");
        doThrow(new RuntimeException("SMTP down")).when(emailProvider).send("user@example.com", "Welcome", "Hi there");

        boolean result = channelNotificationSender.send(n);

        assertThat(result).isFalse();
    }
}
