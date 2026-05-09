package com.pollybreak.footballcore.api.controller;

import com.pollybreak.footballcore.api.dto.auth.TelegramAuthRequest;
import com.pollybreak.footballcore.api.dto.auth.TelegramAuthResponse;
import com.pollybreak.footballcore.service.TelegramAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final TelegramAuthService telegramAuthService;

    @PostMapping("/telegram")
    public TelegramAuthResponse authenticateTelegram(@Valid @RequestBody TelegramAuthRequest request) {
        return telegramAuthService.authenticate(request);
    }
}
