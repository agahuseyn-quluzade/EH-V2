package com.ehi.claim.repository;

import com.ehi.claim.entity.Claim;
import com.ehi.infra.enums.ClaimStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClaimRepository extends JpaRepository<Claim, UUID> {

    List<Claim> findByUserId(UUID userId);

    Page<Claim> findByStatus(ClaimStatus status, Pageable pageable);
}
