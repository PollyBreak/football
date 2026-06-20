package com.pollybreak.footballcore.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.pollybreak.footballcore.api.dto.telegram.ContributionReminderRequest;
import com.pollybreak.footballcore.api.dto.telegram.ContributionReminderResponse;
import com.pollybreak.footballcore.api.dto.telegram.ContributionStatusResponse;
import com.pollybreak.footballcore.api.dto.telegram.StartRegistrationRequest;
import com.pollybreak.footballcore.api.dto.telegram.StartRegistrationResponse;
import com.pollybreak.footballcore.api.dto.telegram.TelegramKnownChatResponse;
import com.pollybreak.footballcore.api.dto.telegram.ValidateTelegramChatRequest;
import com.pollybreak.footballcore.api.dto.telegram.ValidateTelegramChatResponse;
import com.pollybreak.footballcore.service.SessionContributionReminderService;
import com.pollybreak.footballcore.service.TelegramContributionService;
import com.pollybreak.footballcore.service.TelegramKnownChatService;
import com.pollybreak.footballcore.service.TelegramRegistrationService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TelegramController {

    private final TelegramRegistrationService telegramRegistrationService;
    private final TelegramContributionService telegramContributionService;
    private final SessionContributionReminderService sessionContributionReminderService;
    private final TelegramKnownChatService telegramKnownChatService;

    @GetMapping("/api/telegram/chats")
    public List<TelegramKnownChatResponse> getAvailableChats(@RequestParam Long userId) {
        return telegramKnownChatService.getAvailableChatsForUser(userId);
    }

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

    @PostMapping("/api/sessions/{sessionId}/contributions/start")
    public StartRegistrationResponse startContributionCollection(
            @PathVariable Long sessionId,
            @Valid @RequestBody StartRegistrationRequest request
    ) {
        return telegramContributionService.startContributionCollection(sessionId, request.userId());
    }

    @GetMapping("/api/sessions/{sessionId}/contributions/statuses")
    public List<ContributionStatusResponse> getContributionStatuses(@PathVariable Long sessionId) {
        return telegramContributionService.getContributionStatuses(sessionId);
    }

    @GetMapping("/api/sessions/{sessionId}/contribution-reminders")
    public List<ContributionReminderResponse> getContributionReminders(@PathVariable Long sessionId) {
        return sessionContributionReminderService.getReminders(sessionId);
    }

    @PostMapping("/api/sessions/{sessionId}/contribution-reminders")
    public ContributionReminderResponse createContributionReminder(
            @PathVariable Long sessionId,
            @Valid @RequestBody ContributionReminderRequest request
    ) {
        return sessionContributionReminderService.createReminder(sessionId, request.hoursBefore());
    }

    @DeleteMapping("/api/sessions/{sessionId}/contribution-reminders/{hoursBefore}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteContributionReminder(
            @PathVariable Long sessionId,
            @PathVariable Integer hoursBefore
    ) {
        sessionContributionReminderService.deleteReminder(sessionId, hoursBefore);
    }

    @PostMapping("/api/telegram/webhook")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void webhook(@RequestBody JsonNode update) {
        telegramKnownChatService.registerChat(update.path("message").path("chat"));
        telegramKnownChatService.registerChat(update.path("edited_message").path("chat"));
        telegramKnownChatService.registerChat(update.path("channel_post").path("chat"));
        telegramKnownChatService.registerChat(update.path("edited_channel_post").path("chat"));
        telegramKnownChatService.registerChat(update.path("callback_query").path("message").path("chat"));

        JsonNode myChatMember = update.path("my_chat_member");
        if (!myChatMember.isMissingNode() && !myChatMember.isNull()) {
            telegramKnownChatService.markChatMembership(
                    myChatMember.path("chat"),
                    myChatMember.path("new_chat_member").path("status").asText("")
            );
        }

        JsonNode chatMember = update.path("chat_member");
        if (!chatMember.isMissingNode() && !chatMember.isNull()) {
            telegramKnownChatService.registerChat(chatMember.path("chat"));
        }

        JsonNode callbackQuery = update.path("callback_query");
        if (!callbackQuery.isMissingNode() && !callbackQuery.isNull()) {
            String data = callbackQuery.path("data").asText("");
            if (data.startsWith("c:")) {
                telegramContributionService.handleCallback(callbackQuery);
            } else {
                telegramRegistrationService.handleCallback(callbackQuery);
            }
        }
    }
}
