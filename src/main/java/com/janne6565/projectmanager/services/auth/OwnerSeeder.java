package com.janne6565.projectmanager.services.auth;

import com.janne6565.projectmanager.entities.AppUser;
import com.janne6565.projectmanager.entities.Role;
import com.janne6565.projectmanager.repositories.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Bootstraps the first {@code OWNER} on startup so a fresh deployment is reachable. Reads the
 * existing {@code spring.security.user.name}/{@code spring.security.user.password} values (fed by
 * {@code SPRING_SECURITY_USER_PASSWORD} in the cluster) so the current deployment keeps working with
 * zero manifest changes. Runs only when no owner exists; idempotent and a no-op once seeded. Never
 * logs the password.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OwnerSeeder implements ApplicationRunner {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${spring.security.user.name:}")
    private String bootstrapUsername;

    @Value("${spring.security.user.password:}")
    private String bootstrapPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.countByRole(Role.OWNER) > 0) {
            return;
        }
        if (!StringUtils.hasText(bootstrapUsername) || !StringUtils.hasText(bootstrapPassword)) {
            log.warn(
                    "No OWNER exists and spring.security.user.name/password are not configured — "
                            + "set them (SPRING_SECURITY_USER_PASSWORD) to seed the first owner.");
            return;
        }
        if (userRepository.existsByUsername(bootstrapUsername)) {
            log.warn("Cannot seed bootstrap owner: username '{}' already exists", bootstrapUsername);
            return;
        }
        AppUser owner =
                new AppUser(
                        bootstrapUsername, passwordEncoder.encode(bootstrapPassword), Role.OWNER);
        userRepository.save(owner);
        log.info("Seeded bootstrap OWNER '{}'", owner.getUsername());
    }
}
