package com.ehi.ai.mapper;

import com.ehi.ai.dto.response.RiskAiResponse;
import com.ehi.ai.entity.RiskProfile;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-12T14:08:32+0400",
    comments = "version: 1.5.5.Final, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.10.2.jar, environment: Java 17.0.19 (Homebrew)"
)
@Component
public class RiskProfileMapperImpl implements RiskProfileMapper {

    @Override
    public RiskAiResponse toDto(RiskProfile riskProfile) {
        if ( riskProfile == null ) {
            return null;
        }

        RiskAiResponse.RiskAiResponseBuilder riskAiResponse = RiskAiResponse.builder();

        riskAiResponse.userId( riskProfile.getUserId() );
        riskAiResponse.totalClaims( riskProfile.getTotalClaims() );
        riskAiResponse.averageRiskScore( riskProfile.getAverageRiskScore() );
        riskAiResponse.highRiskCount( riskProfile.getHighRiskCount() );
        riskAiResponse.lastClaimAt( riskProfile.getLastClaimAt() );

        return riskAiResponse.build();
    }
}
