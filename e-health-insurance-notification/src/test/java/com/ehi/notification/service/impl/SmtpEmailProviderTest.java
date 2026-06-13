package com.ehi.notification.service.impl;

import com.ehi.notification.config.NotificationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SmtpEmailProviderTest {

    @Mock JavaMailSender mailSender;

    SmtpEmailProvider smtpEmailProvider;

    @BeforeEach
    void setUp() {
        NotificationProperties properties = new NotificationProperties();
        properties.getEmail().setFrom("no-reply@ehi.example.com");
        smtpEmailProvider = new SmtpEmailProvider(mailSender, properties);
    }

    @Test
    void send_buildsAndSendsSimpleMailMessage() {
        smtpEmailProvider.send("user@example.com", "Welcome", "Hi there");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertThat(message.getFrom()).isEqualTo("no-reply@ehi.example.com");
        assertThat(message.getTo()).containsExactly("user@example.com");
        assertThat(message.getSubject()).isEqualTo("Welcome");
        assertThat(message.getText()).isEqualTo("Hi there");
    }
}
