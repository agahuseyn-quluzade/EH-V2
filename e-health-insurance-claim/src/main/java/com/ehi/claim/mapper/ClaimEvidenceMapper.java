package com.ehi.claim.mapper;

import com.ehi.claim.dto.response.ClaimEvidenceDto;
import com.ehi.claim.entity.ClaimEvidence;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClaimEvidenceMapper {

    @Mapping(target = "claimId", source = "claim.id")
    ClaimEvidenceDto toDto(ClaimEvidence evidence);
}
