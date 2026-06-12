package com.ehi.ai.mapper;

import com.ehi.ai.dto.response.FraudAiResponse;
import com.ehi.ai.entity.FraudCheck;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-12T14:08:32+0400",
    comments = "version: 1.5.5.Final, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.10.2.jar, environment: Java 17.0.19 (Homebrew)"
)
@Component
public class FraudCheckMapperImpl implements FraudCheckMapper {

    @Override
    public FraudAiResponse toDto(FraudCheck fraudCheck) {
        if ( fraudCheck == null ) {
            return null;
        }

        FraudAiResponse.FraudAiResponseBuilder fraudAiResponse = FraudAiResponse.builder();

        fraudAiResponse.claimId( fraudCheck.getClaimId() );
        fraudAiResponse.userId( fraudCheck.getUserId() );
        fraudAiResponse.ruleScore( fraudCheck.getRuleScore() );
        fraudAiResponse.aiScore( fraudCheck.getAiScore() );
        fraudAiResponse.finalScore( fraudCheck.getFinalScore() );
        List<String> list = fraudCheck.getFlags();
        if ( list != null ) {
            fraudAiResponse.flags( new ArrayList<String>( list ) );
        }
        fraudAiResponse.aiExplanation( fraudCheck.getAiExplanation() );
        fraudAiResponse.createdAt( fraudCheck.getCreatedAt() );

        return fraudAiResponse.build();
    }
}
