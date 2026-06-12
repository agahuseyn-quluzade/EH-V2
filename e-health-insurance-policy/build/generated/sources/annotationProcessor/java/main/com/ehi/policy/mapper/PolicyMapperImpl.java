package com.ehi.policy.mapper;

import com.ehi.policy.dto.response.PolicyDto;
import com.ehi.policy.entity.Plan;
import com.ehi.policy.entity.Policy;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-12T13:38:38+0400",
    comments = "version: 1.5.5.Final, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.10.2.jar, environment: Java 17.0.19 (Homebrew)"
)
@Component
public class PolicyMapperImpl implements PolicyMapper {

    @Override
    public PolicyDto toDto(Policy policy) {
        if ( policy == null ) {
            return null;
        }

        PolicyDto.PolicyDtoBuilder policyDto = PolicyDto.builder();

        policyDto.planId( policyPlanId( policy ) );
        policyDto.planName( policyPlanName( policy ) );
        policyDto.id( policy.getId() );
        policyDto.policyNumber( policy.getPolicyNumber() );
        policyDto.userId( policy.getUserId() );
        policyDto.status( policy.getStatus() );
        policyDto.premiumAmount( policy.getPremiumAmount() );
        policyDto.startDate( policy.getStartDate() );
        policyDto.endDate( policy.getEndDate() );

        return policyDto.build();
    }

    private UUID policyPlanId(Policy policy) {
        if ( policy == null ) {
            return null;
        }
        Plan plan = policy.getPlan();
        if ( plan == null ) {
            return null;
        }
        UUID id = plan.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String policyPlanName(Policy policy) {
        if ( policy == null ) {
            return null;
        }
        Plan plan = policy.getPlan();
        if ( plan == null ) {
            return null;
        }
        String name = plan.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }
}
