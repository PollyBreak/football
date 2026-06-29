package com.pollybreak.footballcore.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.pollybreak.footballcore.api.dto.telegram.ContributionStatusResponse;
import com.pollybreak.footballcore.api.dto.telegram.StartRegistrationResponse;
import com.pollybreak.footballcore.config.TelegramBotProperties;
import com.pollybreak.footballcore.domain.entity.AppUser;
import com.pollybreak.footballcore.domain.entity.GameSession;
import com.pollybreak.footballcore.domain.entity.Player;
import com.pollybreak.footballcore.domain.entity.SessionContribution;
import com.pollybreak.footballcore.domain.entity.SessionPlayer;
import com.pollybreak.footballcore.repository.AppUserRepository;
import com.pollybreak.footballcore.repository.GameSessionRepository;
import com.pollybreak.footballcore.repository.PlayerRepository;
import com.pollybreak.footballcore.repository.SessionContributionRepository;
import com.pollybreak.footballcore.repository.SessionPlayerRepository;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TelegramContributionService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final String DEFAULT_APP_URL = "https://t.me/football_pozitiv_bot";

    private final TelegramBotApiClient telegramBotApiClient;
    private final TelegramBotProperties telegramBotProperties;
    private final GameSessionRepository gameSessionRepository;
    private final AppUserRepository appUserRepository;
    private final PlayerRepository playerRepository;
    private final SessionPlayerRepository sessionPlayerRepository;
    private final SessionContributionRepository sessionContributionRepository;
    private final TelegramRegistrationService telegramRegistrationService;

    @Transactional
    public StartRegistrationResponse startContributionCollection(Long sessionId, Long userId) {
        GameSession session = getSession(sessionId);
        if (session.getTelegramChatId() == null) {
            throw new IllegalArgumentException("Telegram chat is not linked to this session");
        }
        telegramRegistrationService.validateSessionChat(session, userId);

        String text = buildContributionMessage(session);
        JsonNode result;
        if (session.getTelegramContributionMessageId() != null) {
            result = telegramBotApiClient.tryEditMessageText(
                    session.getTelegramChatId(),
                    session.getTelegramContributionMessageId(),
                    text,
                    contributionKeyboard(session.getId())
            );
            if (result != null) {
                return new StartRegistrationResponse(
                        session.getTelegramChatId(),
                        session.getTelegramContributionMessageId(),
                        buildMessageUrl(session)
                );
            }
        }

        {
            result = telegramBotApiClient.sendMessage(session.getTelegramChatId(), text, contributionKeyboard(session.getId()));
            session.setTelegramContributionMessageId(result.path("message_id").asLong());
        }
        return new StartRegistrationResponse(
                session.getTelegramChatId(),
                session.getTelegramContributionMessageId(),
                buildMessageUrl(session)
        );
    }

    @Transactional
    public void handleCallback(JsonNode callbackQuery) {
        String callbackId = callbackQuery.path("id").asText();
        Long telegramId = callbackQuery.path("from").path("id").asLong();
        ContributionCallbackData callbackData = parseCallbackData(callbackQuery.path("data").asText(""));
        if (callbackData == null) {
            telegramBotApiClient.answerCallbackQuery(callbackId, "Неизвестная команда", true, null);
            return;
        }

        Optional<Player> player = appUserRepository.findByTelegramId(telegramId)
                .flatMap(user -> playerRepository.findByUserId(user.getId()));
        if (player.isEmpty() || sessionPlayerRepository
                .findBySessionIdAndPlayerIdAndActiveTrue(callbackData.sessionId(), player.get().getId())
                .isEmpty()) {
            telegramBotApiClient.answerCallbackQuery(callbackId, "Сначала зарегистрируйтесь на эту игру", false, null);
            return;
        }

        applyContributionStatus(callbackData.sessionId(), player.get(), callbackData.paid());
        answerCallbackSafely(callbackId, "Готово, список взносов обновлен");
        refreshContributionMessageSafely(callbackData.sessionId());
    }

    @Transactional
    public void refreshContributionMessage(Long sessionId) {
        GameSession session = getSession(sessionId);
        if (session.getTelegramChatId() == null || session.getTelegramContributionMessageId() == null) {
            return;
        }
        try {
            telegramBotApiClient.editMessageTextIgnoringNotModified(
                    session.getTelegramChatId(),
                    session.getTelegramContributionMessageId(),
                    buildContributionMessage(session),
                    contributionKeyboard(session.getId())
            );
        } catch (TelegramChatMigratedException exception) {
            session.setTelegramChatId(exception.getMigratedChatId());
            telegramBotApiClient.editMessageTextIgnoringNotModified(
                    session.getTelegramChatId(),
                    session.getTelegramContributionMessageId(),
                    buildContributionMessage(session),
                    contributionKeyboard(session.getId())
            );
        }
    }

    @Transactional(readOnly = true)
    public List<ContributionStatusResponse> getContributionStatuses(Long sessionId) {
        ContributionRoster roster = contributionRoster(sessionId);
        Set<Long> paidPlayerIds = roster.paidPlayers().stream()
                .map(Player::getId)
                .collect(Collectors.toSet());
        return sessionPlayerRepository.findAllBySessionIdAndActiveTrueOrderByJoinedAtAscIdAsc(sessionId)
                .stream()
                .map(SessionPlayer::getPlayer)
                .map(player -> new ContributionStatusResponse(
                        player.getId(),
                        playerName(player),
                        paidPlayerIds.contains(player.getId())
                ))
                .toList();
    }

    @Transactional
    public List<ContributionStatusResponse> updateContributionStatuses(
            Long sessionId,
            Long userId,
            List<Long> playerIds,
            Boolean paid
    ) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (playerIds == null || playerIds.isEmpty()) {
            throw new IllegalArgumentException("playerIds must not be empty");
        }
        boolean paidValue = Boolean.TRUE.equals(paid);
        Player currentPlayer = playerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Player profile not found for user: " + userId));
        if (sessionPlayerRepository.findBySessionIdAndPlayerIdAndActiveTrue(sessionId, currentPlayer.getId()).isEmpty()) {
            throw new IllegalArgumentException("Only session players can update contribution statuses");
        }

        List<Long> uniquePlayerIds = playerIds.stream()
                .distinct()
                .toList();
        if (!paidValue && (uniquePlayerIds.size() != 1 || !uniquePlayerIds.get(0).equals(currentPlayer.getId()))) {
            throw new IllegalArgumentException("Players can only cancel their own contribution status");
        }
        for (Long playerId : uniquePlayerIds) {
            Player player = playerRepository.findById(playerId)
                    .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));
            if (sessionPlayerRepository.findBySessionIdAndPlayerIdAndActiveTrue(sessionId, playerId).isEmpty()) {
                throw new IllegalArgumentException("Player is not registered for this session: " + playerId);
            }
            applyContributionStatus(sessionId, player, paidValue);
        }

        refreshContributionMessageSafely(sessionId);
        return getContributionStatuses(sessionId);
    }

    @Transactional(readOnly = true)
    public void sendContributionReminder(Long sessionId, Integer hoursBefore) {
        GameSession session = getSession(sessionId);
        if (session.getTelegramChatId() == null) {
            return;
        }

        ContributionRoster roster = contributionRoster(session.getId());
        if (roster.unpaidPlayers().isEmpty()) {
            return;
        }

        List<String> lines = new ArrayList<>();
        lines.add("❗<b>Напоминание по взносам</b>");
        lines.add("");
        lines.add("До игры осталось " + hoursBefore + " " + hourWord(hoursBefore) + ".");
        if (session.getFeeAmount() != null || (session.getFeeRecipient() != null && !session.getFeeRecipient().isBlank())) {
            StringBuilder contributionLine = new StringBuilder("💰 ");
            if (session.getFeeAmount() != null) {
                contributionLine.append(session.getFeeAmount()).append(" тг");
            }
            if (session.getFeeRecipient() != null && !session.getFeeRecipient().isBlank()) {
                if (session.getFeeAmount() != null) {
                    contributionLine.append(" / ");
                }
                contributionLine.append(escape(session.getFeeRecipient()));
            }
            lines.add(contributionLine.toString());
            lines.add("");
        }
        lines.add("Не сдали: " + roster.unpaidPlayers().stream()
                .map(this::playerMention)
                .collect(Collectors.joining(", ")));
        lines.add("");
        lines.add("Пожалуйста, сдайте взнос и ПРОГОЛОСУЙТЕ в предыдущем сообщении по <a href=\""
                + escapeAttribute(sessionAppUrl(session.getId()))
                + "\">игре</a> 💰");

        telegramBotApiClient.sendMessage(session.getTelegramChatId(), String.join("\n", lines));
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void refreshContributionMessageAfterRosterChange(SessionRosterChangedEvent event) {
        try {
            refreshContributionMessageAfterCommit(event.sessionId());
        } catch (RuntimeException exception) {
            log.warn("Failed to refresh Telegram contribution message for session {}", event.sessionId(), exception);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void refreshContributionMessageAfterCommit(Long sessionId) {
        refreshContributionMessage(sessionId);
    }

    private void refreshContributionMessageSafely(Long sessionId) {
        try {
            refreshContributionMessage(sessionId);
        } catch (RuntimeException exception) {
            log.warn("Failed to refresh Telegram contribution message for session {}", sessionId, exception);
        }
    }

    private void answerCallbackSafely(String callbackId, String message) {
        try {
            telegramBotApiClient.answerCallbackQuery(callbackId, message, false, null);
        } catch (RuntimeException exception) {
            log.warn("Failed to answer Telegram contribution callback {}", callbackId, exception);
        }
    }

    private void applyContributionStatus(Long sessionId, Player player, boolean paid) {
        GameSession session = getSession(sessionId);
        SessionContribution contribution = sessionContributionRepository.findBySessionIdAndPlayerId(sessionId, player.getId())
                .orElseGet(() -> {
                    SessionContribution created = new SessionContribution();
                    created.setSession(session);
                    created.setPlayer(player);
                    return created;
                });
        contribution.setPaid(paid);
        sessionContributionRepository.save(contribution);
    }

    private String buildContributionMessage(GameSession session) {
        ContributionRoster roster = contributionRoster(session.getId());
        List<Player> paidPlayers = roster.paidPlayers();
        List<Player> unpaidPlayers = roster.unpaidPlayers();

        List<String> lines = new ArrayList<>();
        lines.add("❗<b>Собираем взносы</b> 💰");
        lines.add(headerLine(session));
        if (session.getFeeAmount() != null) {
            lines.add("💰 " + session.getFeeAmount() + " тенге");
        }
        if (session.getFeeRecipient() != null && !session.getFeeRecipient().isBlank()) {
            lines.add("💸 " + escape(session.getFeeRecipient()));
        }
        lines.add("");
        lines.add("Просим всех оперативно сдавать взносы!");
        lines.add("<a href=\"" + escapeAttribute(sessionAppUrl(session.getId())) + "\">Сдать взнос в приложении (можно сдать за себя/других игроков)</a>");
        lines.add("Опрос работает только для тех, кто <a href=\"" + escapeAttribute(sessionAppUrl(session.getId())) + "\">зарегистрирован на игру</a>.");
        lines.add("");
        lines.add("✅ Сдали (" + paidPlayers.size() + "/" + maxPlayersLabel(session) + "): " + code(names(paidPlayers)));
        lines.add("❌ Не сдали: " + code(names(unpaidPlayers)));
        return String.join("\n", lines);
    }

    private ContributionRoster contributionRoster(Long sessionId) {
        List<SessionPlayer> activePlayers = sessionPlayerRepository.findAllBySessionIdAndActiveTrueOrderByJoinedAtAscIdAsc(sessionId);
        Map<Long, Player> activePlayersById = activePlayers.stream()
                .collect(Collectors.toMap(item -> item.getPlayer().getId(), SessionPlayer::getPlayer, (left, right) -> left));
        List<Player> paidPlayers = sessionContributionRepository.findAllBySessionIdAndPaidTrueOrderByUpdatedAtAscIdAsc(sessionId)
                .stream()
                .map(SessionContribution::getPlayer)
                .filter(player -> activePlayersById.containsKey(player.getId()))
                .toList();
        Set<Long> paidPlayerIds = paidPlayers.stream().map(Player::getId).collect(Collectors.toSet());
        List<Player> unpaidPlayers = activePlayers.stream()
                .map(SessionPlayer::getPlayer)
                .filter(player -> !paidPlayerIds.contains(player.getId()))
                .toList();
        return new ContributionRoster(paidPlayers, unpaidPlayers);
    }

    private List<List<Map<String, String>>> contributionKeyboard(Long sessionId) {
        return List.of(
                List.of(Map.of("text", "✅ Сдал", "callback_data", "c:" + sessionId + ":PAID")),
                List.of(Map.of("text", "❌ Не сдал", "callback_data", "c:" + sessionId + ":UNPAID"))
        );
    }

    private String headerLine(GameSession session) {
        List<String> parts = new ArrayList<>();
        parts.add(session.getSessionDate().toString());
        parts.add(session.getSessionTime().format(TIME_FORMAT));
        if (session.getSessionDurationMinutes() != null) {
            parts.add(session.getSessionDurationMinutes() + " минут");
        }
        if (session.getLocation() != null && !session.getLocation().isBlank()) {
            parts.add(session.getLocation());
        }
        parts.add(session.getTitle());
        return escape(String.join(" | ", parts));
    }

    private String names(List<Player> players) {
        if (players == null || players.isEmpty()) {
            return "-";
        }
        return players.stream()
                .map(this::playerName)
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

    private String playerMention(Player player) {
        AppUser user = player.getUser();
        if (user != null && user.getUsername() != null && !user.getUsername().isBlank()) {
            return "@" + escape(user.getUsername());
        }
        String name = playerName(player);
        if (name == null || name.isBlank()) {
            name = "Игрок";
        }
        if (user != null && user.getTelegramId() != null) {
            return "<a href=\"tg://user?id=" + user.getTelegramId() + "\">" + escape(name) + "</a>";
        }
        return escape(name);
    }

    private String hourWord(Integer hours) {
        int value = hours == null ? 0 : Math.abs(hours);
        int lastTwo = value % 100;
        int last = value % 10;
        if (lastTwo >= 11 && lastTwo <= 14) {
            return "часов";
        }
        if (last == 1) {
            return "час";
        }
        if (last >= 2 && last <= 4) {
            return "часа";
        }
        return "часов";
    }

    private String maxPlayersLabel(GameSession session) {
        return session.getMaxPlayers() == null ? "∞" : session.getMaxPlayers().toString();
    }

    private String sessionAppUrl(Long sessionId) {
        String appUrl = telegramBotProperties.getAppUrl();
        String safeAppUrl = appUrl == null || appUrl.isBlank() ? DEFAULT_APP_URL : appUrl;
        String separator = safeAppUrl.contains("?") ? "&" : "?";
        return safeAppUrl + separator + "startapp=session_" + sessionId;
    }

    private String buildMessageUrl(GameSession session) {
        String chat = session.getTelegramChatId() == null ? "" : session.getTelegramChatId().toString();
        if (!chat.startsWith("-100") || session.getTelegramContributionMessageId() == null) {
            return null;
        }
        return "https://t.me/c/" + chat.substring(4) + "/" + session.getTelegramContributionMessageId();
    }

    private ContributionCallbackData parseCallbackData(String data) {
        String[] parts = data.split(":");
        if (parts.length != 3 || !"c".equals(parts[0])) {
            return null;
        }
        try {
            if (!"PAID".equals(parts[2]) && !"UNPAID".equals(parts[2])) {
                return null;
            }
            return new ContributionCallbackData(Long.parseLong(parts[1]), "PAID".equals(parts[2]));
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private GameSession getSession(Long sessionId) {
        return gameSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Game session not found: " + sessionId));
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

    private record ContributionCallbackData(Long sessionId, boolean paid) {
    }

    private record ContributionRoster(List<Player> paidPlayers, List<Player> unpaidPlayers) {
    }
}
