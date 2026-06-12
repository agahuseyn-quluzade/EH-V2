package com.ehi.infra.event;

import com.ehi.infra.enums.NotificationChannel;
import com.ehi.infra.enums.NotificationType;
import lombok.Builder;

import java.util.UUID;

@Builder
public record NotificationEvent(
        UUID userId,
        NotificationChannel channel,
        NotificationType type,
        String recipient,
        String subject,
        String body
) {
}
