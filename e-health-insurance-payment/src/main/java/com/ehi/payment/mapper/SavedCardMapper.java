package com.ehi.payment.mapper;

import com.ehi.payment.dto.response.SavedCardDto;
import com.ehi.payment.entity.SavedCard;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SavedCardMapper {

    SavedCardDto toDto(SavedCard savedCard);
}
