package com.ehi.notification.service.impl;

import com.ehi.notification.dto.response.NotificationDto;
import com.ehi.notification.entity.Notification;
import com.ehi.notification.enums.NotificationStatus;
import com.ehi.notification.mapper.NotificationMapper;
import com.ehi.notification.repository.NotificationRepository;
import com.ehi.notification.service.NotificationService;
import com.ehi.infra.dto.PagedResponse;
import com.ehi.infra.enums.NotificationChannel;
import com.ehi.infra.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final NotificationSender notificationSender;

    @Override
    public NotificationDto send(UUID userId, NotificationType type, NotificationChannel channel, String recipient, String subject, String body) {
        Notification notification = Notification.builder()
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
        notification = notificationRepository.save(notification);

        return notificationMapper.toDto(notification);
    }

    @Override
    public List<NotificationDto> getMyNotifications(UUID userId) {
        return notificationRepository.findByUserId(userId).stream()
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
