package com.ehi.policy.service;

import com.ehi.infra.dto.PagedResponse;
import com.ehi.policy.dto.request.PurchasePolicyRequest;
import com.ehi.policy.dto.response.PolicyDto;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface PolicyService {

    PolicyDto purchasePolicy(UUID userId, PurchasePolicyRequest request);

    List<PolicyDto> getMyPolicies(UUID userId);

    PolicyDto getPolicyById(UUID policyId, UUID requesterId, boolean admin);

    PolicyDto cancelPolicy(UUID policyId, UUID requesterId, boolean admin);

    PagedResponse<PolicyDto> getAllPolicies(Pageable pageable);

    void activatePolicy(UUID policyId);
}
