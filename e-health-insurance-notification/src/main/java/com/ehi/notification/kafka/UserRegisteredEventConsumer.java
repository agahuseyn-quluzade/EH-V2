package com.ehi.notification.kafka;

import com.ehi.notification.entity.UserContact;
import com.ehi.notification.repository.UserContactRepository;
import com.ehi.notification.service.NotificationService;
import com.ehi.infra.config.KafkaTopics;
import com.ehi.infra.enums.NotificationChannel;
import com.ehi.infra.enums.NotificationType;
import com.ehi.infra.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserRegisteredEventConsumer {

    private final NotificationService notificationService;
    private final UserContactRepository userContactRepository;

    @KafkaListener(topics = KafkaTopics.USER_REGISTERED, groupId = "notification-service")
    public void consume(UserRegisteredEvent event) {
        log.info("Received UserRegisteredEvent for userId={}", event.userId());

        userContactRepository.findByUserId(event.userId()).ifPresentOrElse(
                existing -> {
                    existing.setEmail(event.email());
                    existing.setPhone(event.phone());
                    userContactRepository.save(existing);
                },
                () -> userContactRepository.save(UserContact.builder()
                        .userId(event.userId())
                        .email(event.email())
                        .phone(event.phone())
                        .build()));

        String subject = "Welcome to E-Health Insurance";
        String body = "Hi " + event.firstName() + " " + event.lastName() + ", welcome to E-Health Insurance!";

        notificationService.send(event.userId(), event.userId(), NotificationType.WELCOME,
                NotificationChannel.EMAIL, event.email(), subject, body);

        if (event.phone() != null) {
            notificationService.send(event.userId(), event.userId(), NotificationType.WELCOME,
                    NotificationChannel.SMS, event.phone(), subject, body);
        }
    }
}
