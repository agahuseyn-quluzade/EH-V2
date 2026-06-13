package com.ehi.notification.service;

public interface SmsProvider {
    void send(String phone, String message);
}
