package com.eHealthInsurance.client;

import com.eHealthInsurance.client.dto.MemberStatusResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "iam-service", url = "${services.iam-url}")
public interface IamServiceClient {

    @GetMapping("/api/v1/users/{id}/status")
    MemberStatusResponse getMemberStatus(@PathVariable("id") UUID id);
}
