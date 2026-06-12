package com.ehi.policy.mapper;

import com.ehi.policy.dto.response.PlanDto;
import com.ehi.policy.entity.Plan;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PlanMapper {

    PlanDto toDto(Plan plan);
}
