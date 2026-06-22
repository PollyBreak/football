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
import com.pollybreak.footballcore.domain.entity.SessionPlayer;
import com.pollybreak.footballcore.domain.entity.SessionWaitlistEntry;
import com.pollybreak.footballcore.domain.entity.TelegramPendingRegistration;
import com.pollybreak.footballcore.domain.enums.SessionFormatType;
import com.pollybreak.footballcore.domain.enums.SessionRegistrationStatus;
import com.pollybreak.footballcore.domain.enums.PlayerPosition;
import com.pollybreak.footballcore.repository.AppUserRepository;
import com.pollybreak.footballcore.repository.GameSessionRepository;
import com.pollybreak.footballcore.repository.PlayerRepository;
import com.pollybreak.footballcore.repository.SessionRegistrationRepository;
import com.pollybreak.footballcore.repository.SessionPlayerRepository;
import com.pollybreak.footballcore.repository.SessionWaitlistRepository;
import com.pollybreak.footballcore.repository.TelegramPendingRegistrationRepository;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TelegramRegistrationService {

    private static final Locale RU = Locale.forLanguageTag("ru-RU");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final String DEFAULT_APP_URL = "https://t.me/football_pozitiv_bot/join";

    private final TelegramBotApiClient telegramBotApiClient;
    private final TelegramBotProperties telegramBotProperties;
    private final GameSessionRepository gameSessionRepository;
    private final AppUserRepository appUserRepository;
    private final PlayerRepository playerRepository;
    private final SessionRegistrationRepository sessionRegistrationRepository;
    private final SessionPlayerRepository sessionPlayerRepository;
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
            result = telegramBotApiClient.editMessageTextIgnoringNotModified(
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
        answerCallbackSafely(callbackId, "Готово, список обновлен");
        refreshRegistrationMessageSafely(callbackData.sessionId());
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
        telegramBotApiClient.editMessageTextIgnoringNotModified(
                session.getTelegramChatId(),
                session.getTelegramRegistrationMessageId(),
                buildAnnouncement(session),
                registrationKeyboard(session.getId())
        );
    }

    private void refreshRegistrationMessageSafely(Long sessionId) {
        try {
            refreshRegistrationMessage(sessionId);
        } catch (RuntimeException exception) {
            log.warn("Failed to refresh Telegram registration message for session {}", sessionId, exception);
        }
    }

    private void answerCallbackSafely(String callbackId, String message) {
        try {
            telegramBotApiClient.answerCallbackQuery(callbackId, message, false, null);
        } catch (RuntimeException exception) {
            log.warn("Failed to answer Telegram registration callback {}", callbackId, exception);
        }
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
        Set<Long> registeredPlayerIds = groups.values().stream()
                .flatMap(List::stream)
                .map(item -> item.getPlayer().getId())
                .collect(Collectors.toSet());
        List<Player> activePlayersWithoutRegistration = sessionPlayerRepository
                .findAllBySessionIdAndActiveTrueOrderByJoinedAtAscIdAsc(session.getId())
                .stream()
                .map(SessionPlayer::getPlayer)
                .filter(player -> !registeredPlayerIds.contains(player.getId()))
                .filter(player -> !waitlistPlayerIds.contains(player.getId()))
                .toList();
        long activePlayersCount = sessionPlayerRepository.countBySessionIdAndActiveTrue(session.getId());
        long expectedPlayersCount = session.getMaxPlayers() == null ? activePlayersCount : session.getMaxPlayers();
        Integer durationMinutes = session.getSessionDurationMinutes();
        LocalTime endTime = durationMinutes == null ? null : session.getSessionTime().plusMinutes(durationMinutes);

        List<String> lines = new ArrayList<>();
        lines.add("<b>Открыта регистрация на футбол!</b>");
        lines.add(headerLine(session, durationMinutes));
        String sessionUrl = sessionAppUrl(session.getId());
        if (sessionUrl != null && !sessionUrl.isBlank()) {
            lines.add("<a href=\"" + escapeAttribute(sessionUrl) + "\">Открыть игру в приложении</a>");
        }
        lines.add("");
        lines.add(timeLine(session, durationMinutes, endTime));
        lines.add(locationLine(session));
        lines.add("⚽️ " + expectedPlayersCount + " " + participantsWord(expectedPlayersCount) + playerFormatSuffix(session));
        lines.add("🏆 Формат " + escape(formatLabel(session.getFormatType())));
        if (session.getFeeAmount() != null) {
            lines.add("💰 <b>" + session.getFeeAmount() + "</b> тенге");
        }
        if (session.getNotes() != null && !session.getNotes().isBlank()) {
            lines.add("");
            lines.add(escape(session.getNotes()));
        }
        lines.add("");
        lines.add("❕Перед регистрацией необходимо зарегистрироваться в <a href=\"" + escapeAttribute(registrationAppUrl()) + "\">приложении</a>. Если вы зарегистрированы, просто нажмите на одну из кнопок.");
        lines.add("");
        lines.add("Участники (" + activePlayersCount + "/" + maxPlayersLabel(session) + "):");
        lines.add("✅ <i>Записался, иду 100%</i>: " + code(namesPlain(groups.get(SessionRegistrationStatus.GOING), activePlayersWithoutRegistration, waitlistPlayerIds)));
        lines.add("❓ <i>Записался, но под вопросом</i>: " + code(namesPlain(groups.get(SessionRegistrationStatus.MAYBE), waitlistPlayerIds)));
        lines.add("❌ <i>В этот раз без меня</i>: " + code(namesPlain(groups.get(SessionRegistrationStatus.OUT))));
        lines.add("⌛ <i>Записался, в очереди</i>: " + code(waitlistNamesPlain(waitlist)));
        return String.join("\n", lines);
    }

    private List<List<Map<String, String>>> registrationKeyboard(Long sessionId) {
        return List.of(
                List.of(Map.of("text", "✅ Записался, иду 100%", "callback_data", "r:" + sessionId + ":GOING")),
                List.of(Map.of("text", "❓ Записался, но под вопросом", "callback_data", "r:" + sessionId + ":MAYBE")),
                List.of(Map.of("text", "❌ В этот раз без меня", "callback_data", "r:" + sessionId + ":OUT"))
        );
    }

    private String headerLine(GameSession session, Integer durationMinutes) {
        List<String> parts = new ArrayList<>();
        parts.add(session.getSessionDate().toString());
        parts.add(session.getSessionTime().format(TIME_FORMAT));
        if (durationMinutes != null) {
            parts.add(durationMinutes + " минут");
        }
        parts.add(nullToDash(session.getTitle()));
        return escape(String.join(" | ", parts));
    }

    private String timeLine(GameSession session, Integer durationMinutes, LocalTime endTime) {
        String value = "🕒 " + humanDateLine(session) + ", " + session.getSessionTime().format(TIME_FORMAT);
        if (endTime != null && durationMinutes != null) {
            value += " - " + endTime.format(TIME_FORMAT) + " (" + durationMinutes + " мин.)";
        }
        return escape(value);
    }

    private String locationLine(GameSession session) {
        List<String> parts = new ArrayList<>();
        parts.add(nullToDash(session.getLocation()));
        if (session.getLocationAddress() != null && !session.getLocationAddress().isBlank()) {
            parts.add(session.getLocationAddress());
        }
        String locationText = escape(String.join(" - ", parts));
        if (session.getLocationUrl() == null || session.getLocationUrl().isBlank()) {
            return "📍 " + locationText;
        }
        return "📍 " + locationText + " - <a href=\"" + escapeAttribute(session.getLocationUrl()) + "\">Адрес на карте</a>";
    }

    private String playerFormatSuffix(GameSession session) {
        return session.getPlayerFormat() == null || session.getPlayerFormat().isBlank()
                ? ""
                : ", " + escape(session.getPlayerFormat());
    }

    private String maxPlayersLabel(GameSession session) {
        return session.getMaxPlayers() == null ? "∞" : session.getMaxPlayers().toString();
    }

    private String registrationAppUrl() {
        String appUrl = telegramBotProperties.getAppUrl();
        return appUrl == null || appUrl.isBlank() ? DEFAULT_APP_URL : appUrl;
    }

    private String sessionAppUrl(Long sessionId) {
        String appUrl = telegramBotProperties.getAppUrl();
        String safeAppUrl = appUrl == null || appUrl.isBlank() ? DEFAULT_APP_URL : appUrl;
        String separator = safeAppUrl.contains("?") ? "&" : "?";
        return safeAppUrl + separator + "startapp=session_" + sessionId;
    }

    private String names(List<SessionRegistration> registrations) {
        if (registrations == null || registrations.isEmpty()) {
            return "-";
        }
        return registrations.stream()
                .map(item -> escape(playerName(item.getPlayer())))
                .collect(Collectors.joining(", "));
    }

    private String namesPlain(List<SessionRegistration> registrations) {
        if (registrations == null || registrations.isEmpty()) {
            return "-";
        }
        return registrations.stream()
                .map(item -> playerNameWithPosition(item.getPlayer()))
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

    private String namesPlain(List<SessionRegistration> registrations, Set<Long> excludedPlayerIds) {
        if (registrations == null || registrations.isEmpty()) {
            return "-";
        }
        String value = registrations.stream()
                .filter(item -> !excludedPlayerIds.contains(item.getPlayer().getId()))
                .map(item -> playerNameWithPosition(item.getPlayer()))
                .collect(Collectors.joining(", "));
        return value.isBlank() ? "-" : value;
    }

    private String namesPlain(List<SessionRegistration> registrations, List<Player> fallbackPlayers, Set<Long> excludedPlayerIds) {
        List<String> values = new ArrayList<>();
        if (registrations != null) {
            values.addAll(registrations.stream()
                    .filter(item -> !excludedPlayerIds.contains(item.getPlayer().getId()))
                    .map(item -> playerNameWithPosition(item.getPlayer()))
                    .toList());
        }
        if (fallbackPlayers != null) {
            values.addAll(fallbackPlayers.stream()
                    .filter(player -> !excludedPlayerIds.contains(player.getId()))
                    .map(this::playerNameWithPosition)
                    .toList());
        }
        String value = String.join(", ", values);
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

    private String waitlistNamesPlain(List<SessionWaitlistEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return "-";
        }
        return entries.stream()
                .map(entry -> playerNameWithPosition(entry.getPlayer()))
                .collect(Collectors.joining(", "));
    }

    private String playerName(Player player) {
        if (player.getUser() != null
                && player.getUser().getDisplayName() != null
                && !player.getUser().getDisplayName().isBlank()) {
            return player.getUser().getDisplayName();
        }
        if (player.getNickname() != null && !player.getNickname().isBlank()) {
            return player.getNickname();
        }
        return java.util.Arrays.asList(player.getFirstName(), player.getLastName()).stream()
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" "));
    }

    private String playerNameWithPosition(Player player) {
        String name = playerName(player);
        return player.getDefaultPosition() == null
                ? name
                : name + " (" + positionLabel(player.getDefaultPosition()) + ")";
    }

    private String humanDate(GameSession session) {
        String month = session.getSessionDate().getMonth().getDisplayName(TextStyle.FULL, RU);
        String dayOfWeek = session.getSessionDate().getDayOfWeek().getDisplayName(TextStyle.FULL, RU);
        return session.getSessionDate().getDayOfMonth() + " " + month + " (" + dayOfWeek + ")";
    }

    private String humanDateLine(GameSession session) {
        String month = session.getSessionDate().getMonth().getDisplayName(TextStyle.FULL, RU);
        String dayOfWeek = session.getSessionDate().getDayOfWeek().getDisplayName(TextStyle.FULL, RU);
        String capitalizedDayOfWeek = dayOfWeek.substring(0, 1).toUpperCase(RU) + dayOfWeek.substring(1);
        return capitalizedDayOfWeek + ", " + session.getSessionDate().getDayOfMonth() + " " + month;
    }

    private String formatLabel(SessionFormatType formatType) {
        return switch (formatType) {
            case ROUND_ROBIN -> "круговой, по турам";
            case KNOCKOUT -> "на вылет";
            case KING_OF_THE_HILL -> "царь горы";
            case CUSTOM -> "другой";
        };
    }

    private String positionLabel(PlayerPosition position) {
        return switch (position) {
            case GOALKEEPER -> "ВР";
            case DEFENDER -> "ЗАЩ";
            case MIDFIELDER -> "ПЗ";
            case FORWARD -> "НАП";
            case UNIVERSAL -> "УН";
        };
    }

    private String participantsWord(long count) {
        long lastTwo = count % 100;
        long last = count % 10;
        if (lastTwo >= 11 && lastTwo <= 14) {
            return "участников";
        }
        if (last == 1) {
            return "участник";
        }
        if (last >= 2 && last <= 4) {
            return "участника";
        }
        return "участников";
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

    private String code(String value) {
        return "<code>" + escape(value) + "</code>";
    }

    private record CallbackData(Long sessionId, SessionRegistrationStatus status) {
    }
}
