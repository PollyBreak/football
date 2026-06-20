package com.pollybreak.footballcore.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.pollybreak.footballcore.api.dto.telegram.TelegramKnownChatResponse;
import com.pollybreak.footballcore.domain.entity.AppUser;
import com.pollybreak.footballcore.domain.entity.TelegramKnownChat;
import com.pollybreak.footballcore.repository.AppUserRepository;
import com.pollybreak.footballcore.repository.TelegramKnownChatRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TelegramKnownChatService {

    private static final List<String> SUPPORTED_CHAT_TYPES = List.of("group", "supergroup");

    private final TelegramKnownChatRepository telegramKnownChatRepository;
    private final AppUserRepository appUserRepository;
    private final TelegramBotApiClient telegramBotApiClient;

    @Transactional
    public void registerChat(JsonNode chatNode) {
        if (chatNode == null || chatNode.isMissingNode() || chatNode.isNull()) {
            return;
        }

        long chatId = chatNode.path("id").asLong(0L);
        String chatType = chatNode.path("type").asText("");
        if (chatId == 0L || !SUPPORTED_CHAT_TYPES.contains(chatType)) {
            return;
        }

        TelegramKnownChat chat = telegramKnownChatRepository.findById(chatId)
                .orElseGet(TelegramKnownChat::new);
        chat.setChatId(chatId);
        chat.setChatType(chatType);
        chat.setTitle(resolveChatTitle(chatNode));
        chat.setUsername(readOptional(chatNode.path("username").asText(null)));
        chat.setActive(true);
        telegramKnownChatRepository.save(chat);
    }

    @Transactional
    public void markChatMembership(JsonNode chatNode, String membershipStatus) {
        if (chatNode == null || chatNode.isMissingNode() || chatNode.isNull()) {
            return;
        }

        long chatId = chatNode.path("id").asLong(0L);
        String chatType = chatNode.path("type").asText("");
        if (chatId == 0L || !SUPPORTED_CHAT_TYPES.contains(chatType)) {
            return;
        }

        TelegramKnownChat chat = telegramKnownChatRepository.findById(chatId)
                .orElseGet(TelegramKnownChat::new);
        chat.setChatId(chatId);
        chat.setChatType(chatType);
        chat.setTitle(resolveChatTitle(chatNode));
        chat.setUsername(readOptional(chatNode.path("username").asText(null)));
        chat.setActive(!isInactiveMembership(membershipStatus));
        telegramKnownChatRepository.save(chat);
    }

    public List<TelegramKnownChatResponse> getAvailableChatsForUser(Long userId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        if (user.getTelegramId() == null) {
            throw new IllegalArgumentException("User is not linked to Telegram");
        }

        return telegramKnownChatRepository
                .findAllByActiveTrueAndChatTypeInOrderByTitleAscChatIdAsc(SUPPORTED_CHAT_TYPES)
                .stream()
                .filter(chat -> isUserMember(chat.getChatId(), user.getTelegramId()))
                .map(TelegramKnownChatResponse::fromEntity)
                .toList();
    }

    private boolean isUserMember(Long chatId, Long telegramUserId) {
        try {
            String status = telegramBotApiClient.getChatMember(chatId, telegramUserId).path("status").asText("");
            return !isInactiveMembership(status);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean isInactiveMembership(String status) {
        return "left".equalsIgnoreCase(status) || "kicked".equalsIgnoreCase(status);
    }

    private String resolveChatTitle(JsonNode chatNode) {
        String title = readOptional(chatNode.path("title").asText(null));
        if (title != null) {
            return title;
        }

        String firstName = readOptional(chatNode.path("first_name").asText(null));
        String lastName = readOptional(chatNode.path("last_name").asText(null));
        if (firstName == null && lastName == null) {
            return null;
        }
        return (Optional.ofNullable(firstName).orElse("") + " " + Optional.ofNullable(lastName).orElse("")).trim();
    }

    private String readOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
