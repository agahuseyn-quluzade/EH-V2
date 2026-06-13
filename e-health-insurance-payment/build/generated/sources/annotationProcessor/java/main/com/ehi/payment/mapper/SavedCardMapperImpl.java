package com.ehi.payment.mapper;

import com.ehi.payment.dto.response.SavedCardDto;
import com.ehi.payment.entity.SavedCard;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-13T06:36:21+0400",
    comments = "version: 1.5.5.Final, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.10.2.jar, environment: Java 17.0.19 (Eclipse Adoptium)"
)
@Component
public class SavedCardMapperImpl implements SavedCardMapper {

    @Override
    public SavedCardDto toDto(SavedCard savedCard) {
        if ( savedCard == null ) {
            return null;
        }

        SavedCardDto.SavedCardDtoBuilder savedCardDto = SavedCardDto.builder();

        savedCardDto.id( savedCard.getId() );
        savedCardDto.cardMask( savedCard.getCardMask() );
        savedCardDto.cardName( savedCard.getCardName() );
        savedCardDto.active( savedCard.isActive() );
        savedCardDto.createdAt( savedCard.getCreatedAt() );

        return savedCardDto.build();
    }
}
