package com.janne6565.projectmanager.repositories;

import com.janne6565.projectmanager.entities.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, String> {
    Optional<ApiKey> findByKeyHashAndActiveTrue(String keyHash);
}
