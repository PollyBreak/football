package com.pollybreak.footballcore.api.dto.telegram;

public record StartRegistrationResponse(
        Long chatId,
        Long messageId,
        String messageUrl
) {
}
