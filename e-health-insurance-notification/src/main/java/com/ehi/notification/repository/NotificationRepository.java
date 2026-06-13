package com.ehi.notification.repository;

import com.ehi.notification.entity.Notification;
import com.ehi.notification.enums.NotificationStatus;
import com.ehi.infra.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserId(UUID userId);

    List<Notification> findByStatusAndRetryCountLessThan(NotificationStatus status, int retryCount);

    boolean existsByCorrelationIdAndType(UUID correlationId, NotificationType type);

    Notification findByCorrelationIdAndType(UUID correlationId, NotificationType type);
}
