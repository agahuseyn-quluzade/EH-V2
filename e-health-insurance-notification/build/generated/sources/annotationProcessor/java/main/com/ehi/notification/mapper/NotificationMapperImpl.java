package com.ehi.notification.mapper;

import com.ehi.notification.dto.response.NotificationDto;
import com.ehi.notification.entity.Notification;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-13T14:49:40+0400",
    comments = "version: 1.5.5.Final, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.10.2.jar, environment: Java 21.0.11 (Homebrew)"
)
@Component
public class NotificationMapperImpl implements NotificationMapper {

    @Override
    public NotificationDto toDto(Notification notification) {
        if ( notification == null ) {
            return null;
        }

        NotificationDto.NotificationDtoBuilder notificationDto = NotificationDto.builder();

        notificationDto.id( notification.getId() );
        notificationDto.userId( notification.getUserId() );
        notificationDto.type( notification.getType() );
        notificationDto.channel( notification.getChannel() );
        notificationDto.recipient( notification.getRecipient() );
        notificationDto.subject( notification.getSubject() );
        notificationDto.body( notification.getBody() );
        notificationDto.status( notification.getStatus() );
        notificationDto.retryCount( notification.getRetryCount() );
        notificationDto.createdAt( notification.getCreatedAt() );

        return notificationDto.build();
    }
}
