package com.janne6565.projectmanager.controllers;

import com.janne6565.projectmanager.dto.ApiKeyDto;
import com.janne6565.projectmanager.dto.CreateApiKeyRequest;
import com.janne6565.projectmanager.dto.GeneratedApiKeyResponse;
import com.janne6565.projectmanager.services.ApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @PostMapping
    public ResponseEntity<GeneratedApiKeyResponse> generateKey(@RequestBody CreateApiKeyRequest request) {
        return ResponseEntity.ok(apiKeyService.generateKey(request.name()));
    }

    @GetMapping
    public ResponseEntity<List<ApiKeyDto>> listKeys() {
        return ResponseEntity.ok(apiKeyService.listKeys());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokeKey(@PathVariable String id) {
        boolean revoked = apiKeyService.revokeKey(id);
        return revoked ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
