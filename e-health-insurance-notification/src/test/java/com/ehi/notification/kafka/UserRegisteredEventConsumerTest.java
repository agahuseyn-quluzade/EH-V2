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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRegisteredEventConsumerTest {

    @Mock NotificationService notificationService;
    @Mock UserContactRepository userContactRepository;

    @InjectMocks UserRegisteredEventConsumer consumer;

    @Test
    void consume_savesContactAndSendsWelcomeEmail() {
        UUID userId = UUID.randomUUID();
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .userId(userId)
                .email("user@example.com")
                .firstName("John")
                .lastName("Doe")
                .phone("+994501234567")
                .build();

        when(userContactRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userContactRepository.save(any(UserContact.class))).thenAnswer(i -> i.getArgument(0));

        consumer.consume(event);

        verify(userContactRepository).save(any(UserContact.class));
        verify(notificationService).send(userId, userId, NotificationType.WELCOME, NotificationChannel.EMAIL,
                "user@example.com", "Welcome to E-Health Insurance", "Hi John Doe, welcome to E-Health Insurance!");
    }
}
