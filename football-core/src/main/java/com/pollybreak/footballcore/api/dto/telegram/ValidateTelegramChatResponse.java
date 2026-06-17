package com.pollybreak.footballcore.api.dto.telegram;

public record ValidateTelegramChatResponse(
        Long chatId,
        String title,
        boolean valid
) {
}
