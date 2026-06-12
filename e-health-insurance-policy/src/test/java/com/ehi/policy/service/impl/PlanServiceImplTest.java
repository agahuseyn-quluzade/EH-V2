package com.ehi.policy.service.impl;

import com.ehi.infra.exception.DuplicateResourceException;
import com.ehi.infra.exception.NotFoundException;
import com.ehi.policy.dto.request.CreatePlanRequest;
import com.ehi.policy.dto.response.PlanDto;
import com.ehi.policy.entity.Plan;
import com.ehi.policy.mapper.PlanMapper;
import com.ehi.policy.repository.PlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanServiceImplTest {

    @Mock PlanRepository planRepository;
    @Mock PlanMapper planMapper;

    @InjectMocks PlanServiceImpl planService;

    private Plan samplePlan(boolean active) {
        return Plan.builder()
                .id(UUID.randomUUID())
                .name("Basic")
                .description("Basic coverage")
                .coverageAmount(BigDecimal.valueOf(10000))
                .premiumAmount(BigDecimal.valueOf(50))
                .durationMonths(12)
                .active(active)
                .build();
    }

    private CreatePlanRequest sampleRequest() {
        return CreatePlanRequest.builder()
                .name("Basic")
                .description("Basic coverage")
                .coverageAmount(BigDecimal.valueOf(10000))
                .premiumAmount(BigDecimal.valueOf(50))
                .durationMonths(12)
                .build();
    }

    @Test
    void createPlan_throwsDuplicateResource_whenNameExists() {
        when(planRepository.existsByName("Basic")).thenReturn(true);

        assertThatThrownBy(() -> planService.createPlan(sampleRequest()))
                .isInstanceOf(DuplicateResourceException.class);

        verify(planRepository, never()).save(any(Plan.class));
    }

    @Test
    void createPlan_savesActivePlan_whenNameIsNew() {
        when(planRepository.existsByName("Basic")).thenReturn(false);
        when(planRepository.save(any(Plan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(planMapper.toDto(any(Plan.class))).thenAnswer(invocation -> {
            Plan plan = invocation.getArgument(0);
            return PlanDto.builder().id(plan.getId()).name(plan.getName()).active(plan.isActive()).build();
        });

        PlanDto result = planService.createPlan(sampleRequest());

        ArgumentCaptor<Plan> captor = ArgumentCaptor.forClass(Plan.class);
        verify(planRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isTrue();
        assertThat(result.active()).isTrue();
        assertThat(result.name()).isEqualTo("Basic");
    }

    @Test
    void getActivePlans_returnsOnlyActivePlans() {
        Plan activePlan = samplePlan(true);
        when(planRepository.findByActiveTrue()).thenReturn(List.of(activePlan));
        when(planMapper.toDto(activePlan)).thenReturn(
                PlanDto.builder().id(activePlan.getId()).name(activePlan.getName()).active(true).build());

        List<PlanDto> result = planService.getActivePlans();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).active()).isTrue();
    }

    @Test
    void getPlanById_throwsNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        when(planRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planService.getPlanById(id))
                .isInstanceOf(NotFoundException.class);
    }
}
