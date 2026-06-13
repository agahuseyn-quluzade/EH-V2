package com.ehi.notification.service.impl;

import com.ehi.notification.service.SmsProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MockSmsProvider implements SmsProvider {

    @Override
    public void send(String phone, String message) {
        log.info("SMS TO {} : {}", phone, message);
    }
}
