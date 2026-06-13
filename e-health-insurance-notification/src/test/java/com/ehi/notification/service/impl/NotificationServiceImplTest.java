package com.ehi.notification.service.impl;

import com.ehi.notification.dto.response.NotificationDto;
import com.ehi.notification.entity.Notification;
import com.ehi.notification.enums.NotificationStatus;
import com.ehi.notification.mapper.NotificationMapper;
import com.ehi.notification.repository.NotificationRepository;
import com.ehi.notification.service.NotificationSender;
import com.ehi.infra.enums.NotificationChannel;
import com.ehi.infra.enums.NotificationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock NotificationRepository notificationRepository;
    @Mock NotificationMapper notificationMapper;
    @Mock NotificationSender notificationSender;

    @InjectMocks NotificationServiceImpl notificationService;

    private NotificationDto toDtoStub(Notification notification) {
        return NotificationDto.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .type(notification.getType())
                .channel(notification.getChannel())
                .recipient(notification.getRecipient())
                .subject(notification.getSubject())
                .body(notification.getBody())
                .status(notification.getStatus())
                .retryCount(notification.getRetryCount())
                .build();
    }

    // ---- send ----

    @Test
    void send_savesPendingThenSent_whenSenderSucceeds() {
        List<NotificationStatus> savedStatuses = new ArrayList<>();
        when(notificationRepository.existsByCorrelationIdAndType(any(), any())).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            savedStatuses.add(n.getStatus());
            return n;
        });
        when(notificationSender.send(any(Notification.class))).thenReturn(true);
        when(notificationMapper.toDto(any(Notification.class))).thenAnswer(invocation -> toDtoStub(invocation.getArgument(0)));

        UUID userId = UUID.randomUUID();

        NotificationDto result = notificationService.send(UUID.randomUUID(), userId, NotificationType.WELCOME, NotificationChannel.EMAIL,
                "user@example.com", "Welcome", "Hi there");

        verify(notificationRepository, times(2)).save(any(Notification.class));
        assertThat(savedStatuses).containsExactly(NotificationStatus.PENDING, NotificationStatus.SENT);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationSender).send(captor.capture());
        Notification sentNotification = captor.getValue();
        assertThat(sentNotification.getUserId()).isEqualTo(userId);
        assertThat(sentNotification.getType()).isEqualTo(NotificationType.WELCOME);
        assertThat(sentNotification.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(sentNotification.getRecipient()).isEqualTo("user@example.com");
        assertThat(sentNotification.getSubject()).isEqualTo("Welcome");
        assertThat(sentNotification.getBody()).isEqualTo("Hi there");
        assertThat(sentNotification.getRetryCount()).isZero();

        assertThat(result.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(result.userId()).isEqualTo(userId);
    }

    @Test
    void send_savesFailed_whenSenderFails() {
        List<NotificationStatus> savedStatuses = new ArrayList<>();
        when(notificationRepository.existsByCorrelationIdAndType(any(), any())).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            savedStatuses.add(n.getStatus());
            return n;
        });
        when(notificationSender.send(any(Notification.class))).thenReturn(false);
        when(notificationMapper.toDto(any(Notification.class))).thenAnswer(invocation -> toDtoStub(invocation.getArgument(0)));

        NotificationDto result = notificationService.send(UUID.randomUUID(), UUID.randomUUID(), NotificationType.CLAIM_SUBMITTED, NotificationChannel.EMAIL,
                "user@example.com", "Claim Submitted", "Your claim was submitted");

        assertThat(savedStatuses).containsExactly(NotificationStatus.PENDING, NotificationStatus.FAILED);
        assertThat(result.status()).isEqualTo(NotificationStatus.FAILED);
    }

    @Test
    void send_skipsDuplicate_whenCorrelationIdAndTypeAlreadyExists() {
        UUID correlationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Notification existing = Notification.builder()
                .id(UUID.randomUUID()).correlationId(correlationId).userId(userId)
                .type(NotificationType.CLAIM_APPROVED).channel(NotificationChannel.EMAIL)
                .recipient("user@example.com").subject("Claim Approved").body("Approved")
                .status(NotificationStatus.SENT).retryCount(0).build();

        when(notificationRepository.existsByCorrelationIdAndType(correlationId, NotificationType.CLAIM_APPROVED)).thenReturn(true);
        when(notificationRepository.findByCorrelationIdAndType(correlationId, NotificationType.CLAIM_APPROVED)).thenReturn(existing);
        when(notificationMapper.toDto(existing)).thenReturn(toDtoStub(existing));

        NotificationDto result = notificationService.send(correlationId, userId, NotificationType.CLAIM_APPROVED,
                NotificationChannel.EMAIL, "user@example.com", "Claim Approved", "Approved");

        verify(notificationRepository, org.mockito.Mockito.never()).save(any());
        verify(notificationSender, org.mockito.Mockito.never()).send(any());
        assertThat(result.status()).isEqualTo(NotificationStatus.SENT);
    }

    // ---- getMyNotifications ----

    @Test
    void getMyNotifications_returnsNotifications_scopedByUserId() {
        UUID userId = UUID.randomUUID();
        Notification notification = Notification.builder().id(UUID.randomUUID()).userId(userId)
                .type(NotificationType.WELCOME).channel(NotificationChannel.EMAIL)
                .recipient("user@example.com").subject("Welcome").body("Hi")
                .status(NotificationStatus.SENT).retryCount(0).build();

        when(notificationRepository.findByUserId(userId)).thenReturn(List.of(notification));
        when(notificationMapper.toDto(notification)).thenReturn(toDtoStub(notification));

        List<NotificationDto> result = notificationService.getMyNotifications(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).userId()).isEqualTo(userId);
    }

    // ---- getAllNotifications ----

    @Test
    void getAllNotifications_returnsPagedResponse() {
        Notification notification = Notification.builder().id(UUID.randomUUID()).userId(UUID.randomUUID())
                .type(NotificationType.CLAIM_APPROVED).channel(NotificationChannel.EMAIL)
                .recipient("user@example.com").subject("Claim Approved").body("Approved")
                .status(NotificationStatus.SENT).retryCount(0).build();
        Pageable pageable = PageRequest.of(0, 20);
        Page<Notification> page = new PageImpl<>(List.of(notification), pageable, 1);

        when(notificationRepository.findAll(pageable)).thenReturn(page);
        when(notificationMapper.toDto(notification)).thenReturn(toDtoStub(notification));

        var result = notificationService.getAllNotifications(pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.last()).isTrue();
    }
}
