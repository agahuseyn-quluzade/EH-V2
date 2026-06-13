package com.ehi.policy.service.impl;

import com.ehi.infra.dto.PagedResponse;
import com.ehi.infra.enums.PolicyStatus;
import com.ehi.infra.event.PolicyCreatedEvent;
import com.ehi.infra.exception.base.BadRequestException;
import com.ehi.infra.exception.NotFoundException;
import com.ehi.policy.dto.request.PurchasePolicyRequest;
import com.ehi.policy.dto.response.PolicyDto;
import com.ehi.policy.entity.Plan;
import com.ehi.policy.entity.Policy;
import com.ehi.policy.kafka.PolicyCreatedEventProducer;
import com.ehi.policy.mapper.PolicyMapper;
import com.ehi.policy.repository.PlanRepository;
import com.ehi.policy.repository.PolicyRepository;
import com.ehi.policy.service.PolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyServiceImpl implements PolicyService {

    private final PolicyRepository policyRepository;
    private final PlanRepository planRepository;
    private final PolicyMapper policyMapper;
    private final PolicyCreatedEventProducer policyCreatedEventProducer;

    @Override
    @Transactional
    public PolicyDto purchasePolicy(UUID userId, PurchasePolicyRequest request) {
        Plan plan = planRepository.findById(request.planId())
                .orElseThrow(() -> new NotFoundException("Plan", request.planId()));

        if (!plan.isActive()) {
            throw new BadRequestException("Plan is not active: " + plan.getName());
        }

        Policy policy = Policy.builder()
                .policyNumber(generatePolicyNumber())
                .userId(userId)
                .plan(plan)
                .status(PolicyStatus.PENDING)
                .premiumAmount(plan.getPremiumAmount())
                .build();

        policy = policyRepository.save(policy);
        log.info("Policy purchased: policyId={}, userId={}, planId={}", policy.getId(), userId, plan.getId());

        policyCreatedEventProducer.publish(PolicyCreatedEvent.builder()
                .policyId(policy.getId())
                .userId(policy.getUserId())
                .planId(plan.getId())
                .policyNumber(policy.getPolicyNumber())
                .premiumAmount(policy.getPremiumAmount())
                .build());

        return policyMapper.toDto(policy);
    }

    @Override
    public List<PolicyDto> getMyPolicies(UUID userId) {
        return policyRepository.findByUserId(userId).stream()
                .map(policyMapper::toDto)
                .toList();
    }

    @Override
    public PolicyDto getPolicyById(UUID policyId, UUID requesterId, boolean admin) {
        return policyMapper.toDto(findAccessiblePolicy(policyId, requesterId, admin));
    }

    @Override
    public PolicyDto cancelPolicy(UUID policyId, UUID requesterId, boolean admin) {
        Policy policy = findAccessiblePolicy(policyId, requesterId, admin);

        if (policy.getStatus() != PolicyStatus.PENDING && policy.getStatus() != PolicyStatus.ACTIVE) {
            throw new BadRequestException("Policy cannot be cancelled in status: " + policy.getStatus());
        }

        policy.setStatus(PolicyStatus.CANCELLED);
        PolicyDto result = policyMapper.toDto(policyRepository.save(policy));
        log.info("Policy cancelled: policyId={}, userId={}", policyId, requesterId);
        return result;
    }

    @Override
    public PagedResponse<PolicyDto> getAllPolicies(Pageable pageable) {
        Page<Policy> page = policyRepository.findAll(pageable);

        return PagedResponse.<PolicyDto>builder()
                .content(page.getContent().stream().map(policyMapper::toDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Override
    @Transactional
    public void activatePolicy(UUID policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new NotFoundException("Policy", policyId));

        if (policy.getStatus() != PolicyStatus.PENDING) {
            return;
        }

        Instant start = Instant.now();
        policy.setStatus(PolicyStatus.ACTIVE);
        policy.setStartDate(start);
        policy.setEndDate(start.atZone(ZoneOffset.UTC).plusMonths(policy.getPlan().getDurationMonths()).toInstant());
        policyRepository.save(policy);
    }

    private Policy findAccessiblePolicy(UUID policyId, UUID requesterId, boolean admin) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new NotFoundException("Policy", policyId));

        if (!admin && !policy.getUserId().equals(requesterId)) {
            throw new NotFoundException("Policy", policyId);
        }

        return policy;
    }

    private String generatePolicyNumber() {
        return "POL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
