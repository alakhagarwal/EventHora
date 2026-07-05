package com.eventHora.backend.repository;

import com.eventHora.backend.model.SystemUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SystemUserRepository extends JpaRepository<SystemUser, UUID> {
    Optional<SystemUser> findByEmail(String email);
    boolean existsByEmail(String email);
}
