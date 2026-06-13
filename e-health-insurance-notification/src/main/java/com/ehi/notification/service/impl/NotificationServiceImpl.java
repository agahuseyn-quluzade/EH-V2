package com.ehi.notification.service.impl;

import com.ehi.notification.dto.response.NotificationDto;
import com.ehi.notification.entity.Notification;
import com.ehi.notification.enums.NotificationStatus;
import com.ehi.notification.mapper.NotificationMapper;
import com.ehi.notification.repository.NotificationRepository;
import com.ehi.notification.service.NotificationSender;
import com.ehi.notification.service.NotificationService;
import com.ehi.infra.dto.PagedResponse;
import com.ehi.infra.enums.NotificationChannel;
import com.ehi.infra.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final NotificationSender notificationSender;

    @Override
    public NotificationDto send(UUID correlationId, UUID userId, NotificationType type, NotificationChannel channel, String recipient, String subject, String body) {
        if (notificationRepository.existsByCorrelationIdAndTypeAndChannel(correlationId, type, channel)) {
            log.warn("Duplicate notification skipped: correlationId={}, type={}, channel={}", correlationId, type, channel);
            return notificationMapper.toDto(
                    notificationRepository.findByCorrelationIdAndTypeAndChannel(correlationId, type, channel));
        }

        Notification notification = Notification.builder()
                .correlationId(correlationId)
                .userId(userId)
                .type(type)
                .channel(channel)
                .recipient(recipient)
                .subject(subject)
                .body(body)
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .build();

        notification = notificationRepository.save(notification);

        boolean sent = notificationSender.send(notification);
        notification.setStatus(sent ? NotificationStatus.SENT : NotificationStatus.FAILED);
        if (!sent) {
            log.warn("Notification FAILED: type={}, userId={}, recipient={}", type, userId, recipient);
        }
        notification = notificationRepository.save(notification);

        return notificationMapper.toDto(notification);
    }

    @Override
    public List<NotificationDto> getMyNotifications(UUID userId) {
        Map<String, Notification> deduped = new LinkedHashMap<>();
        for (Notification notification : notificationRepository.findByUserId(userId)) {
            String key = notification.getCorrelationId() != null
                    ? notification.getCorrelationId() + ":" + notification.getType()
                    : notification.getId().toString();
            deduped.merge(key, notification, (existing, current) ->
                    existing.getChannel() == NotificationChannel.EMAIL ? existing : current);
        }
        return deduped.values().stream()
                .map(notificationMapper::toDto)
                .toList();
    }

    @Override
    public PagedResponse<NotificationDto> getAllNotifications(Pageable pageable) {
        Page<Notification> page = notificationRepository.findAll(pageable);

        return PagedResponse.<NotificationDto>builder()
                .content(page.getContent().stream().map(notificationMapper::toDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
