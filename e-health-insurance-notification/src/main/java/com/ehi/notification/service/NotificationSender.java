package com.ehi.notification.service;

import com.ehi.notification.entity.Notification;

public interface NotificationSender {
    boolean send(Notification notification);
}
