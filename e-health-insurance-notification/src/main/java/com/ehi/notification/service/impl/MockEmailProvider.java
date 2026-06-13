package com.ehi.notification.service.impl;

import com.ehi.notification.service.EmailProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("it")
@Slf4j
public class MockEmailProvider implements EmailProvider {

    @Override
    public void send(String to, String subject, String body) {
        log.info("EMAIL TO {} | {} | {}", to, subject, body);
    }
}
