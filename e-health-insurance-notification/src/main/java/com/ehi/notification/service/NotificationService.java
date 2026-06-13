package com.ehi.notification.service;

import com.ehi.notification.dto.response.NotificationDto;
import com.ehi.infra.dto.PagedResponse;
import com.ehi.infra.enums.NotificationChannel;
import com.ehi.infra.enums.NotificationType;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    NotificationDto send(UUID correlationId, UUID userId, NotificationType type, NotificationChannel channel, String recipient, String subject, String body);

    List<NotificationDto> getMyNotifications(UUID userId);

    PagedResponse<NotificationDto> getAllNotifications(Pageable pageable);
}
