package com.ehi.ai.mapper;

import com.ehi.ai.dto.response.ChatMessageDto;
import com.ehi.ai.entity.ChatMessage;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChatMessageMapper {

    ChatMessageDto toDto(ChatMessage chatMessage);
}
