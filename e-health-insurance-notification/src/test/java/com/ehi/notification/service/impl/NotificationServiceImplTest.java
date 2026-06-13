package com.ehi.notification.service.impl;

import com.ehi.notification.dto.response.NotificationDto;
import com.ehi.notification.entity.Notification;
import com.ehi.notification.enums.NotificationStatus;
import com.ehi.notification.mapper.NotificationMapper;
import com.ehi.notification.repository.NotificationRepository;
import com.ehi.notification.service.NotificationSender;
import com.ehi.infra.dto.PagedResponse;
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

    @Test
    void send_savesPendingThenSent_whenSenderSucceeds() {
        UUID correlationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(notificationRepository.existsByCorrelationIdAndTypeAndChannel(correlationId, NotificationType.WELCOME, NotificationChannel.EMAIL))
                .thenReturn(false);

        List<NotificationStatus> savedStatuses = new ArrayList<>();
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            savedStatuses.add(n.getStatus());
            return n;
        });
        when(notificationSender.send(any(Notification.class))).thenReturn(true);
        when(notificationMapper.toDto(any(Notification.class))).thenAnswer(invocation -> toDtoStub(invocation.getArgument(0)));

        NotificationDto result = notificationService.send(correlationId, userId, NotificationType.WELCOME, NotificationChannel.EMAIL,
                "user@example.com", "Welcome", "Hi there");

        verify(notificationRepository, times(2)).save(any(Notification.class));
        assertThat(savedStatuses).containsExactly(NotificationStatus.PENDING, NotificationStatus.SENT);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationSender).send(captor.capture());
        Notification sentNotification = captor.getValue();
        assertThat(sentNotification.getCorrelationId()).isEqualTo(correlationId);
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
        UUID correlationId = UUID.randomUUID();

        when(notificationRepository.existsByCorrelationIdAndTypeAndChannel(correlationId, NotificationType.CLAIM_SUBMITTED, NotificationChannel.EMAIL))
                .thenReturn(false);

        List<NotificationStatus> savedStatuses = new ArrayList<>();
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            savedStatuses.add(n.getStatus());
            return n;
        });
        when(notificationSender.send(any(Notification.class))).thenReturn(false);
        when(notificationMapper.toDto(any(Notification.class))).thenAnswer(invocation -> toDtoStub(invocation.getArgument(0)));

        NotificationDto result = notificationService.send(correlationId, UUID.randomUUID(), NotificationType.CLAIM_SUBMITTED, NotificationChannel.EMAIL,
                "user@example.com", "Claim Submitted", "Your claim was submitted");

        assertThat(savedStatuses).containsExactly(NotificationStatus.PENDING, NotificationStatus.FAILED);
        assertThat(result.status()).isEqualTo(NotificationStatus.FAILED);
    }

    @Test
    void send_returnsDuplicate_whenAlreadySent() {
        UUID correlationId = UUID.randomUUID();
        Notification existing = Notification.builder()
                .id(UUID.randomUUID()).correlationId(correlationId).userId(UUID.randomUUID())
                .type(NotificationType.WELCOME).channel(NotificationChannel.EMAIL)
                .status(NotificationStatus.SENT).retryCount(0).build();

        when(notificationRepository.existsByCorrelationIdAndTypeAndChannel(correlationId, NotificationType.WELCOME, NotificationChannel.EMAIL))
                .thenReturn(true);
        when(notificationRepository.findByCorrelationIdAndTypeAndChannel(correlationId, NotificationType.WELCOME, NotificationChannel.EMAIL))
                .thenReturn(existing);
        when(notificationMapper.toDto(existing)).thenReturn(toDtoStub(existing));

        NotificationDto result = notificationService.send(correlationId, UUID.randomUUID(), NotificationType.WELCOME, NotificationChannel.EMAIL,
                "user@example.com", "Welcome", "Hi there");

        assertThat(result.status()).isEqualTo(NotificationStatus.SENT);
        verify(notificationRepository, times(0)).save(any());
    }

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

    @Test
    void getMyNotifications_collapsesEmailAndSmsRows_fromSameEvent() {
        UUID userId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        Notification emailNotification = Notification.builder().id(UUID.randomUUID()).userId(userId)
                .correlationId(correlationId).type(NotificationType.WELCOME).channel(NotificationChannel.EMAIL)
                .recipient("user@example.com").subject("Welcome").body("Hi")
                .status(NotificationStatus.SENT).retryCount(0).build();
        Notification smsNotification = Notification.builder().id(UUID.randomUUID()).userId(userId)
                .correlationId(correlationId).type(NotificationType.WELCOME).channel(NotificationChannel.SMS)
                .recipient("+994501234567").subject("Welcome").body("Hi")
                .status(NotificationStatus.SENT).retryCount(0).build();

        when(notificationRepository.findByUserId(userId)).thenReturn(List.of(emailNotification, smsNotification));
        when(notificationMapper.toDto(emailNotification)).thenReturn(toDtoStub(emailNotification));

        List<NotificationDto> result = notificationService.getMyNotifications(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).channel()).isEqualTo(NotificationChannel.EMAIL);
    }

    @Test
    void getMyNotifications_keepsRows_withNullCorrelationId() {
        UUID userId = UUID.randomUUID();
        Notification first = Notification.builder().id(UUID.randomUUID()).userId(userId)
                .type(NotificationType.WELCOME).channel(NotificationChannel.EMAIL)
                .recipient("user@example.com").subject("Welcome").body("Hi")
                .status(NotificationStatus.SENT).retryCount(0).build();
        Notification second = Notification.builder().id(UUID.randomUUID()).userId(userId)
                .type(NotificationType.POLICY_ACTIVATED).channel(NotificationChannel.EMAIL)
                .recipient("user@example.com").subject("Policy Activated").body("Hi")
                .status(NotificationStatus.SENT).retryCount(0).build();

        when(notificationRepository.findByUserId(userId)).thenReturn(List.of(first, second));
        when(notificationMapper.toDto(any(Notification.class))).thenAnswer(invocation -> toDtoStub(invocation.getArgument(0)));

        List<NotificationDto> result = notificationService.getMyNotifications(userId);

        assertThat(result).hasSize(2);
    }

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

        PagedResponse<NotificationDto> result = notificationService.getAllNotifications(pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.last()).isTrue();
    }
}
