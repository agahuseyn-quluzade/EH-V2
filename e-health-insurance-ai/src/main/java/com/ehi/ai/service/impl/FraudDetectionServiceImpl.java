package com.ehi.ai.service.impl;

import com.ehi.ai.client.OpenAiMessage;
import com.ehi.ai.dto.response.FraudAiResponse;
import com.ehi.ai.entity.FraudCheck;
import com.ehi.ai.kafka.FraudDetectedEventProducer;
import com.ehi.ai.mapper.FraudCheckMapper;
import com.ehi.ai.repository.FraudCheckRepository;
import com.ehi.ai.service.AiClientService;
import com.ehi.ai.service.FraudDetectionService;
import com.ehi.ai.service.RiskProfileService;
import com.ehi.infra.enums.ClaimType;
import com.ehi.infra.event.ClaimSubmittedEvent;
import com.ehi.infra.event.FraudDetectedEvent;
import com.ehi.infra.exception.NotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionServiceImpl implements FraudDetectionService {

    private static final int AI_TRIGGER_THRESHOLD = 40;
    private static final int HIGH_RISK_THRESHOLD = 70;
    private static final int REPEAT_HIGH_RISK_USER_SCORE = 20;
    private static final int AMOUNT_ABOVE_THRESHOLD_SCORE = 40;
    private static final int AMOUNT_FAR_ABOVE_THRESHOLD_SCORE = 20;

    private final FraudCheckRepository fraudCheckRepository;
    private final FraudCheckMapper fraudCheckMapper;
    private final AiClientService aiClientService;
    private final RiskProfileService riskProfileService;
    private final FraudDetectedEventProducer fraudDetectedEventProducer;
    private final ObjectMapper objectMapper;

    @Override
    public FraudAiResponse evaluateClaim(ClaimSubmittedEvent event) {
        List<String> flags = new ArrayList<>();
        int ruleScore = computeRuleScore(event, flags);

        Integer aiScore = null;
        String aiExplanation = null;

        if (ruleScore >= AI_TRIGGER_THRESHOLD) {
            try {
                AiAssessment assessment = callAiAssessment(event, ruleScore, flags);
                aiScore = assessment.score();
                aiExplanation = assessment.explanation();
                if (assessment.flags() != null) {
                    flags.addAll(assessment.flags());
                }
            } catch (Exception e) {
                log.warn("OpenAI fraud assessment failed for claim {}, falling back to rule-only score", event.claimId(), e);
            }
        }

        int finalScore = aiScore != null
                ? Math.round(ruleScore * 0.4f + aiScore * 0.6f)
                : ruleScore;

        Optional<FraudCheck> existing = fraudCheckRepository.findByClaimId(event.claimId());
        boolean isNewCheck = existing.isEmpty();

        FraudCheck fraudCheck = existing.orElseGet(() -> FraudCheck.builder()
                .claimId(event.claimId())
                .userId(event.userId())
                .build());

        fraudCheck.setClaimType(event.claimType());
        fraudCheck.setAmount(event.amount());
        fraudCheck.setRuleScore(ruleScore);
        fraudCheck.setAiScore(aiScore);
        fraudCheck.setFinalScore(finalScore);
        fraudCheck.setFlags(flags);
        fraudCheck.setAiExplanation(aiExplanation);

        fraudCheck = fraudCheckRepository.save(fraudCheck);

        if (isNewCheck) {
            riskProfileService.recordFraudCheck(event.userId(), finalScore, finalScore >= HIGH_RISK_THRESHOLD);
        }

        fraudDetectedEventProducer.publish(FraudDetectedEvent.builder()
                .claimId(fraudCheck.getClaimId())
                .userId(fraudCheck.getUserId())
                .riskScore(fraudCheck.getFinalScore())
                .flags(fraudCheck.getFlags())
                .aiExplanation(fraudCheck.getAiExplanation())
                .build());

        return fraudCheckMapper.toDto(fraudCheck);
    }

    @Override
    public FraudAiResponse getFraudCheck(UUID claimId) {
        return fraudCheckMapper.toDto(fraudCheckRepository.findByClaimId(claimId)
                .orElseThrow(() -> new NotFoundException("FraudCheck", claimId)));
    }

    @Override
    public FraudAiResponse reanalyzeClaim(UUID claimId) {
        FraudCheck existing = fraudCheckRepository.findByClaimId(claimId)
                .orElseThrow(() -> new NotFoundException("FraudCheck", claimId));

        return evaluateClaim(ClaimSubmittedEvent.builder()
                .claimId(existing.getClaimId())
                .userId(existing.getUserId())
                .claimType(existing.getClaimType())
                .amount(existing.getAmount())
                .build());
    }

    private int computeRuleScore(ClaimSubmittedEvent event, List<String> flags) {
        int score = 0;
        BigDecimal threshold = thresholdFor(event.claimType());

        if (event.amount().compareTo(threshold) > 0) {
            score += AMOUNT_ABOVE_THRESHOLD_SCORE;
            flags.add("AMOUNT_ABOVE_TYPE_THRESHOLD");
        }

        if (event.amount().compareTo(threshold.multiply(BigDecimal.valueOf(2))) > 0) {
            score += AMOUNT_FAR_ABOVE_THRESHOLD_SCORE;
            flags.add("AMOUNT_FAR_ABOVE_TYPE_THRESHOLD");
        }

        if (riskProfileService.isHighRiskUser(event.userId())) {
            score += REPEAT_HIGH_RISK_USER_SCORE;
            flags.add("REPEAT_HIGH_RISK_USER");
        }

        return Math.min(score, 100);
    }

    private BigDecimal thresholdFor(ClaimType claimType) {
        return switch (claimType) {
            case HOSPITALIZATION -> BigDecimal.valueOf(10000);
            case MEDICATION -> BigDecimal.valueOf(2000);
            case DENTAL -> BigDecimal.valueOf(1500);
            case CONSULTATION -> BigDecimal.valueOf(500);
        };
    }

    private AiAssessment callAiAssessment(ClaimSubmittedEvent event, int ruleScore, List<String> ruleFlags) throws JsonProcessingException {
        String prompt = """
                You are a health insurance fraud analyst. Assess the following claim and respond ONLY with JSON \
                in this exact format: {"score": <0-100 integer>, "explanation": "<short reason>", "flags": ["<flag>", ...]}.

                Claim type: %s
                Claim amount: %s
                Rule-engine pre-score: %d
                Rule-engine flags: %s
                """.formatted(event.claimType(), event.amount(), ruleScore, ruleFlags);

        String content = aiClientService.chatCompletion(List.of(
                new OpenAiMessage("system", "You are a fraud detection assistant for a health insurance company."),
                new OpenAiMessage("user", prompt)
        ));

        // Strip markdown code fences (e.g. ```json ... ```) that some models add
        content = content.trim().replaceAll("(?s)^```[a-z]*\\s*", "").replaceAll("```\\s*$", "").trim();

        return objectMapper.readValue(content, AiAssessment.class);
    }

    private record AiAssessment(int score, String explanation, List<String> flags) {
    }
}
