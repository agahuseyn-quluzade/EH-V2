package com.ehi.notification.service;

public interface EmailProvider {
    void send(String to, String subject, String body);
}
