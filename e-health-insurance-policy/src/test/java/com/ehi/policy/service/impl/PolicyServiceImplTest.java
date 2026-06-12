package com.ehi.policy.service.impl;

import com.ehi.infra.enums.PolicyStatus;
import com.ehi.infra.event.PolicyCreatedEvent;
import com.ehi.infra.exception.BadRequestException;
import com.ehi.infra.exception.NotFoundException;
import com.ehi.policy.dto.request.PurchasePolicyRequest;
import com.ehi.policy.dto.response.PolicyDto;
import com.ehi.policy.entity.Plan;
import com.ehi.policy.entity.Policy;
import com.ehi.policy.kafka.PolicyCreatedEventProducer;
import com.ehi.policy.mapper.PolicyMapper;
import com.ehi.policy.repository.PlanRepository;
import com.ehi.policy.repository.PolicyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyServiceImplTest {

    @Mock PolicyRepository policyRepository;
    @Mock PlanRepository planRepository;
    @Mock PolicyMapper policyMapper;
    @Mock PolicyCreatedEventProducer policyCreatedEventProducer;

    @InjectMocks PolicyServiceImpl policyService;

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

    private Policy samplePolicy(UUID userId, Plan plan, PolicyStatus status) {
        return Policy.builder()
                .id(UUID.randomUUID())
                .policyNumber("POL-12345678")
                .userId(userId)
                .plan(plan)
                .status(status)
                .premiumAmount(plan.getPremiumAmount())
                .build();
    }

    @Test
    void purchasePolicy_throwsNotFound_whenPlanMissing() {
        UUID userId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        when(planRepository.findById(planId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> policyService.purchasePolicy(userId, new PurchasePolicyRequest(planId)))
                .isInstanceOf(NotFoundException.class);

        verify(policyRepository, never()).save(any(Policy.class));
    }

    @Test
    void purchasePolicy_throwsBadRequest_whenPlanInactive() {
        UUID userId = UUID.randomUUID();
        Plan plan = samplePlan(false);
        when(planRepository.findById(plan.getId())).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> policyService.purchasePolicy(userId, new PurchasePolicyRequest(plan.getId())))
                .isInstanceOf(BadRequestException.class);

        verify(policyRepository, never()).save(any(Policy.class));
    }

    @Test
    void purchasePolicy_createsPendingPolicy_copiesPremium_andPublishesEvent() {
        UUID userId = UUID.randomUUID();
        Plan plan = samplePlan(true);
        when(planRepository.findById(plan.getId())).thenReturn(Optional.of(plan));
        when(policyRepository.save(any(Policy.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(policyMapper.toDto(any(Policy.class))).thenAnswer(invocation -> {
            Policy policy = invocation.getArgument(0);
            return PolicyDto.builder()
                    .id(policy.getId())
                    .policyNumber(policy.getPolicyNumber())
                    .userId(policy.getUserId())
                    .planId(plan.getId())
                    .planName(plan.getName())
                    .status(policy.getStatus())
                    .premiumAmount(policy.getPremiumAmount())
                    .build();
        });

        PolicyDto result = policyService.purchasePolicy(userId, new PurchasePolicyRequest(plan.getId()));

        ArgumentCaptor<Policy> captor = ArgumentCaptor.forClass(Policy.class);
        verify(policyRepository).save(captor.capture());
        Policy saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(PolicyStatus.PENDING);
        assertThat(saved.getPolicyNumber()).startsWith("POL-");
        assertThat(saved.getPremiumAmount()).isEqualTo(plan.getPremiumAmount());

        assertThat(result.status()).isEqualTo(PolicyStatus.PENDING);
        assertThat(result.policyNumber()).startsWith("POL-");

        ArgumentCaptor<PolicyCreatedEvent> eventCaptor = ArgumentCaptor.forClass(PolicyCreatedEvent.class);
        verify(policyCreatedEventProducer).publish(eventCaptor.capture());
        PolicyCreatedEvent event = eventCaptor.getValue();
        assertThat(event.userId()).isEqualTo(userId);
        assertThat(event.planId()).isEqualTo(plan.getId());
        assertThat(event.premiumAmount()).isEqualTo(plan.getPremiumAmount());
    }

    @Test
    void getPolicyById_throwsNotFound_whenNonOwnerNonAdmin() {
        UUID ownerId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Plan plan = samplePlan(true);
        Policy policy = samplePolicy(ownerId, plan, PolicyStatus.ACTIVE);
        when(policyRepository.findById(policy.getId())).thenReturn(Optional.of(policy));

        assertThatThrownBy(() -> policyService.getPolicyById(policy.getId(), requesterId, false))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getPolicyById_returnsPolicy_forAdmin_regardlessOfOwner() {
        UUID ownerId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Plan plan = samplePlan(true);
        Policy policy = samplePolicy(ownerId, plan, PolicyStatus.ACTIVE);
        when(policyRepository.findById(policy.getId())).thenReturn(Optional.of(policy));
        when(policyMapper.toDto(policy)).thenReturn(
                PolicyDto.builder().id(policy.getId()).userId(ownerId).status(PolicyStatus.ACTIVE).build());

        PolicyDto result = policyService.getPolicyById(policy.getId(), adminId, true);

        assertThat(result.userId()).isEqualTo(ownerId);
    }

    @Test
    void cancelPolicy_allowsFromPendingOrActive() {
        UUID userId = UUID.randomUUID();
        Plan plan = samplePlan(true);
        Policy policy = samplePolicy(userId, plan, PolicyStatus.ACTIVE);
        when(policyRepository.findById(policy.getId())).thenReturn(Optional.of(policy));
        when(policyRepository.save(any(Policy.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(policyMapper.toDto(any(Policy.class))).thenAnswer(invocation -> {
            Policy p = invocation.getArgument(0);
            return PolicyDto.builder().id(p.getId()).status(p.getStatus()).build();
        });

        PolicyDto result = policyService.cancelPolicy(policy.getId(), userId, false);

        assertThat(result.status()).isEqualTo(PolicyStatus.CANCELLED);
    }

    @Test
    void cancelPolicy_throwsBadRequest_whenAlreadyCancelled() {
        UUID userId = UUID.randomUUID();
        Plan plan = samplePlan(true);
        Policy policy = samplePolicy(userId, plan, PolicyStatus.CANCELLED);
        when(policyRepository.findById(policy.getId())).thenReturn(Optional.of(policy));

        assertThatThrownBy(() -> policyService.cancelPolicy(policy.getId(), userId, false))
                .isInstanceOf(BadRequestException.class);

        verify(policyRepository, never()).save(any(Policy.class));
    }

    @Test
    void activatePolicy_skipsNonPendingPolicy() {
        UUID userId = UUID.randomUUID();
        Plan plan = samplePlan(true);
        Policy policy = samplePolicy(userId, plan, PolicyStatus.ACTIVE);
        when(policyRepository.findById(policy.getId())).thenReturn(Optional.of(policy));

        policyService.activatePolicy(policy.getId());

        verify(policyRepository, never()).save(any(Policy.class));
    }

    @Test
    void activatePolicy_activatesPendingPolicy_withEndDateFromDuration() {
        UUID userId = UUID.randomUUID();
        Plan plan = samplePlan(true);
        Policy policy = samplePolicy(userId, plan, PolicyStatus.PENDING);
        when(policyRepository.findById(policy.getId())).thenReturn(Optional.of(policy));
        when(policyRepository.save(any(Policy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        policyService.activatePolicy(policy.getId());

        ArgumentCaptor<Policy> captor = ArgumentCaptor.forClass(Policy.class);
        verify(policyRepository).save(captor.capture());
        Policy saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(PolicyStatus.ACTIVE);
        assertThat(saved.getStartDate()).isNotNull();
        assertThat(saved.getEndDate()).isEqualTo(
                saved.getStartDate().atZone(java.time.ZoneOffset.UTC).plusMonths(plan.getDurationMonths()).toInstant());
    }
}
