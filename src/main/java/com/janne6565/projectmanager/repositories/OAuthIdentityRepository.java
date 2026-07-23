package com.janne6565.projectmanager.repositories;

import com.janne6565.projectmanager.entities.OAuthIdentityEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthIdentityRepository extends JpaRepository<OAuthIdentityEntity, UUID> {

    Optional<OAuthIdentityEntity> findByProviderAndProviderSubject(
            String provider, String providerSubject);
}
