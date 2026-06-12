package com.ehi.policy.mapper;

import com.ehi.policy.dto.response.PlanDto;
import com.ehi.policy.entity.Plan;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-12T13:38:38+0400",
    comments = "version: 1.5.5.Final, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.10.2.jar, environment: Java 17.0.19 (Homebrew)"
)
@Component
public class PlanMapperImpl implements PlanMapper {

    @Override
    public PlanDto toDto(Plan plan) {
        if ( plan == null ) {
            return null;
        }

        PlanDto.PlanDtoBuilder planDto = PlanDto.builder();

        planDto.id( plan.getId() );
        planDto.name( plan.getName() );
        planDto.description( plan.getDescription() );
        planDto.coverageAmount( plan.getCoverageAmount() );
        planDto.premiumAmount( plan.getPremiumAmount() );
        planDto.durationMonths( plan.getDurationMonths() );
        planDto.active( plan.isActive() );

        return planDto.build();
    }
}
