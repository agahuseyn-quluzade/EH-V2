package com.ehi.ai.service.impl;

import com.ehi.ai.entity.FraudCheck;
import com.ehi.ai.entity.PolicyCoverage;
import com.ehi.ai.kafka.FraudDetectedEventProducer;
import com.ehi.ai.mapper.FraudCheckMapper;
import com.ehi.ai.repository.FraudCheckRepository;
import com.ehi.ai.repository.PolicyCoverageRepository;
import com.ehi.ai.service.AiClientService;
import com.ehi.ai.service.RiskProfileService;
import com.ehi.infra.enums.ClaimType;
import com.ehi.infra.event.ClaimSubmittedEvent;
import com.ehi.infra.event.FraudDetectedEvent;
import com.ehi.infra.exception.NotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FraudDetectionServiceImplTest {

    @Mock FraudCheckRepository fraudCheckRepository;
    @Mock FraudCheckMapper fraudCheckMapper;
    @Mock AiClientService aiClientService;
    @Mock RiskProfileService riskProfileService;
    @Mock FraudDetectedEventProducer fraudDetectedEventProducer;
    @Mock PolicyCoverageRepository policyCoverageRepository;

    FraudDetectionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FraudDetectionServiceImpl(
                fraudCheckRepository, fraudCheckMapper, aiClientService, riskProfileService,
                fraudDetectedEventProducer, new ObjectMapper(), policyCoverageRepository);

        when(fraudCheckRepository.save(any(FraudCheck.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private ClaimSubmittedEvent event(ClaimType claimType, BigDecimal amount, UUID claimId, UUID userId) {
        return ClaimSubmittedEvent.builder()
                .claimId(claimId).userId(userId).policyId(UUID.randomUUID())
                .claimNumber("CLM-1").claimType(claimType).amount(amount).build();
    }

    @Test
    void evaluateClaim_belowThreshold_zeroRuleScore_noAiCall() {
        UUID claimId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(fraudCheckRepository.findByClaimId(claimId)).thenReturn(Optional.empty());
        when(riskProfileService.isHighRiskUser(userId)).thenReturn(false);

        service.evaluateClaim(event(ClaimType.CONSULTATION, BigDecimal.valueOf(100), claimId, userId));

        ArgumentCaptor<FraudCheck> captor = ArgumentCaptor.forClass(FraudCheck.class);
        verify(fraudCheckRepository).save(captor.capture());
        FraudCheck saved = captor.getValue();
        assertThat(saved.getRuleScore()).isEqualTo(0);
        assertThat(saved.getAiScore()).isNull();
        assertThat(saved.getFinalScore()).isEqualTo(0);
        assertThat(saved.getFlags()).isEmpty();

        verify(aiClientService, never()).chatCompletion(any());
        verify(riskProfileService).recordFraudCheck(userId, 0, false);

        ArgumentCaptor<FraudDetectedEvent> eventCaptor = ArgumentCaptor.forClass(FraudDetectedEvent.class);
        verify(fraudDetectedEventProducer).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().riskScore()).isEqualTo(0);
    }

    @Test
    void evaluateClaim_amountAboveThreshold_triggersAi_andCombinesScores() {
        UUID claimId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(fraudCheckRepository.findByClaimId(claimId)).thenReturn(Optional.empty());
        when(riskProfileService.isHighRiskUser(userId)).thenReturn(false);
        when(aiClientService.chatCompletion(any())).thenReturn("{\"score\": 50, \"explanation\": \"ok\", \"flags\": [\"AI_FLAG\"]}");

        service.evaluateClaim(event(ClaimType.CONSULTATION, BigDecimal.valueOf(600), claimId, userId));

        ArgumentCaptor<FraudCheck> captor = ArgumentCaptor.forClass(FraudCheck.class);
        verify(fraudCheckRepository).save(captor.capture());
        FraudCheck saved = captor.getValue();
        assertThat(saved.getRuleScore()).isEqualTo(40);
        assertThat(saved.getFlags()).contains("AMOUNT_ABOVE_TYPE_THRESHOLD", "AI_FLAG");
        assertThat(saved.getAiScore()).isEqualTo(50);
        assertThat(saved.getAiExplanation()).isEqualTo("ok");
        assertThat(saved.getFinalScore()).isEqualTo(46);

        verify(riskProfileService).recordFraudCheck(userId, 46, false);
    }

    @Test
    void evaluateClaim_farAboveThresholdAndHighRiskUser_parsesFencedAiResponse_andFlagsHighRisk() {
        UUID claimId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(fraudCheckRepository.findByClaimId(claimId)).thenReturn(Optional.empty());
        when(riskProfileService.isHighRiskUser(userId)).thenReturn(true);
        when(aiClientService.chatCompletion(any())).thenReturn("```json\n{\"score\": 90, \"explanation\": \"high\", \"flags\": []}\n```");

        service.evaluateClaim(event(ClaimType.CONSULTATION, BigDecimal.valueOf(1200), claimId, userId));

        ArgumentCaptor<FraudCheck> captor = ArgumentCaptor.forClass(FraudCheck.class);
        verify(fraudCheckRepository).save(captor.capture());
        FraudCheck saved = captor.getValue();
        assertThat(saved.getRuleScore()).isEqualTo(80);
        assertThat(saved.getFlags()).contains("AMOUNT_ABOVE_TYPE_THRESHOLD", "AMOUNT_FAR_ABOVE_TYPE_THRESHOLD", "REPEAT_HIGH_RISK_USER");
        assertThat(saved.getAiScore()).isEqualTo(90);
        assertThat(saved.getFinalScore()).isEqualTo(86);

        verify(riskProfileService).recordFraudCheck(userId, 86, true);
    }

    @Test
    void evaluateClaim_planBasedThreshold_doesNotFlagSmallClaimAgainstLargeCoverage() {
        UUID claimId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID policyId = UUID.randomUUID();
        when(fraudCheckRepository.findByClaimId(claimId)).thenReturn(Optional.empty());
        when(riskProfileService.isHighRiskUser(userId)).thenReturn(false);
        when(policyCoverageRepository.findByPolicyId(policyId)).thenReturn(Optional.of(
                PolicyCoverage.builder().policyId(policyId).coverageAmount(BigDecimal.valueOf(10000)).build()));

        // 600 exceeds the CONSULTATION per-type threshold (500) but is far below 50% of the 10000 coverage
        ClaimSubmittedEvent ev = ClaimSubmittedEvent.builder()
                .claimId(claimId).userId(userId).policyId(policyId)
                .claimNumber("CLM-1").claimType(ClaimType.CONSULTATION).amount(BigDecimal.valueOf(600)).build();
        service.evaluateClaim(ev);

        ArgumentCaptor<FraudCheck> captor = ArgumentCaptor.forClass(FraudCheck.class);
        verify(fraudCheckRepository).save(captor.capture());
        FraudCheck saved = captor.getValue();
        assertThat(saved.getRuleScore()).isEqualTo(0);
        assertThat(saved.getFlags()).doesNotContain("AMOUNT_ABOVE_TYPE_THRESHOLD", "AMOUNT_ABOVE_COVERAGE");
        verify(aiClientService, never()).chatCompletion(any());
    }

    @Test
    void evaluateClaim_planBasedThreshold_flagsClaimAboveCoverageRatio() {
        UUID claimId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID policyId = UUID.randomUUID();
        when(fraudCheckRepository.findByClaimId(claimId)).thenReturn(Optional.empty());
        when(riskProfileService.isHighRiskUser(userId)).thenReturn(false);
        when(policyCoverageRepository.findByPolicyId(policyId)).thenReturn(Optional.of(
                PolicyCoverage.builder().policyId(policyId).coverageAmount(BigDecimal.valueOf(1000)).build()));
        when(aiClientService.chatCompletion(any())).thenReturn("{\"score\": 50, \"explanation\": \"ok\", \"flags\": []}");

        // 600 > 50% of 1000 (=500) but < 1000 → above-coverage flag only
        ClaimSubmittedEvent ev = ClaimSubmittedEvent.builder()
                .claimId(claimId).userId(userId).policyId(policyId)
                .claimNumber("CLM-1").claimType(ClaimType.CONSULTATION).amount(BigDecimal.valueOf(600)).build();
        service.evaluateClaim(ev);

        ArgumentCaptor<FraudCheck> captor = ArgumentCaptor.forClass(FraudCheck.class);
        verify(fraudCheckRepository).save(captor.capture());
        FraudCheck saved = captor.getValue();
        assertThat(saved.getRuleScore()).isEqualTo(40);
        assertThat(saved.getFlags()).contains("AMOUNT_ABOVE_COVERAGE");
        assertThat(saved.getFlags()).doesNotContain("AMOUNT_FAR_ABOVE_COVERAGE", "AMOUNT_ABOVE_TYPE_THRESHOLD");
    }

    @Test
    void evaluateClaim_aiThrows_fallsBackToRuleOnlyScore() {
        UUID claimId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(fraudCheckRepository.findByClaimId(claimId)).thenReturn(Optional.empty());
        when(riskProfileService.isHighRiskUser(userId)).thenReturn(false);
        when(aiClientService.chatCompletion(any())).thenThrow(new RuntimeException("OpenRouter down"));

        service.evaluateClaim(event(ClaimType.CONSULTATION, BigDecimal.valueOf(600), claimId, userId));

        ArgumentCaptor<FraudCheck> captor = ArgumentCaptor.forClass(FraudCheck.class);
        verify(fraudCheckRepository).save(captor.capture());
        FraudCheck saved = captor.getValue();
        assertThat(saved.getRuleScore()).isEqualTo(40);
        assertThat(saved.getAiScore()).isNull();
        assertThat(saved.getAiExplanation()).isNull();
        assertThat(saved.getFinalScore()).isEqualTo(40);

        verify(riskProfileService).recordFraudCheck(userId, 40, false);
    }

    @Test
    void evaluateClaim_existingFraudCheck_doesNotRecordRiskProfileAgain() {
        UUID claimId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        FraudCheck existing = FraudCheck.builder().id(UUID.randomUUID()).claimId(claimId).userId(userId)
                .claimType(ClaimType.CONSULTATION).amount(BigDecimal.valueOf(100))
                .ruleScore(0).finalScore(0).build();
        when(fraudCheckRepository.findByClaimId(claimId)).thenReturn(Optional.of(existing));
        when(riskProfileService.isHighRiskUser(userId)).thenReturn(false);

        service.evaluateClaim(event(ClaimType.CONSULTATION, BigDecimal.valueOf(100), claimId, userId));

        verify(riskProfileService, never()).recordFraudCheck(any(), anyInt(), anyBoolean());

        ArgumentCaptor<FraudCheck> captor = ArgumentCaptor.forClass(FraudCheck.class);
        verify(fraudCheckRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(existing.getId());
    }

    @Test
    void getFraudCheck_throwsNotFound_whenMissing() {
        UUID claimId = UUID.randomUUID();
        when(fraudCheckRepository.findByClaimId(claimId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getFraudCheck(claimId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void reanalyzeClaim_rebuildsEventFromExistingFraudCheck() {
        UUID claimId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        FraudCheck existing = FraudCheck.builder().id(UUID.randomUUID()).claimId(claimId).userId(userId)
                .claimType(ClaimType.HOSPITALIZATION).amount(BigDecimal.valueOf(20000))
                .ruleScore(40).finalScore(40).build();
        when(fraudCheckRepository.findByClaimId(claimId)).thenReturn(Optional.of(existing));
        when(riskProfileService.isHighRiskUser(userId)).thenReturn(false);
        when(aiClientService.chatCompletion(any())).thenReturn("{\"score\": 60, \"explanation\": \"re\", \"flags\": []}");

        service.reanalyzeClaim(claimId);

        ArgumentCaptor<FraudCheck> captor = ArgumentCaptor.forClass(FraudCheck.class);
        verify(fraudCheckRepository).save(captor.capture());
        FraudCheck saved = captor.getValue();
        assertThat(saved.getRuleScore()).isEqualTo(40);
        assertThat(saved.getAiScore()).isEqualTo(60);
        verify(riskProfileService, never()).recordFraudCheck(any(), anyInt(), anyBoolean());
    }
}
