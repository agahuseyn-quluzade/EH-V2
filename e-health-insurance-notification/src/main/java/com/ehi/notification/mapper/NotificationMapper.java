package com.ehi.notification.mapper;

import com.ehi.notification.dto.response.NotificationDto;
import com.ehi.notification.entity.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationDto toDto(Notification notification);
}
