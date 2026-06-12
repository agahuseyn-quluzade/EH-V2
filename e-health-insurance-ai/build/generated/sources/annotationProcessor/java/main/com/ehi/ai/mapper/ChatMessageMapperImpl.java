package com.ehi.ai.mapper;

import com.ehi.ai.dto.response.ChatMessageDto;
import com.ehi.ai.entity.ChatMessage;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-12T14:01:57+0400",
    comments = "version: 1.5.5.Final, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.10.2.jar, environment: Java 17.0.19 (Homebrew)"
)
@Component
public class ChatMessageMapperImpl implements ChatMessageMapper {

    @Override
    public ChatMessageDto toDto(ChatMessage chatMessage) {
        if ( chatMessage == null ) {
            return null;
        }

        ChatMessageDto.ChatMessageDtoBuilder chatMessageDto = ChatMessageDto.builder();

        chatMessageDto.id( chatMessage.getId() );
        chatMessageDto.sessionId( chatMessage.getSessionId() );
        chatMessageDto.role( chatMessage.getRole() );
        chatMessageDto.content( chatMessage.getContent() );
        chatMessageDto.createdAt( chatMessage.getCreatedAt() );

        return chatMessageDto.build();
    }
}
