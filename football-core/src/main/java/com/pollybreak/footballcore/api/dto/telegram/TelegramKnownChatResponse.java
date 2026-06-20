package com.pollybreak.footballcore.api.dto.telegram;

import com.pollybreak.footballcore.domain.entity.TelegramKnownChat;

public record TelegramKnownChatResponse(
        Long chatId,
        String title,
        String username,
        String chatType
) {

    public static TelegramKnownChatResponse fromEntity(TelegramKnownChat chat) {
        return new TelegramKnownChatResponse(
                chat.getChatId(),
                chat.getTitle(),
                chat.getUsername(),
                chat.getChatType()
        );
    }
}
