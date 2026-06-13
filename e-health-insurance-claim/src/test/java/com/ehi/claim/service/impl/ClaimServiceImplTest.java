package com.ehi.claim.service.impl;

import com.ehi.claim.dto.request.ReviewClaimRequest;
import com.ehi.claim.dto.request.SubmitClaimRequest;
import com.ehi.claim.dto.response.ClaimDto;
import com.ehi.claim.entity.Claim;
import com.ehi.claim.kafka.ClaimDecisionEventProducer;
import com.ehi.claim.kafka.ClaimSubmittedEventProducer;
import com.ehi.claim.mapper.ClaimEvidenceMapper;
import com.ehi.claim.mapper.ClaimMapper;
import com.ehi.claim.repository.ClaimEvidenceRepository;
import com.ehi.claim.repository.ClaimRepository;
import com.ehi.infra.enums.ClaimStatus;
import com.ehi.infra.enums.ClaimType;
import com.ehi.infra.event.ClaimDecisionEvent;
import com.ehi.infra.event.ClaimSubmittedEvent;
import com.ehi.infra.event.FraudDetectedEvent;
import com.ehi.infra.exception.base.BadRequestException;
import com.ehi.infra.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClaimServiceImplTest {

    @Mock ClaimRepository claimRepository;
    @Mock ClaimEvidenceRepository claimEvidenceRepository;
    @Mock ClaimMapper claimMapper;
    @Mock ClaimEvidenceMapper claimEvidenceMapper;
    @Mock ClaimSubmittedEventProducer claimSubmittedEventProducer;
    @Mock ClaimDecisionEventProducer claimDecisionEventProducer;

    @InjectMocks ClaimServiceImpl claimService;

    private SubmitClaimRequest sampleRequest() {
        return new SubmitClaimRequest(UUID.randomUUID(), ClaimType.HOSPITALIZATION, BigDecimal.valueOf(500), "Surgery");
    }

    private Claim sampleClaim(UUID userId, ClaimStatus status, BigDecimal amount) {
        return Claim.builder()
                .id(UUID.randomUUID())
                .claimNumber("CLM-12345678")
                .userId(userId)
                .policyId(UUID.randomUUID())
                .claimType(ClaimType.HOSPITALIZATION)
                .amount(amount)
                .description("Surgery")
                .status(status)
                .build();
    }

    private ClaimDto toDtoStub(Claim claim) {
        return ClaimDto.builder()
                .id(claim.getId())
                .claimNumber(claim.getClaimNumber())
                .userId(claim.getUserId())
                .status(claim.getStatus())
                .amount(claim.getAmount())
                .approvedAmount(claim.getApprovedAmount())
                .rejectionReason(claim.getRejectionReason())
                .reviewedBy(claim.getReviewedBy())
                .riskScore(claim.getRiskScore())
                .fraudFlags(claim.getFraudFlags())
                .aiExplanation(claim.getAiExplanation())
                .build();
    }


    @Test
    void submitClaim_setsSubmittedStatus_generatesClmNumber_andPublishesEvent() {
        when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(claimMapper.toDto(any(Claim.class))).thenAnswer(invocation -> toDtoStub(invocation.getArgument(0)));

        UUID userId = UUID.randomUUID();
        ClaimDto result = claimService.submitClaim(userId, sampleRequest());

        ArgumentCaptor<Claim> captor = ArgumentCaptor.forClass(Claim.class);
        verify(claimRepository).save(captor.capture());
        Claim saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ClaimStatus.SUBMITTED);
        assertThat(saved.getClaimNumber()).startsWith("CLM-");
        assertThat(saved.getUserId()).isEqualTo(userId);

        assertThat(result.status()).isEqualTo(ClaimStatus.SUBMITTED);

        ArgumentCaptor<ClaimSubmittedEvent> eventCaptor = ArgumentCaptor.forClass(ClaimSubmittedEvent.class);
        verify(claimSubmittedEventProducer).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().userId()).isEqualTo(userId);
        assertThat(eventCaptor.getValue().amount()).isEqualTo(BigDecimal.valueOf(500));
    }


    @Test
    void getClaimById_throwsNotFound_whenNonOwnerNonPrivileged() {
        UUID ownerId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Claim claim = sampleClaim(ownerId, ClaimStatus.SUBMITTED, BigDecimal.valueOf(500));
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> claimService.getClaimById(claim.getId(), requesterId, false))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void uploadEvidence_throwsNotFound_whenNonOwnerNonPrivileged() {
        UUID ownerId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Claim claim = sampleClaim(ownerId, ClaimStatus.SUBMITTED, BigDecimal.valueOf(500));
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));

        var file = mock(org.springframework.web.multipart.MultipartFile.class);

        assertThatThrownBy(() -> claimService.uploadEvidence(claim.getId(), requesterId, false, file))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getClaimById_allowsPrivilegedRequester_regardlessOfOwner() {
        UUID ownerId = UUID.randomUUID();
        UUID agentId = UUID.randomUUID();
        Claim claim = sampleClaim(ownerId, ClaimStatus.SUBMITTED, BigDecimal.valueOf(500));
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(claimMapper.toDto(claim)).thenReturn(toDtoStub(claim));

        ClaimDto result = claimService.getClaimById(claim.getId(), agentId, true);

        assertThat(result.userId()).isEqualTo(ownerId);
    }


    @Test
    void reviewClaim_throwsBadRequest_whenStatusNotReviewable() {
        Claim claim = sampleClaim(UUID.randomUUID(), ClaimStatus.APPROVED, BigDecimal.valueOf(500));
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));

        var request = new ReviewClaimRequest(ClaimStatus.APPROVED, BigDecimal.valueOf(500), null);

        assertThatThrownBy(() -> claimService.reviewClaim(claim.getId(), UUID.randomUUID(), request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void reviewClaim_throwsBadRequest_whenApprovedAmountMissing() {
        Claim claim = sampleClaim(UUID.randomUUID(), ClaimStatus.SUBMITTED, BigDecimal.valueOf(500));
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));

        var request = new ReviewClaimRequest(ClaimStatus.APPROVED, null, null);

        assertThatThrownBy(() -> claimService.reviewClaim(claim.getId(), UUID.randomUUID(), request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void reviewClaim_throwsBadRequest_whenRejectionReasonMissing() {
        Claim claim = sampleClaim(UUID.randomUUID(), ClaimStatus.SUBMITTED, BigDecimal.valueOf(500));
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));

        var request = new ReviewClaimRequest(ClaimStatus.REJECTED, null, null);

        assertThatThrownBy(() -> claimService.reviewClaim(claim.getId(), UUID.randomUUID(), request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void reviewClaim_throwsBadRequest_whenApprovedAmountExceedsClaimAmount() {
        Claim claim = sampleClaim(UUID.randomUUID(), ClaimStatus.SUBMITTED, BigDecimal.valueOf(500));
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));

        var request = new ReviewClaimRequest(ClaimStatus.APPROVED, BigDecimal.valueOf(10000), null);

        assertThatThrownBy(() -> claimService.reviewClaim(claim.getId(), UUID.randomUUID(), request))
                .isInstanceOf(BadRequestException.class);

        verify(claimRepository, never()).save(any(Claim.class));
    }

    @Test
    void reviewClaim_throwsBadRequest_whenApprovedAmountNotPositive() {
        Claim claim = sampleClaim(UUID.randomUUID(), ClaimStatus.SUBMITTED, BigDecimal.valueOf(500));
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));

        var request = new ReviewClaimRequest(ClaimStatus.APPROVED, BigDecimal.ZERO, null);

        assertThatThrownBy(() -> claimService.reviewClaim(claim.getId(), UUID.randomUUID(), request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void reviewClaim_approves_setsFields_andPublishesDecisionEvent() {
        Claim claim = sampleClaim(UUID.randomUUID(), ClaimStatus.SUBMITTED, BigDecimal.valueOf(500));
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(claimMapper.toDto(any(Claim.class))).thenAnswer(invocation -> toDtoStub(invocation.getArgument(0)));

        UUID reviewerId = UUID.randomUUID();
        var request = new ReviewClaimRequest(ClaimStatus.APPROVED, BigDecimal.valueOf(400), null);

        ClaimDto result = claimService.reviewClaim(claim.getId(), reviewerId, request);

        assertThat(result.status()).isEqualTo(ClaimStatus.APPROVED);
        assertThat(result.approvedAmount()).isEqualTo(BigDecimal.valueOf(400));
        assertThat(result.reviewedBy()).isEqualTo(reviewerId);

        ArgumentCaptor<ClaimDecisionEvent> eventCaptor = ArgumentCaptor.forClass(ClaimDecisionEvent.class);
        verify(claimDecisionEventProducer).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().decision()).isEqualTo(ClaimStatus.APPROVED);
        assertThat(eventCaptor.getValue().reviewedBy()).isEqualTo(reviewerId);
    }


    @Test
    void applyFraudResult_lowScore_autoApproves_withFullAmount_andPublishesEvent() {
        Claim claim = sampleClaim(UUID.randomUUID(), ClaimStatus.SUBMITTED, BigDecimal.valueOf(500));
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FraudDetectedEvent event = FraudDetectedEvent.builder()
                .claimId(claim.getId()).userId(claim.getUserId())
                .riskScore(20).flags(List.of()).aiExplanation("low risk").build();

        claimService.applyFraudResult(event);

        ArgumentCaptor<Claim> captor = ArgumentCaptor.forClass(Claim.class);
        verify(claimRepository).save(captor.capture());
        Claim saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ClaimStatus.APPROVED);
        assertThat(saved.getApprovedAmount()).isEqualTo(claim.getAmount());
        assertThat(saved.getRiskScore()).isEqualTo(20);

        ArgumentCaptor<ClaimDecisionEvent> eventCaptor = ArgumentCaptor.forClass(ClaimDecisionEvent.class);
        verify(claimDecisionEventProducer).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().decision()).isEqualTo(ClaimStatus.APPROVED);
        assertThat(eventCaptor.getValue().reviewedBy()).isNull();
    }

    @Test
    void applyFraudResult_midScore_setsUnderReview_andPublishesNoDecisionEvent() {
        Claim claim = sampleClaim(UUID.randomUUID(), ClaimStatus.SUBMITTED, BigDecimal.valueOf(500));
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FraudDetectedEvent event = FraudDetectedEvent.builder()
                .claimId(claim.getId()).userId(claim.getUserId())
                .riskScore(55).flags(List.of("unusual_amount")).aiExplanation("medium risk").build();

        claimService.applyFraudResult(event);

        ArgumentCaptor<Claim> captor = ArgumentCaptor.forClass(Claim.class);
        verify(claimRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ClaimStatus.UNDER_REVIEW);

        verify(claimDecisionEventProducer, never()).publish(any(ClaimDecisionEvent.class));
    }

    @Test
    void applyFraudResult_highScore_autoRejects_andPublishesEvent() {
        Claim claim = sampleClaim(UUID.randomUUID(), ClaimStatus.SUBMITTED, BigDecimal.valueOf(500));
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FraudDetectedEvent event = FraudDetectedEvent.builder()
                .claimId(claim.getId()).userId(claim.getUserId())
                .riskScore(85).flags(List.of("duplicate_claim")).aiExplanation("high risk").build();

        claimService.applyFraudResult(event);

        ArgumentCaptor<Claim> captor = ArgumentCaptor.forClass(Claim.class);
        verify(claimRepository).save(captor.capture());
        Claim saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ClaimStatus.REJECTED);
        assertThat(saved.getRejectionReason()).isNotBlank();

        ArgumentCaptor<ClaimDecisionEvent> eventCaptor = ArgumentCaptor.forClass(ClaimDecisionEvent.class);
        verify(claimDecisionEventProducer).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().decision()).isEqualTo(ClaimStatus.REJECTED);
    }

    @Test
    void applyFraudResult_skipsNonSubmittedClaim_noOverwrite() {
        Claim claim = sampleClaim(UUID.randomUUID(), ClaimStatus.UNDER_REVIEW, BigDecimal.valueOf(500));
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));

        FraudDetectedEvent event = FraudDetectedEvent.builder()
                .claimId(claim.getId()).userId(claim.getUserId())
                .riskScore(10).flags(List.of()).aiExplanation("low risk").build();

        claimService.applyFraudResult(event);

        verify(claimRepository, never()).save(any(Claim.class));
        verify(claimDecisionEventProducer, never()).publish(any(ClaimDecisionEvent.class));
    }
}
