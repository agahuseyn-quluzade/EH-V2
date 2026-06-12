package com.ehi.ai.mapper;

import com.ehi.ai.dto.response.FraudAiResponse;
import com.ehi.ai.entity.FraudCheck;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FraudCheckMapper {

    FraudAiResponse toDto(FraudCheck fraudCheck);
}
