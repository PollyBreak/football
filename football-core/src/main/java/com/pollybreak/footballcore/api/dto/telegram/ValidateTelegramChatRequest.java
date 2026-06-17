package com.pollybreak.footballcore.api.dto.telegram;

import jakarta.validation.constraints.NotNull;

public record ValidateTelegramChatRequest(
        @NotNull Long chatId,
        @NotNull Long userId
) {
}
