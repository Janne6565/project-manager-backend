package com.janne6565.projectmanager.repositories;

import com.janne6565.projectmanager.entities.AppUser;
import com.janne6565.projectmanager.entities.Role;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    long countByRole(Role role);
}
