package com.ehi.notification.repository;

import com.ehi.notification.entity.UserContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserContactRepository extends JpaRepository<UserContact, UUID> {

    Optional<UserContact> findByUserId(UUID userId);
}
