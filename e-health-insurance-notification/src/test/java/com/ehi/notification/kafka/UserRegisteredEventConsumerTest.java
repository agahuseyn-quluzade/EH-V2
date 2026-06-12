package com.ehi.notification.kafka;

import com.ehi.notification.service.NotificationService;
import com.ehi.infra.enums.NotificationChannel;
import com.ehi.infra.enums.NotificationType;
import com.ehi.infra.event.UserRegisteredEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserRegisteredEventConsumerTest {

    @Mock NotificationService notificationService;

    @InjectMocks UserRegisteredEventConsumer consumer;

    @Test
    void consume_sendsWelcomeNotification_toUserEmail() {
        UUID userId = UUID.randomUUID();
        UserRegisteredEvent event = new UserRegisteredEvent(userId, "user@example.com", "John", "Doe");

        consumer.consume(event);

        verify(notificationService).send(userId, NotificationType.WELCOME, NotificationChannel.EMAIL,
                "user@example.com", "Welcome to E-Health Insurance", "Hi John Doe, welcome to E-Health Insurance!");
    }
}
