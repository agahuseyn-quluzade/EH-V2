package com.ehi.notification.scheduler;

import com.ehi.notification.entity.Notification;
import com.ehi.notification.enums.NotificationStatus;
import com.ehi.notification.repository.NotificationRepository;
import com.ehi.notification.service.NotificationSender;
import com.ehi.infra.enums.NotificationChannel;
import com.ehi.infra.enums.NotificationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetrySchedulerTest {

    @Mock NotificationRepository notificationRepository;
    @Mock NotificationSender notificationSender;

    @InjectMocks RetryScheduler retryScheduler;

    private Notification failedNotification(int retryCount) {
        return Notification.builder()
                .id(UUID.randomUUID()).userId(UUID.randomUUID())
                .type(NotificationType.WELCOME).channel(NotificationChannel.EMAIL)
                .recipient("user@example.com").subject("Welcome").body("Hi")
                .status(NotificationStatus.FAILED).retryCount(retryCount)
                .build();
    }

    @Test
    void retryFailedNotifications_onlyQueriesFailedWithRetryCountBelowMax() {
        when(notificationRepository.findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, 3)).thenReturn(List.of());

        retryScheduler.retryFailedNotifications();

        verify(notificationRepository).findByStatusAndRetryCountLessThan(eq(NotificationStatus.FAILED), eq(3));
    }

    @Test
    void retryFailedNotifications_marksSent_andIncrementsRetryCount_whenSenderSucceeds() {
        Notification notification = failedNotification(1);
        when(notificationRepository.findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, 3)).thenReturn(List.of(notification));
        when(notificationSender.send(any(Notification.class))).thenReturn(true);

        retryScheduler.retryFailedNotifications();

        assertThat(notification.getRetryCount()).isEqualTo(2);
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        verify(notificationRepository).save(notification);
    }

    @Test
    void retryFailedNotifications_marksFailed_andIncrementsRetryCount_whenSenderFails() {
        Notification notification = failedNotification(0);
        when(notificationRepository.findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, 3)).thenReturn(List.of(notification));
        when(notificationSender.send(any(Notification.class))).thenReturn(false);

        retryScheduler.retryFailedNotifications();

        assertThat(notification.getRetryCount()).isEqualTo(1);
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
        verify(notificationRepository).save(notification);
    }
}
