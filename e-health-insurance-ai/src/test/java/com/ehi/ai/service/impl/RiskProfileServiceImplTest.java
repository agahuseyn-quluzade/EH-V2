package com.ehi.ai.service.impl;

import com.ehi.ai.dto.response.RiskAiResponse;
import com.ehi.ai.entity.RiskProfile;
import com.ehi.ai.mapper.RiskProfileMapper;
import com.ehi.ai.repository.RiskProfileRepository;
import com.ehi.infra.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskProfileServiceImplTest {

    @Mock RiskProfileRepository riskProfileRepository;
    @Mock RiskProfileMapper riskProfileMapper;

    @InjectMocks
    RiskProfileServiceImpl service;

    @Test
    void getRiskProfile_returnsDto_whenFound() {
        UUID userId = UUID.randomUUID();
        RiskProfile profile = RiskProfile.builder().userId(userId).totalClaims(3).build();
        RiskAiResponse dto = RiskAiResponse.builder().userId(userId).totalClaims(3).build();
        when(riskProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(riskProfileMapper.toDto(profile)).thenReturn(dto);

        RiskAiResponse result = service.getRiskProfile(userId);

        assertThat(result).isEqualTo(dto);
    }

    @Test
    void getRiskProfile_throwsNotFound_whenMissing() {
        UUID userId = UUID.randomUUID();
        when(riskProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRiskProfile(userId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void recordFraudCheck_createsNewProfile_whenNoneExists() {
        UUID userId = UUID.randomUUID();
        when(riskProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        service.recordFraudCheck(userId, 50, false);

        ArgumentCaptor<RiskProfile> captor = ArgumentCaptor.forClass(RiskProfile.class);
        verify(riskProfileRepository).save(captor.capture());
        RiskProfile saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getTotalClaims()).isEqualTo(1);
        assertThat(saved.getAverageRiskScore()).isEqualTo(50.0);
        assertThat(saved.getHighRiskCount()).isEqualTo(0);
        assertThat(saved.getLastClaimAt()).isCloseTo(java.time.Instant.now(), within(5, java.time.temporal.ChronoUnit.SECONDS));
    }

    @Test
    void recordFraudCheck_updatesRunningAverage_andHighRiskCount_whenProfileExists() {
        UUID userId = UUID.randomUUID();
        RiskProfile existing = RiskProfile.builder()
                .userId(userId).totalClaims(2).averageRiskScore(30.0).highRiskCount(1).build();
        when(riskProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existing));

        service.recordFraudCheck(userId, 90, true);

        ArgumentCaptor<RiskProfile> captor = ArgumentCaptor.forClass(RiskProfile.class);
        verify(riskProfileRepository).save(captor.capture());
        RiskProfile saved = captor.getValue();
        assertThat(saved.getTotalClaims()).isEqualTo(3);
        assertThat(saved.getAverageRiskScore()).isEqualTo(50.0);
        assertThat(saved.getHighRiskCount()).isEqualTo(2);
    }

    @Test
    void recordFraudCheck_doesNotIncrementHighRiskCount_whenNotHighRisk() {
        UUID userId = UUID.randomUUID();
        RiskProfile existing = RiskProfile.builder()
                .userId(userId).totalClaims(1).averageRiskScore(10.0).highRiskCount(1).build();
        when(riskProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existing));

        service.recordFraudCheck(userId, 20, false);

        ArgumentCaptor<RiskProfile> captor = ArgumentCaptor.forClass(RiskProfile.class);
        verify(riskProfileRepository).save(captor.capture());
        assertThat(captor.getValue().getHighRiskCount()).isEqualTo(1);
    }

    @Test
    void isHighRiskUser_trueWhenHighRiskCountAtThreshold() {
        UUID userId = UUID.randomUUID();
        RiskProfile profile = RiskProfile.builder().userId(userId).highRiskCount(2).build();
        when(riskProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        assertThat(service.isHighRiskUser(userId)).isTrue();
    }

    @Test
    void isHighRiskUser_falseBelowThreshold() {
        UUID userId = UUID.randomUUID();
        RiskProfile profile = RiskProfile.builder().userId(userId).highRiskCount(1).build();
        when(riskProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        assertThat(service.isHighRiskUser(userId)).isFalse();
    }

    @Test
    void isHighRiskUser_falseWhenNoProfile() {
        UUID userId = UUID.randomUUID();
        when(riskProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThat(service.isHighRiskUser(userId)).isFalse();
    }
}
