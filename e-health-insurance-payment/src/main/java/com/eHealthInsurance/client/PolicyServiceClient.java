package com.eHealthInsurance.client;

import com.eHealthInsurance.client.dto.PolicyResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "policy-service", url = "${services.policy-url}")
public interface PolicyServiceClient {

    @GetMapping("/api/v1/policies/{id}")
    PolicyResponse getPolicy(@PathVariable("id") UUID id);
}
