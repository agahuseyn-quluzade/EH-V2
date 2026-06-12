package com.ehi.ai.mapper;

import com.ehi.ai.dto.response.RiskAiResponse;
import com.ehi.ai.entity.RiskProfile;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RiskProfileMapper {

    RiskAiResponse toDto(RiskProfile riskProfile);
}
