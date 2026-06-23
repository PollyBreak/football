package com.pollybreak.footballcore.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.pollybreak.footballcore.api.dto.mvp.SessionMvpCandidateResponse;
import com.pollybreak.footballcore.api.dto.mvp.SessionMvpVotingResponse;
import com.pollybreak.footballcore.config.TelegramBotProperties;
import com.pollybreak.footballcore.domain.entity.AppUser;
import com.pollybreak.footballcore.domain.entity.GameSession;
import com.pollybreak.footballcore.domain.entity.MatchEvent;
import com.pollybreak.footballcore.domain.entity.Player;
import com.pollybreak.footballcore.domain.entity.SessionMvpVote;
import com.pollybreak.footballcore.domain.entity.SessionPlayer;
import com.pollybreak.footballcore.domain.entity.SessionTeamPlayer;
import com.pollybreak.footballcore.domain.enums.MatchEventType;
import com.pollybreak.footballcore.domain.enums.MvpVotingParticipantScope;
import com.pollybreak.footballcore.domain.enums.PlayerPosition;
import com.pollybreak.footballcore.repository.AppUserRepository;
import com.pollybreak.footballcore.repository.GameSessionRepository;
import com.pollybreak.footballcore.repository.MatchEventRepository;
import com.pollybreak.footballcore.repository.PlayerRepository;
import com.pollybreak.footballcore.repository.SessionMvpVoteRepository;
import com.pollybreak.footballcore.repository.SessionPlayerRepository;
import com.pollybreak.footballcore.repository.SessionTeamPlayerRepository;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionMvpVotingService {

    private static final String DEFAULT_APP_URL = "https://t.me/PozitivFootballBot/app";
    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Qyzylorda");
    private static final DateTimeFormatter END_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm, d MMMM", Locale.forLanguageTag("ru"));

    private final GameSessionRepository gameSessionRepository;
    private final SessionPlayerRepository sessionPlayerRepository;
    private final SessionTeamPlayerRepository sessionTeamPlayerRepository;
    private final MatchEventRepository matchEventRepository;
    private final SessionMvpVoteRepository sessionMvpVoteRepository;
    private final AppUserRepository appUserRepository;
    private final PlayerRepository playerRepository;
    private final TelegramBotApiClient telegramBotApiClient;
    private final TelegramBotProperties telegramBotProperties;

    @Transactional
    public void startVotingIfNeeded(GameSession session) {
        if (!session.isMvpVotingEnabled() || session.getMvpVotingStartedAt() != null) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        int durationHours = session.getMvpVotingDurationHours() == null ? 24 : session.getMvpVotingDurationHours();
        session.setMvpVotingStartedAt(now);
        session.setMvpVotingEndsAt(now.plusHours(durationHours));
        gameSessionRepository.save(session);

        if (session.isMvpVotingTelegramEnabled() && session.getTelegramChatId() != null) {
            trySendOrRefreshVotingMessage(session);
        }
    }

    @Transactional
    public void refreshVotingMessageIfNeeded(GameSession session) {
        if (session.isMvpVotingEnabled()
                && session.getMvpVotingStartedAt() != null
                && session.isMvpVotingTelegramEnabled()
                && session.getTelegramChatId() != null) {
            trySendOrRefreshVotingMessage(session);
        }
    }

    @Transactional
    public void sendVotingMessageAgain(Long sessionId) {
        GameSession session = getSession(sessionId);
        if (!session.isMvpVotingEnabled()) {
            throw new IllegalStateException("Голосование за MVP не включено");
        }
        if (session.getMvpVotingStartedAt() == null) {
            throw new IllegalStateException("Голосование за MVP еще не началось");
        }
        if (session.getTelegramChatId() == null) {
            throw new IllegalStateException("Укажите Telegram chat ID для отправки голосования");
        }

        String text = buildVotingMessage(session);
        List<List<Map<String, String>>> keyboard = votingKeyboard(session);
        JsonNode result = telegramBotApiClient.sendMessage(session.getTelegramChatId(), text, keyboard);
        session.setTelegramMvpVotingMessageId(result.path("message_id").asLong());
        gameSessionRepository.save(session);
    }

    public SessionMvpVotingResponse getVoting(Long sessionId, Long userId) {
        GameSession session = getSession(sessionId);
        List<SessionMvpCandidateResponse> candidates = candidates(session);
        List<SessionMvpVote> votes = sessionMvpVoteRepository.findAllBySessionId(sessionId);
        Long selectedPlayerId = userId == null ? null : votes.stream()
                .filter(vote -> Objects.equals(vote.getVoter().getId(), userId))
                .map(vote -> vote.getCandidate().getId())
                .findFirst()
                .orElse(null);

        Eligibility eligibility = eligibility(session, userId);
        return new SessionMvpVotingResponse(
                session.getId(),
                session.isMvpVotingEnabled(),
                session.getMvpVotingStartedAt() != null,
                isVotingFinished(session),
                session.getMvpVotingStartedAt(),
                session.getMvpVotingEndsAt(),
                session.getMvpVotingParticipantScope(),
                eligibility.canVote(),
                eligibility.reason(),
                selectedPlayerId,
                candidates,
                winners(candidates)
        );
    }

    @Transactional
    public SessionMvpVotingResponse vote(Long sessionId, Long userId, Long playerId) {
        GameSession session = getSession(sessionId);
        Eligibility eligibility = eligibility(session, userId);
        if (!eligibility.canVote()) {
            throw new IllegalStateException(eligibility.reason());
        }
        if (isVotingFinished(session)) {
            throw new IllegalStateException("Голосование уже завершено");
        }
        if (sessionPlayerRepository.findBySessionIdAndPlayerIdAndActiveTrue(sessionId, playerId).isEmpty()) {
            throw new IllegalArgumentException("Игрок не участвует в этой сессии");
        }

        AppUser voter = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        Player candidate = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));
        SessionMvpVote vote = sessionMvpVoteRepository.findBySessionIdAndVoterId(sessionId, userId)
                .orElseGet(() -> {
                    SessionMvpVote created = new SessionMvpVote();
                    created.setSession(session);
                    created.setVoter(voter);
                    return created;
                });
        vote.setCandidate(candidate);
        sessionMvpVoteRepository.save(vote);

        if (session.isMvpVotingTelegramEnabled() && session.getTelegramChatId() != null) {
            trySendOrRefreshVotingMessage(session);
        }
        return getVoting(sessionId, userId);
    }

    @Scheduled(fixedDelayString = "${app.mvp-voting.fixed-delay-ms:60000}")
    @Transactional
    public void announceExpiredVotingResults() {
        for (GameSession session : gameSessionRepository.findMvpVotingResultAnnouncementCandidates()) {
            if (session.isMvpVotingTelegramEnabled() && session.getTelegramChatId() != null) {
                try {
                    telegramBotApiClient.sendMessage(session.getTelegramChatId(), buildFinalMessage(session));
                } catch (RuntimeException ignored) {
                    continue;
                }
            }
            session.setTelegramMvpResultSentAt(OffsetDateTime.now());
            gameSessionRepository.save(session);
        }
    }

    private void trySendOrRefreshVotingMessage(GameSession session) {
        try {
            sendOrRefreshVotingMessage(session);
        } catch (RuntimeException ignored) {
            // MVP voting state must stay saved even if Telegram is temporarily unavailable.
        }
    }

    private void sendOrRefreshVotingMessage(GameSession session) {
        String text = buildVotingMessage(session);
        List<List<Map<String, String>>> keyboard = votingKeyboard(session);
        if (session.getTelegramMvpVotingMessageId() == null) {
            JsonNode result = telegramBotApiClient.sendMessage(session.getTelegramChatId(), text, keyboard);
            session.setTelegramMvpVotingMessageId(result.path("message_id").asLong());
            gameSessionRepository.save(session);
            return;
        }
        telegramBotApiClient.editMessageTextIgnoringNotModified(
                session.getTelegramChatId(),
                session.getTelegramMvpVotingMessageId(),
                text,
                keyboard
        );
    }

    private List<List<Map<String, String>>> votingKeyboard(GameSession session) {
        return List.of(List.of(Map.of(
                "text", "Проголосовать",
                "url", mvpPageUrl(session.getId())
        )));
    }

    private String buildVotingMessage(GameSession session) {
        List<SessionMvpCandidateResponse> candidates = candidates(session);
        SessionMvpCandidateResponse topScorer = candidates.stream()
                .filter(candidate -> candidate.goals() > 0)
                .max(Comparator.comparingInt(SessionMvpCandidateResponse::goals)
                        .thenComparingInt(SessionMvpCandidateResponse::assists))
                .orElse(null);
        SessionMvpCandidateResponse secondHint = secondHintCandidate(candidates, topScorer);
        List<String> lines = new ArrayList<>();
        lines.add("Игра завершена, а теперь время выбрать <b>MVP</b> дня 🎯");
        lines.add("");
        lines.add("Может это " + hintName(topScorer) + "? Или " + hintName(secondHint) + "? Делай свой выбор в приложении.");
        lines.add("");
        lines.add("⏳ Голосование окончится в <b>" + escapeHtml(formatEndsAt(session)) + "</b>.");
        lines.add("Предварительные результаты:");
        voteResults(session).forEach((name, votes) -> lines.add("<code>" + escapeHtml(name) + "</code> (" + votes + ")"));
        return String.join("\n", lines);
    }

    private String buildFinalMessage(GameSession session) {
        List<SessionMvpCandidateResponse> winners = winners(candidates(session));
        if (winners.isEmpty()) {
            return "Голосование за <b>MVP</b> завершилось без голосов.";
        }
        return winners.stream()
                .map(winner -> {
                    List<String> stats = new ArrayList<>();
                    if (winner.goals() > 0) {
                        stats.add(winner.goals() + " " + goalsLabel(winner.goals()));
                    }
                    if (winner.assists() > 0) {
                        stats.add(winner.assists() + " " + assistsLabel(winner.assists()));
                    }
                    String statsText = stats.isEmpty() ? "" : " с " + String.join(" и ", stats);
                    return "MVP сегодняшней игры - <b>" + escapeHtml(displayName(winner)) + "</b> 🎉"
                            + statsText
                            + ". За " + escapeHtml(displayName(winner)) + " проголосовало " + winner.votes() + " человек.\nПоздравляем! 🏆";
                })
                .collect(Collectors.joining("\n\n"));
    }

    private List<SessionMvpCandidateResponse> candidates(GameSession session) {
        List<SessionPlayer> sessionPlayers = sessionPlayerRepository.findAllBySessionIdAndActiveTrueOrderByJoinedAtAscIdAsc(session.getId());
        Map<Long, PlayerStats> stats = playerStats(session.getId());
        Map<Long, Integer> votes = sessionMvpVoteRepository.findAllBySessionId(session.getId()).stream()
                .collect(Collectors.groupingBy(vote -> vote.getCandidate().getId(), Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));
        Map<Long, SessionTeamPlayer> teamByPlayerId = sessionTeamPlayerRepository.findAllBySessionTeamSessionIdAndActiveTrue(session.getId()).stream()
                .collect(Collectors.toMap(member -> member.getPlayer().getId(), Function.identity(), (left, right) -> left));

        return sessionPlayers.stream()
                .map(sessionPlayer -> {
                    Player player = sessionPlayer.getPlayer();
                    PlayerStats playerStats = stats.getOrDefault(player.getId(), new PlayerStats(0, 0));
                    SessionTeamPlayer teamMember = teamByPlayerId.get(player.getId());
                    return new SessionMvpCandidateResponse(
                            player.getId(),
                            player.getFirstName(),
                            player.getLastName(),
                            player.getUser() != null ? player.getUser().getDisplayName() : player.getNickname(),
                            player.getUser() != null ? player.getUser().getPhotoUrl() : null,
                            positionName(sessionPlayer.getPosition()),
                            teamMember != null ? teamMember.getSessionTeam().getId() : null,
                            teamMember != null ? teamMember.getSessionTeam().getName() : null,
                            teamMember != null ? teamMember.getSessionTeam().getColor() : null,
                            playerStats.goals(),
                            playerStats.assists(),
                            votes.getOrDefault(player.getId(), 0)
                    );
                })
                .toList();
    }

    private Map<Long, PlayerStats> playerStats(Long sessionId) {
        Map<Long, PlayerStats> stats = new LinkedHashMap<>();
        for (MatchEvent event : matchEventRepository.findAllByMatchSessionIdOrderByEventTimeAscIdAsc(sessionId)) {
            if (event.getPlayer() == null) {
                continue;
            }
            PlayerStats current = stats.getOrDefault(event.getPlayer().getId(), new PlayerStats(0, 0));
            if (event.getEventType() == MatchEventType.GOAL) {
                stats.put(event.getPlayer().getId(), new PlayerStats(current.goals() + 1, current.assists()));
            } else if (event.getEventType() == MatchEventType.ASSIST) {
                stats.put(event.getPlayer().getId(), new PlayerStats(current.goals(), current.assists() + 1));
            }
        }
        return stats;
    }

    private List<SessionMvpCandidateResponse> winners(List<SessionMvpCandidateResponse> candidates) {
        int maxVotes = candidates.stream().mapToInt(SessionMvpCandidateResponse::votes).max().orElse(0);
        if (maxVotes <= 0) {
            return List.of();
        }
        return candidates.stream()
                .filter(candidate -> candidate.votes() == maxVotes)
                .sorted(Comparator.comparing(SessionMvpCandidateResponse::displayName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    private Map<String, Integer> voteResults(GameSession session) {
        return candidates(session).stream()
                .filter(candidate -> candidate.votes() > 0)
                .sorted(Comparator.comparingInt(SessionMvpCandidateResponse::votes).reversed()
                        .thenComparing(candidate -> displayName(candidate).toLowerCase(Locale.ROOT)))
                .collect(Collectors.toMap(this::displayName, SessionMvpCandidateResponse::votes, (left, right) -> left, LinkedHashMap::new));
    }

    private Eligibility eligibility(GameSession session, Long userId) {
        if (!session.isMvpVotingEnabled()) {
            return new Eligibility(false, "Голосование за MVP не включено");
        }
        if (session.getMvpVotingStartedAt() == null) {
            return new Eligibility(false, "Голосование еще не началось");
        }
        if (isVotingFinished(session)) {
            return new Eligibility(false, "Голосование уже завершено");
        }
        if (userId == null) {
            return new Eligibility(false, "Откройте приложение через Telegram Mini App");
        }
        if (session.getMvpVotingParticipantScope() == MvpVotingParticipantScope.PLAYERS_ONLY) {
            Player player = playerRepository.findByUserId(userId).orElse(null);
            if (player == null || sessionPlayerRepository.findBySessionIdAndPlayerIdAndActiveTrue(session.getId(), player.getId()).isEmpty()) {
                return new Eligibility(false, "Голосовать могут только те, кто сегодня играли");
            }
        }
        return new Eligibility(true, null);
    }

    private boolean isVotingFinished(GameSession session) {
        return session.getMvpVotingEndsAt() != null && !session.getMvpVotingEndsAt().isAfter(OffsetDateTime.now());
    }

    private SessionMvpCandidateResponse secondHintCandidate(List<SessionMvpCandidateResponse> candidates, SessionMvpCandidateResponse topScorer) {
        SessionMvpCandidateResponse topAssistant = candidates.stream()
                .filter(candidate -> candidate.assists() > 0)
                .max(Comparator.comparingInt(SessionMvpCandidateResponse::assists)
                        .thenComparingInt(SessionMvpCandidateResponse::goals))
                .orElse(null);
        if (topAssistant != null && !samePlayer(topAssistant, topScorer)) {
            return topAssistant;
        }
        SessionMvpCandidateResponse goalkeeper = candidates.stream()
                .filter(candidate -> Objects.equals(candidate.position(), PlayerPosition.GOALKEEPER.name()))
                .filter(candidate -> !samePlayer(candidate, topScorer))
                .findFirst()
                .orElse(null);
        if (goalkeeper != null) {
            return goalkeeper;
        }
        return candidates.stream()
                .filter(candidate -> !samePlayer(candidate, topScorer))
                .findFirst()
                .orElse(topAssistant);
    }

    private boolean samePlayer(SessionMvpCandidateResponse left, SessionMvpCandidateResponse right) {
        return left != null && right != null && Objects.equals(left.playerId(), right.playerId());
    }

    private String hintName(SessionMvpCandidateResponse candidate) {
        return candidate == null ? "кто-то из игроков" : "<code>" + escapeHtml(displayName(candidate)) + "</code>";
    }

    private String displayName(SessionMvpCandidateResponse candidate) {
        if (candidate.displayName() != null && !candidate.displayName().isBlank()) {
            return candidate.displayName();
        }
        String fullName = ((candidate.firstName() == null ? "" : candidate.firstName()) + " " + (candidate.lastName() == null ? "" : candidate.lastName())).trim();
        return fullName.isBlank() ? "Игрок" : fullName;
    }

    private String positionName(PlayerPosition position) {
        return position == null ? null : position.name();
    }

    private String formatEndsAt(GameSession session) {
        return session.getMvpVotingEndsAt() == null ? "" : session.getMvpVotingEndsAt().atZoneSameInstant(DISPLAY_ZONE).format(END_TIME_FORMATTER);
    }

    private String goalsLabel(int count) {
        return count == 1 ? "голом" : "голами";
    }

    private String assistsLabel(int count) {
        return count == 1 ? "ассистом" : "ассистами";
    }

    private String mvpPageUrl(Long sessionId) {
        String appUrl = telegramBotProperties.getAppUrl();
        String safeAppUrl = appUrl == null || appUrl.isBlank() ? DEFAULT_APP_URL : appUrl;
        String separator = safeAppUrl.contains("?") ? "&" : "?";
        return safeAppUrl + separator + "startapp=mvp_" + sessionId;
    }

    private String escapeHtml(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private GameSession getSession(Long sessionId) {
        return gameSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Game session not found: " + sessionId));
    }

    private record PlayerStats(int goals, int assists) {
    }

    private record Eligibility(boolean canVote, String reason) {
    }
}
