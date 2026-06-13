package com.ehi.claim.mapper;

import com.ehi.claim.dto.response.ClaimDto;
import com.ehi.claim.entity.Claim;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-13T11:18:12+0400",
    comments = "version: 1.5.5.Final, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.10.2.jar, environment: Java 21.0.11 (Homebrew)"
)
@Component
public class ClaimMapperImpl implements ClaimMapper {

    @Override
    public ClaimDto toDto(Claim claim) {
        if ( claim == null ) {
            return null;
        }

        ClaimDto.ClaimDtoBuilder claimDto = ClaimDto.builder();

        claimDto.id( claim.getId() );
        claimDto.claimNumber( claim.getClaimNumber() );
        claimDto.userId( claim.getUserId() );
        claimDto.policyId( claim.getPolicyId() );
        claimDto.claimType( claim.getClaimType() );
        claimDto.amount( claim.getAmount() );
        claimDto.description( claim.getDescription() );
        claimDto.status( claim.getStatus() );
        claimDto.approvedAmount( claim.getApprovedAmount() );
        claimDto.rejectionReason( claim.getRejectionReason() );
        claimDto.reviewedBy( claim.getReviewedBy() );
        claimDto.riskScore( claim.getRiskScore() );
        List<String> list = claim.getFraudFlags();
        if ( list != null ) {
            claimDto.fraudFlags( new ArrayList<String>( list ) );
        }
        claimDto.aiExplanation( claim.getAiExplanation() );
        claimDto.createdAt( claim.getCreatedAt() );

        return claimDto.build();
    }
}
