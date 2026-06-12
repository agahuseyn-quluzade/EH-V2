package com.ehi.policy.mapper;

import com.ehi.policy.dto.response.PolicyDto;
import com.ehi.policy.entity.Policy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PolicyMapper {

    @Mapping(target = "planId", source = "plan.id")
    @Mapping(target = "planName", source = "plan.name")
    PolicyDto toDto(Policy policy);
}
