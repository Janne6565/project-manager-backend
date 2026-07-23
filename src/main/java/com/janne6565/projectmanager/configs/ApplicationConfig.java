package com.janne6565.projectmanager.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Core beans. Authentication now flows through the app's own services (see {@code services/auth}),
 * so the previous in-memory {@code UserDetailsService}/{@code DaoAuthenticationProvider} wiring is
 * gone; only the shared password encoder remains.
 */
@Configuration
public class ApplicationConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
