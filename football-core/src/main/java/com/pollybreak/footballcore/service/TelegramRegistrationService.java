package com.pollybreak.footballcore.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.pollybreak.footballcore.api.dto.session.AddPlayerToSessionRequest;
import com.pollybreak.footballcore.api.dto.telegram.StartRegistrationResponse;
import com.pollybreak.footballcore.api.dto.telegram.ValidateTelegramChatResponse;
import com.pollybreak.footballcore.config.TelegramBotProperties;
import com.pollybreak.footballcore.domain.entity.AppUser;
import com.pollybreak.footballcore.domain.entity.GameSession;
import com.pollybreak.footballcore.domain.entity.Player;
import com.pollybreak.footballcore.domain.entity.SessionRegistration;
import com.pollybreak.footballcore.domain.entity.SessionWaitlistEntry;
import com.pollybreak.footballcore.domain.entity.TelegramPendingRegistration;
import com.pollybreak.footballcore.domain.enums.SessionFormatType;
import com.pollybreak.footballcore.domain.enums.SessionRegistrationStatus;
import com.pollybreak.footballcore.repository.AppUserRepository;
import com.pollybreak.footballcore.repository.GameSessionRepository;
import com.pollybreak.footballcore.repository.PlayerRepository;
import com.pollybreak.footballcore.repository.SessionRegistrationRepository;
import com.pollybreak.footballcore.repository.SessionWaitlistRepository;
import com.pollybreak.footballcore.repository.TelegramPendingRegistrationRepository;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TelegramRegistrationService {

    private static final Locale RU = Locale.forLanguageTag("ru-RU");

    private final TelegramBotApiClient telegramBotApiClient;
    private final TelegramBotProperties telegramBotProperties;
    private final GameSessionRepository gameSessionRepository;
    private final AppUserRepository appUserRepository;
    private final PlayerRepository playerRepository;
    private final SessionRegistrationRepository sessionRegistrationRepository;
    private final SessionWaitlistRepository sessionWaitlistRepository;
    private final TelegramPendingRegistrationRepository pendingRegistrationRepository;
    private final SessionPlayerService sessionPlayerService;

    public ValidateTelegramChatResponse validateChat(Long chatId, Long userId) {
        AppUser user = getUser(userId);
        if (user.getTelegramId() == null) {
            throw new IllegalArgumentException("User is not linked to Telegram");
        }

        JsonNode me = telegramBotApiClient.getMe();
        Long botTelegramId = me.path("id").asLong();
        ensureMember(chatId, user.getTelegramId(), "User is not a member of this Telegram chat");
        ensureMember(chatId, botTelegramId, "Bot is not a member of this Telegram chat");

        JsonNode chat = telegramBotApiClient.getChat(chatId);
        String title = chat.path("title").asText(null);
        return new ValidateTelegramChatResponse(chatId, title, true);
    }

    @Transactional
    public StartRegistrationResponse startRegistration(Long sessionId, Long userId) {
        GameSession session = getSession(sessionId);
        if (session.getTelegramChatId() == null) {
            throw new IllegalArgumentException("Telegram chat is not linked to this session");
        }
        validateChat(session.getTelegramChatId(), userId);

        String text = buildAnnouncement(session);
        JsonNode result;
        if (session.getTelegramRegistrationMessageId() == null) {
            result = telegramBotApiClient.sendMessage(session.getTelegramChatId(), text, registrationKeyboard(session.getId()));
            session.setTelegramRegistrationMessageId(result.path("message_id").asLong());
        } else {
            result = telegramBotApiClient.editMessageText(
                    session.getTelegramChatId(),
                    session.getTelegramRegistrationMessageId(),
                    text,
                    registrationKeyboard(session.getId())
            );
        }
        return new StartRegistrationResponse(
                session.getTelegramChatId(),
                session.getTelegramRegistrationMessageId(),
                buildMessageUrl(session)
        );
    }

    @Transactional
    public void handleCallback(JsonNode callbackQuery) {
        String callbackId = callbackQuery.path("id").asText();
        Long telegramId = callbackQuery.path("from").path("id").asLong();
        String data = callbackQuery.path("data").asText("");
        CallbackData callbackData = parseCallbackData(data);
        if (callbackData == null) {
            telegramBotApiClient.answerCallbackQuery(callbackId, "Неизвестная команда", true, null);
            return;
        }

        Optional<Player> player = appUserRepository.findByTelegramId(telegramId)
                .flatMap(user -> playerRepository.findByUserId(user.getId()));
        if (player.isEmpty()) {
            savePendingRegistration(telegramId, callbackData.sessionId(), callbackData.status());
            telegramBotApiClient.answerCallbackQuery(
                    callbackId,
                    "Сначала заполните профиль игрока в приложении. После регистрации заявка применится автоматически.",
                    true,
                    buildAppUrl(callbackData.sessionId(), callbackData.status())
            );
            return;
        }

        applyRegistration(callbackData.sessionId(), player.get(), callbackData.status());
        refreshRegistrationMessage(callbackData.sessionId());
        telegramBotApiClient.answerCallbackQuery(callbackId, "Готово, список обновлен", false, null);
    }

    @Transactional
    public void applyPendingRegistrations(Player player) {
        AppUser user = player.getUser();
        if (user == null || user.getTelegramId() == null) {
            return;
        }

        List<TelegramPendingRegistration> pending = pendingRegistrationRepository
                .findAllByTelegramIdOrderByCreatedAtAscIdAsc(user.getTelegramId());
        for (TelegramPendingRegistration item : pending) {
            applyRegistration(item.getSession().getId(), player, item.getStatus());
            refreshRegistrationMessage(item.getSession().getId());
        }
        pendingRegistrationRepository.deleteAllByTelegramId(user.getTelegramId());
    }

    @Transactional
    public void applyRegistration(Long sessionId, Player player, SessionRegistrationStatus status) {
        GameSession session = getSession(sessionId);
        SessionRegistration registration = sessionRegistrationRepository.findBySessionIdAndPlayerId(sessionId, player.getId())
                .orElseGet(() -> {
                    SessionRegistration created = new SessionRegistration();
                    created.setSession(session);
                    created.setPlayer(player);
                    return created;
                });
        registration.setStatus(status);
        sessionRegistrationRepository.save(registration);

        if (status == SessionRegistrationStatus.GOING || status == SessionRegistrationStatus.MAYBE) {
            if (sessionPlayerService.findActiveSessionPlayer(sessionId, player.getId()).isEmpty()
                    && sessionPlayerService.findActiveWaitlistEntry(sessionId, player.getId()).isEmpty()) {
                sessionPlayerService.join(sessionId, new AddPlayerToSessionRequest(player.getId(), player.getDefaultPosition()));
            }
            return;
        }

        sessionPlayerService.removePlayerIfActive(sessionId, player.getId());
        sessionPlayerService.leaveWaitlistIfActive(sessionId, player.getId());
    }

    @Transactional
    public void refreshRegistrationMessage(Long sessionId) {
        GameSession session = getSession(sessionId);
        if (session.getTelegramChatId() == null || session.getTelegramRegistrationMessageId() == null) {
            return;
        }
        telegramBotApiClient.editMessageText(
                session.getTelegramChatId(),
                session.getTelegramRegistrationMessageId(),
                buildAnnouncement(session),
                registrationKeyboard(session.getId())
        );
    }

    private void savePendingRegistration(Long telegramId, Long sessionId, SessionRegistrationStatus status) {
        GameSession session = getSession(sessionId);
        TelegramPendingRegistration pending = pendingRegistrationRepository.findByTelegramIdAndSessionId(telegramId, sessionId)
                .orElseGet(() -> {
                    TelegramPendingRegistration created = new TelegramPendingRegistration();
                    created.setTelegramId(telegramId);
                    created.setSession(session);
                    return created;
                });
        pending.setStatus(status);
        pendingRegistrationRepository.save(pending);
    }

    private void ensureMember(Long chatId, Long telegramId, String message) {
        String status = telegramBotApiClient.getChatMember(chatId, telegramId).path("status").asText("");
        if (status.isBlank() || "left".equals(status) || "kicked".equals(status)) {
            throw new IllegalArgumentException(message);
        }
    }

    private String buildAnnouncement(GameSession session) {
        Map<SessionRegistrationStatus, List<SessionRegistration>> groups = sessionRegistrationRepository
                .findAllBySessionIdOrderByUpdatedAtAscIdAsc(session.getId())
                .stream()
                .sorted(Comparator.comparing(SessionRegistration::getId))
                .collect(Collectors.groupingBy(
                        SessionRegistration::getStatus,
                        () -> new java.util.EnumMap<>(SessionRegistrationStatus.class),
                        Collectors.toList()
                ));
        List<SessionWaitlistEntry> waitlist = sessionWaitlistRepository
                .findAllBySessionIdAndActiveTrueOrderByQueuedAtAscIdAsc(session.getId());
        Set<Long> waitlistPlayerIds = waitlist.stream()
                .map(entry -> entry.getPlayer().getId())
                .collect(Collectors.toSet());

        List<String> lines = new ArrayList<>();
        lines.add("<b>Открыта регистрация на футбол!</b>");
        lines.add(escape(session.getSessionDate().toString()) + " | " + escape(session.getSessionTime().toString().substring(0, 5)) + " | " + escape(nullToDash(session.getTitle())));
        lines.add("Время: " + escape(session.getSessionTime().toString().substring(0, 5) + " " + humanDate(session)));
        if (session.getLocationUrl() != null && !session.getLocationUrl().isBlank()) {
            lines.add("Адрес: <a href=\"" + escapeAttribute(session.getLocationUrl()) + "\">" + escape(nullToDash(session.getLocation())) + "</a>");
        } else {
            lines.add("Адрес: " + escape(nullToDash(session.getLocation())));
        }
        if (session.getFeeAmount() != null) {
            String recipient = session.getFeeRecipient() == null || session.getFeeRecipient().isBlank()
                    ? ""
                    : " -> " + session.getFeeRecipient();
            lines.add("Взнос: " + escape(session.getFeeAmount() + " тенге" + recipient));
        }
        lines.add("Формат: " + escape(formatLabel(session.getFormatType())));
        if (session.getNotes() != null && !session.getNotes().isBlank()) {
            lines.add("Описание: " + escape(session.getNotes()));
        }
        lines.add("");
        lines.add("<b>Участники:</b>");
        lines.add("✅ Записался, иду 100%: " + names(groups.get(SessionRegistrationStatus.GOING), waitlistPlayerIds));
        lines.add("❓ Записался, но под вопросом: " + names(groups.get(SessionRegistrationStatus.MAYBE), waitlistPlayerIds));
        lines.add("❌ В этот раз без меня: " + names(groups.get(SessionRegistrationStatus.OUT)));
        lines.add("⌛ Записался, в очереди: " + waitlistNames(waitlist));
        return String.join("\n", lines);
    }

    private List<List<Map<String, String>>> registrationKeyboard(Long sessionId) {
        return List.of(
                List.of(Map.of("text", "✅ Записался, иду 100%", "callback_data", "r:" + sessionId + ":GOING")),
                List.of(Map.of("text", "❓ Записался, но под вопросом", "callback_data", "r:" + sessionId + ":MAYBE")),
                List.of(Map.of("text", "❌ В этот раз без меня", "callback_data", "r:" + sessionId + ":OUT"))
        );
    }

    private String names(List<SessionRegistration> registrations) {
        if (registrations == null || registrations.isEmpty()) {
            return "-";
        }
        return registrations.stream()
                .map(item -> escape(playerName(item.getPlayer())))
                .collect(Collectors.joining(", "));
    }

    private String names(List<SessionRegistration> registrations, Set<Long> excludedPlayerIds) {
        if (registrations == null || registrations.isEmpty()) {
            return "-";
        }
        String value = registrations.stream()
                .filter(item -> !excludedPlayerIds.contains(item.getPlayer().getId()))
                .map(item -> escape(playerName(item.getPlayer())))
                .collect(Collectors.joining(", "));
        return value.isBlank() ? "-" : value;
    }

    private String waitlistNames(List<SessionWaitlistEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return "-";
        }
        return entries.stream()
                .map(entry -> escape(playerName(entry.getPlayer())))
                .collect(Collectors.joining(", "));
    }

    private String playerName(Player player) {
        if (player.getNickname() != null && !player.getNickname().isBlank()) {
            return player.getNickname();
        }
        return java.util.Arrays.asList(player.getFirstName(), player.getLastName()).stream()
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" "));
    }

    private String humanDate(GameSession session) {
        String month = session.getSessionDate().getMonth().getDisplayName(TextStyle.FULL, RU);
        String dayOfWeek = session.getSessionDate().getDayOfWeek().getDisplayName(TextStyle.FULL, RU);
        return session.getSessionDate().getDayOfMonth() + " " + month + " (" + dayOfWeek + ")";
    }

    private String formatLabel(SessionFormatType formatType) {
        return switch (formatType) {
            case ROUND_ROBIN -> "по кругу";
            case KNOCKOUT -> "на вылет";
            case KING_OF_THE_HILL -> "царь горы";
            case CUSTOM -> "другой";
        };
    }

    private String buildMessageUrl(GameSession session) {
        if (telegramBotProperties.getUsername() == null || telegramBotProperties.getUsername().isBlank()) {
            return null;
        }
        String chat = session.getTelegramChatId() == null ? "" : session.getTelegramChatId().toString();
        if (!chat.startsWith("-100") || session.getTelegramRegistrationMessageId() == null) {
            return null;
        }
        return "https://t.me/c/" + chat.substring(4) + "/" + session.getTelegramRegistrationMessageId();
    }

    private String buildAppUrl(Long sessionId, SessionRegistrationStatus status) {
        String appUrl = telegramBotProperties.getAppUrl();
        if (appUrl == null || appUrl.isBlank()) {
            return null;
        }
        String separator = appUrl.contains("?") ? "&" : "?";
        return appUrl + separator + "startapp=join_" + sessionId + "_" + status.name();
    }

    private CallbackData parseCallbackData(String data) {
        String[] parts = data.split(":");
        if (parts.length != 3 || !"r".equals(parts[0])) {
            return null;
        }
        try {
            return new CallbackData(Long.parseLong(parts[1]), SessionRegistrationStatus.valueOf(parts[2]));
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private GameSession getSession(Long sessionId) {
        return gameSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Game session not found: " + sessionId));
    }

    private AppUser getUser(Long userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String escape(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String escapeAttribute(String value) {
        return escape(value).replace("\"", "&quot;");
    }

    private record CallbackData(Long sessionId, SessionRegistrationStatus status) {
    }
}
