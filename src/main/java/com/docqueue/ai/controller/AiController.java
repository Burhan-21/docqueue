package com.docqueue.ai.controller;

import com.docqueue.ai.dto.AiRequest;
import com.docqueue.ai.dto.AiResponse;
import com.docqueue.ai.service.AiService;
import com.docqueue.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI Assistant", description = "AI generation endpoints for intelligent assistance")
public class AiController {

    private final AiService aiService;

    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Generate an AI response for a given query")
    public ResponseEntity<ApiResponse<AiResponse>> chat(@Valid @RequestBody AiRequest request) {
        return ResponseEntity.ok(ApiResponse.success(aiService.generateResponse(request)));
    }
}
