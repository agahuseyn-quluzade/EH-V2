package com.ehi.notification.service.impl;

import com.ehi.infra.enums.NotificationChannel;
import com.ehi.notification.config.AwsProperties;
import com.ehi.notification.entity.Notification;
import com.ehi.notification.service.NotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SesV2Exception;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.SnsException;

@Component
@ConditionalOnProperty(prefix = "aws", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class AwsNotificationSender implements NotificationSender {

    private final SnsClient snsClient;
    private final SesV2Client sesV2Client;
    private final AwsProperties awsProperties;

    @Override
    public boolean send(Notification notification) {
        if (notification.getChannel() == NotificationChannel.SMS) {
            return sendSms(notification);
        }
        return sendEmail(notification);
    }

    private boolean sendSms(Notification notification) {
        try {
            snsClient.publish(PublishRequest.builder()
                    .phoneNumber(notification.getRecipient())
                    .message(notification.getBody())
                    .build());
            log.info("SMS sent via SNS to {}", notification.getRecipient());
            return true;
        } catch (SnsException e) {
            log.error("SNS SMS failed for userId={} recipient={}: {}",
                    notification.getUserId(), notification.getRecipient(), e.getMessage());
            return false;
        }
    }

    private boolean sendEmail(Notification notification) {
        try {
            sesV2Client.sendEmail(SendEmailRequest.builder()
                    .fromEmailAddress(awsProperties.getSes().getFrom())
                    .destination(Destination.builder()
                            .toAddresses(notification.getRecipient())
                            .build())
                    .content(EmailContent.builder()
                            .simple(Message.builder()
                                    .subject(Content.builder().data(notification.getSubject()).build())
                                    .body(Body.builder()
                                            .text(Content.builder().data(notification.getBody()).build())
                                            .build())
                                    .build())
                            .build())
                    .build());
            log.info("Email sent via SES to {}", notification.getRecipient());
            return true;
        } catch (SesV2Exception e) {
            log.error("SES email failed for userId={} recipient={}: {}",
                    notification.getUserId(), notification.getRecipient(), e.getMessage());
            return false;
        }
    }
}
