package com.ehi.claim.mapper;

import com.ehi.claim.dto.response.ClaimDto;
import com.ehi.claim.entity.Claim;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ClaimMapper {

    ClaimDto toDto(Claim claim);
}
