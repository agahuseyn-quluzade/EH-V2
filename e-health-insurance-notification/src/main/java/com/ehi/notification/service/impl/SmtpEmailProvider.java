package com.ehi.notification.service.impl;

import com.ehi.notification.config.NotificationProperties;
import com.ehi.notification.service.EmailProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Profile("!it")
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailProvider implements EmailProvider {

    private final JavaMailSender mailSender;
    private final NotificationProperties notificationProperties;

    @Override
    public void send(String to, String subject, String body) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(notificationProperties.getEmail().getFrom());
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        mailSender.send(msg);
        log.info("Email sent to {}", to);
    }
}
