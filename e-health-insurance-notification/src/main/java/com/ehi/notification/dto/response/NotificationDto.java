package com.ehi.notification.dto.response;

import com.ehi.notification.enums.NotificationStatus;
import com.ehi.infra.enums.NotificationChannel;
import com.ehi.infra.enums.NotificationType;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record NotificationDto(
        UUID id,
        UUID userId,
        NotificationType type,
        NotificationChannel channel,
        String recipient,
        String subject,
        String body,
        NotificationStatus status,
        int retryCount,
        Instant createdAt
) {
}
