package com.janne6565.projectmanager.dto;

import java.time.Instant;

public record GeneratedApiKeyResponse(
        String id,
        String name,
        String prefix,
        String key,
        Instant createdAt
) {}
