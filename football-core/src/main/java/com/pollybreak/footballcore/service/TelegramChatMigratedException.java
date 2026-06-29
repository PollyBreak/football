package com.pollybreak.footballcore.service;

public class TelegramChatMigratedException extends RuntimeException {

    private final Long migratedChatId;

    public TelegramChatMigratedException(Long migratedChatId) {
        super("Telegram chat was migrated to supergroup chat: " + migratedChatId);
        this.migratedChatId = migratedChatId;
    }

    public Long getMigratedChatId() {
        return migratedChatId;
    }
}
