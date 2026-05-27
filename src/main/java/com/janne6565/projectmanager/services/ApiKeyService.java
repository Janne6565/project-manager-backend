package com.janne6565.projectmanager.services;

import com.janne6565.projectmanager.dto.ApiKeyDto;
import com.janne6565.projectmanager.dto.GeneratedApiKeyResponse;
import com.janne6565.projectmanager.entities.ApiKey;
import com.janne6565.projectmanager.repositories.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public GeneratedApiKeyResponse generateKey(String name) {
        byte[] rawBytes = new byte[32];
        secureRandom.nextBytes(rawBytes);
        String token = "pm_" + Base64.getUrlEncoder().withoutPadding().encodeToString(rawBytes);
        String prefix = token.substring(0, Math.min(12, token.length()));
        String hash = sha256(token);

        ApiKey key = ApiKey.builder()
                .name(name)
                .keyHash(hash)
                .prefix(prefix)
                .build();
        ApiKey saved = apiKeyRepository.save(key);

        return new GeneratedApiKeyResponse(saved.getId(), saved.getName(), saved.getPrefix(), token, saved.getCreatedAt());
    }

    public boolean validate(String rawKey) {
        String hash = sha256(rawKey);
        return apiKeyRepository.findByKeyHashAndActiveTrue(hash)
                .map(key -> {
                    key.setLastUsedAt(Instant.now());
                    apiKeyRepository.save(key);
                    return true;
                })
                .orElse(false);
    }

    public List<ApiKeyDto> listKeys() {
        return apiKeyRepository.findAll().stream()
                .map(k -> new ApiKeyDto(k.getId(), k.getName(), k.getPrefix(), k.getCreatedAt(), k.getLastUsedAt(), k.getActive()))
                .toList();
    }

    public boolean revokeKey(String id) {
        return apiKeyRepository.findById(id)
                .map(key -> {
                    key.setActive(false);
                    apiKeyRepository.save(key);
                    return true;
                })
                .orElse(false);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
