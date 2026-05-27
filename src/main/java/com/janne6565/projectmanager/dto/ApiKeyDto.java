package com.janne6565.projectmanager.dto;

import java.time.Instant;

public record ApiKeyDto(
        String id,
        String name,
        String prefix,
        Instant createdAt,
        Instant lastUsedAt,
        Boolean active
) {}
