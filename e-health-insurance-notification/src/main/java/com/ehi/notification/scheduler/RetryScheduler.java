package com.ehi.notification.scheduler;

import com.ehi.notification.entity.Notification;
import com.ehi.notification.enums.NotificationStatus;
import com.ehi.notification.repository.NotificationRepository;
import com.ehi.notification.service.impl.NotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RetryScheduler {

    private static final int MAX_RETRIES = 3;

    private final NotificationRepository notificationRepository;
    private final NotificationSender notificationSender;

    @Scheduled(fixedRate = 300000)
    public void retryFailedNotifications() {
        List<Notification> failed = notificationRepository.findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, MAX_RETRIES);

        for (Notification notification : failed) {
            notification.setRetryCount(notification.getRetryCount() + 1);

            boolean sent = notificationSender.send(notification);
            notification.setStatus(sent ? NotificationStatus.SENT : NotificationStatus.FAILED);

            notificationRepository.save(notification);
            log.info("Retried notification {} (attempt {}), result: {}", notification.getId(), notification.getRetryCount(), notification.getStatus());
        }
    }
}
