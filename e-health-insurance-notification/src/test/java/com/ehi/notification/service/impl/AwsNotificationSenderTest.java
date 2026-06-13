package com.ehi.notification.service.impl;

import com.ehi.infra.enums.NotificationChannel;
import com.ehi.infra.enums.NotificationType;
import com.ehi.notification.config.AwsProperties;
import com.ehi.notification.entity.Notification;
import com.ehi.notification.enums.NotificationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SesV2Exception;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.SnsException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AwsNotificationSenderTest {

    @Mock SnsClient snsClient;
    @Mock SesV2Client sesV2Client;
    @Mock AwsProperties awsProperties;
    @Mock AwsProperties.Ses ses;

    @InjectMocks AwsNotificationSender sender;

    // ---- SMS via SNS ----

    @Test
    void send_returnsTrueAndPublishesToSns_forSmsChannel() {
        Notification notification = smsNotification("+994501234567", "Your claim has been approved.");

        boolean result = sender.send(notification);

        assertThat(result).isTrue();
        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());
        PublishRequest req = captor.getValue();
        assertThat(req.phoneNumber()).isEqualTo("+994501234567");
        assertThat(req.message()).isEqualTo("Your claim has been approved.");
    }

    @Test
    void send_returnsFalse_whenSnsThrows() {
        Notification notification = smsNotification("+994501234567", "body");
        when(snsClient.publish(any(PublishRequest.class))).thenThrow(
                SnsException.builder().message("SnsException").statusCode(500).build());

        boolean result = sender.send(notification);

        assertThat(result).isFalse();
    }

    // ---- Email via SES ----

    @Test
    void send_returnsTrueAndSendsEmail_forEmailChannel() {
        when(awsProperties.getSes()).thenReturn(ses);
        when(ses.getFrom()).thenReturn("no-reply@ehi.example.com");
        Notification notification = emailNotification("user@example.com", "Claim Approved", "Your claim is approved.");

        boolean result = sender.send(notification);

        assertThat(result).isTrue();
        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(sesV2Client).sendEmail(captor.capture());
        SendEmailRequest req = captor.getValue();
        assertThat(req.fromEmailAddress()).isEqualTo("no-reply@ehi.example.com");
        assertThat(req.destination().toAddresses()).containsExactly("user@example.com");
        assertThat(req.content().simple().subject().data()).isEqualTo("Claim Approved");
        assertThat(req.content().simple().body().text().data()).isEqualTo("Your claim is approved.");
    }

    @Test
    void send_returnsFalse_whenSesThrows() {
        when(awsProperties.getSes()).thenReturn(ses);
        when(ses.getFrom()).thenReturn("no-reply@ehi.example.com");
        Notification notification = emailNotification("user@example.com", "subject", "body");
        when(sesV2Client.sendEmail(any(SendEmailRequest.class))).thenThrow(
                SesV2Exception.builder().message("SesException").statusCode(500).build());

        boolean result = sender.send(notification);

        assertThat(result).isFalse();
    }

    // ---- helpers ----

    private Notification smsNotification(String phone, String body) {
        return Notification.builder()
                .id(UUID.randomUUID()).userId(UUID.randomUUID())
                .type(NotificationType.CLAIM_APPROVED).channel(NotificationChannel.SMS)
                .recipient(phone).subject("subject").body(body)
                .status(NotificationStatus.PENDING).retryCount(0)
                .build();
    }

    private Notification emailNotification(String email, String subject, String body) {
        return Notification.builder()
                .id(UUID.randomUUID()).userId(UUID.randomUUID())
                .type(NotificationType.CLAIM_APPROVED).channel(NotificationChannel.EMAIL)
                .recipient(email).subject(subject).body(body)
                .status(NotificationStatus.PENDING).retryCount(0)
                .build();
    }
}
