package com.ehi.claim.mapper;

import com.ehi.claim.dto.response.ClaimEvidenceDto;
import com.ehi.claim.entity.Claim;
import com.ehi.claim.entity.ClaimEvidence;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-13T19:30:11+0400",
    comments = "version: 1.5.5.Final, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.10.2.jar, environment: Java 21.0.11 (Homebrew)"
)
@Component
public class ClaimEvidenceMapperImpl implements ClaimEvidenceMapper {

    @Override
    public ClaimEvidenceDto toDto(ClaimEvidence evidence) {
        if ( evidence == null ) {
            return null;
        }

        ClaimEvidenceDto.ClaimEvidenceDtoBuilder claimEvidenceDto = ClaimEvidenceDto.builder();

        claimEvidenceDto.claimId( evidenceClaimId( evidence ) );
        claimEvidenceDto.id( evidence.getId() );
        claimEvidenceDto.fileName( evidence.getFileName() );
        claimEvidenceDto.filePath( evidence.getFilePath() );
        claimEvidenceDto.contentType( evidence.getContentType() );
        claimEvidenceDto.uploadedAt( evidence.getUploadedAt() );

        return claimEvidenceDto.build();
    }

    private UUID evidenceClaimId(ClaimEvidence claimEvidence) {
        if ( claimEvidence == null ) {
            return null;
        }
        Claim claim = claimEvidence.getClaim();
        if ( claim == null ) {
            return null;
        }
        UUID id = claim.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}
