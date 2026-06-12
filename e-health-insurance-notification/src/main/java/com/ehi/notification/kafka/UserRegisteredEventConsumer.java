package com.ehi.notification.kafka;

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

    @KafkaListener(topics = KafkaTopics.USER_REGISTERED, groupId = "notification-service")
    public void consume(UserRegisteredEvent event) {
        log.info("Received UserRegisteredEvent for userId={}", event.userId());

        notificationService.send(
                event.userId(),
                NotificationType.WELCOME,
                NotificationChannel.EMAIL,
                event.email(),
                "Welcome to E-Health Insurance",
                "Hi " + event.firstName() + " " + event.lastName() + ", welcome to E-Health Insurance!");
    }
}
