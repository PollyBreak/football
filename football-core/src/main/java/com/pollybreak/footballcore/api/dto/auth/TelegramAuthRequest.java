package com.pollybreak.footballcore.api.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record TelegramAuthRequest(
        @NotBlank String initData
) {
}
