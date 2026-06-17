package com.pollybreak.footballcore.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.pollybreak.footballcore.api.dto.telegram.StartRegistrationRequest;
import com.pollybreak.footballcore.api.dto.telegram.StartRegistrationResponse;
import com.pollybreak.footballcore.api.dto.telegram.ValidateTelegramChatRequest;
import com.pollybreak.footballcore.api.dto.telegram.ValidateTelegramChatResponse;
import com.pollybreak.footballcore.service.TelegramRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TelegramController {

    private final TelegramRegistrationService telegramRegistrationService;

    @PostMapping("/api/sessions/{sessionId}/telegram-chat/validate")
    public ValidateTelegramChatResponse validateChat(
            @PathVariable Long sessionId,
            @Valid @RequestBody ValidateTelegramChatRequest request
    ) {
        return telegramRegistrationService.validateChat(request.chatId(), request.userId());
    }

    @PostMapping("/api/sessions/{sessionId}/registration/start")
    public StartRegistrationResponse startRegistration(
            @PathVariable Long sessionId,
            @Valid @RequestBody StartRegistrationRequest request
    ) {
        return telegramRegistrationService.startRegistration(sessionId, request.userId());
    }

    @PostMapping("/api/telegram/webhook")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void webhook(@RequestBody JsonNode update) {
        JsonNode callbackQuery = update.path("callback_query");
        if (!callbackQuery.isMissingNode() && !callbackQuery.isNull()) {
            telegramRegistrationService.handleCallback(callbackQuery);
        }
    }
}
