package com.ehi.notification.kafka;

import com.ehi.notification.entity.UserContact;
import com.ehi.notification.repository.UserContactRepository;
import com.ehi.notification.service.NotificationService;
import com.ehi.infra.enums.NotificationChannel;
import com.ehi.infra.enums.NotificationType;
import com.ehi.infra.event.UserRegisteredEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRegisteredEventConsumerTest {

    @Mock NotificationService notificationService;
    @Mock UserContactRepository userContactRepository;

    @InjectMocks UserRegisteredEventConsumer consumer;

    @Test
    void consume_sendsEmailOnly_whenNoPhone() {
        UUID userId = UUID.randomUUID();
        UserRegisteredEvent event = new UserRegisteredEvent(userId, "user@example.com", "John", "Doe", null);
        when(userContactRepository.findByUserId(userId)).thenReturn(Optional.empty());

        consumer.consume(event);

        verify(notificationService).send(userId, userId, NotificationType.WELCOME, NotificationChannel.EMAIL,
                "user@example.com", "Welcome to E-Health Insurance", "Hi John Doe, welcome to E-Health Insurance!");
        verify(notificationService, times(1)).send(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void consume_sendsEmailAndSms_whenPhonePresent() {
        UUID userId = UUID.randomUUID();
        UserRegisteredEvent event = new UserRegisteredEvent(userId, "user@example.com", "John", "Doe", "+994501234567");
        when(userContactRepository.findByUserId(userId)).thenReturn(Optional.empty());

        consumer.consume(event);

        verify(notificationService).send(userId, userId, NotificationType.WELCOME, NotificationChannel.EMAIL,
                "user@example.com", "Welcome to E-Health Insurance", "Hi John Doe, welcome to E-Health Insurance!");
        verify(notificationService).send(userId, userId, NotificationType.WELCOME, NotificationChannel.SMS,
                "+994501234567", "Welcome to E-Health Insurance", "Hi John Doe, welcome to E-Health Insurance!");
        verify(notificationService, times(2)).send(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void consume_upsertExistingContact() {
        UUID userId = UUID.randomUUID();
        UserRegisteredEvent event = new UserRegisteredEvent(userId, "new@example.com", "John", "Doe", null);
        UserContact existing = UserContact.builder().id(UUID.randomUUID()).userId(userId)
                .email("old@example.com").phone(null).build();
        when(userContactRepository.findByUserId(userId)).thenReturn(Optional.of(existing));

        consumer.consume(event);

        verify(userContactRepository).save(existing);
    }
}
