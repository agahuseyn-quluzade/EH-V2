package com.ehi.payment.repository;

import com.ehi.payment.entity.SavedCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavedCardRepository extends JpaRepository<SavedCard, UUID> {

    Optional<SavedCard> findByCardId(String cardId);

    Optional<SavedCard> findFirstByUserIdAndActiveTrueOrderByCreatedAtDesc(UUID userId);

    List<SavedCard> findByUserId(UUID userId);
}
