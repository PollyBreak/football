package com.pollybreak.footballcore.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.pollybreak.footballcore.domain.entity.GameSession;
import com.pollybreak.footballcore.domain.entity.SessionRatingVote;
import com.pollybreak.footballcore.repository.GameSessionRepository;
import com.pollybreak.footballcore.repository.SessionRatingVoteRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SessionRatingPollService {

    private static final Locale RU = Locale.forLanguageTag("ru-RU");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final List<String> RATING_OPTIONS = List.of("5️⃣", "4️⃣", "3️⃣", "2️⃣", "1️⃣", "Тык, не играл");

    private final GameSessionRepository gameSessionRepository;
    private final SessionRatingVoteRepository sessionRatingVoteRepository;
    private final TelegramBotApiClient telegramBotApiClient;

    @Transactional
    public void startPollIfNeeded(GameSession session) {
        if (!session.isSessionRatingPollEnabled()
                || session.getTelegramChatId() == null
                || session.getTelegramSessionRatingPollId() != null) {
            return;
        }

        try {
            sendSummaryMessageIfNeeded(session);
            JsonNode result = telegramBotApiClient.sendPoll(
                    session.getTelegramChatId(),
                    "На сколько мне понравился позитивный футбол?",
                    RATING_OPTIONS
            );
            session.setTelegramSessionRatingPollMessageId(result.path("message_id").asLong());
            session.setTelegramSessionRatingPollId(result.path("poll").path("id").asText(null));
        } catch (TelegramChatMigratedException exception) {
            session.setTelegramChatId(exception.getMigratedChatId());
            sendSummaryMessageIfNeeded(session);
            JsonNode result = telegramBotApiClient.sendPoll(
                    session.getTelegramChatId(),
                    "На сколько мне понравился позитивный футбол?",
                    RATING_OPTIONS
            );
            session.setTelegramSessionRatingPollMessageId(result.path("message_id").asLong());
            session.setTelegramSessionRatingPollId(result.path("poll").path("id").asText(null));
        } catch (RuntimeException exception) {
            log.warn("Failed to start session rating poll for session {}", session.getId(), exception);
        }
    }

    private void sendSummaryMessageIfNeeded(GameSession session) {
        if (session.getTelegramSessionRatingSummaryMessageId() != null) {
            return;
        }
        JsonNode summary = telegramBotApiClient.sendMessage(
                session.getTelegramChatId(),
                buildSummaryMessage(session)
        );
        session.setTelegramSessionRatingSummaryMessageId(summary.path("message_id").asLong());
    }

    @Transactional
    public void handlePollAnswer(JsonNode pollAnswer) {
        String pollId = pollAnswer.path("poll_id").asText("");
        if (pollId.isBlank()) {
            return;
        }

        GameSession session = gameSessionRepository.findByTelegramSessionRatingPollId(pollId)
                .orElse(null);
        if (session == null) {
            return;
        }

        Long telegramUserId = pollAnswer.path("user").path("id").canConvertToLong()
                ? pollAnswer.path("user").path("id").asLong()
                : null;
        if (telegramUserId == null) {
            return;
        }

        JsonNode optionIds = pollAnswer.path("option_ids");
        if (!optionIds.isArray() || optionIds.size() == 0) {
            sessionRatingVoteRepository.findBySessionIdAndTelegramUserId(session.getId(), telegramUserId)
                    .ifPresent(sessionRatingVoteRepository::delete);
            recalculateAverage(session);
            refreshSummaryMessage(session);
            return;
        }

        int optionId = optionIds.get(0).asInt(-1);
        if (optionId < 0 || optionId >= RATING_OPTIONS.size()) {
            return;
        }
        if (optionId == RATING_OPTIONS.size() - 1) {
            sessionRatingVoteRepository.findBySessionIdAndTelegramUserId(session.getId(), telegramUserId)
                    .ifPresent(sessionRatingVoteRepository::delete);
            recalculateAverage(session);
            refreshSummaryMessage(session);
            return;
        }
        int rating = 5 - optionId;

        SessionRatingVote vote = sessionRatingVoteRepository
                .findBySessionIdAndTelegramUserId(session.getId(), telegramUserId)
                .orElseGet(() -> {
                    SessionRatingVote created = new SessionRatingVote();
                    created.setSession(session);
                    created.setTelegramUserId(telegramUserId);
                    return created;
                });
        vote.setRating(rating);
        sessionRatingVoteRepository.save(vote);
        recalculateAverage(session);
        refreshSummaryMessage(session);
    }

    private void recalculateAverage(GameSession session) {
        List<SessionRatingVote> votes = sessionRatingVoteRepository.findAllBySessionId(session.getId());
        session.setSessionRatingVoteCount(votes.size());
        if (votes.isEmpty()) {
            session.setSessionRatingAverage(null);
            return;
        }

        double average = votes.stream()
                .mapToInt(SessionRatingVote::getRating)
                .average()
                .orElse(0);
        session.setSessionRatingAverage(BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP));
    }

    private void refreshSummaryMessage(GameSession session) {
        if (session.getTelegramChatId() == null || session.getTelegramSessionRatingSummaryMessageId() == null) {
            return;
        }
        try {
            telegramBotApiClient.editMessageText(
                    session.getTelegramChatId(),
                    session.getTelegramSessionRatingSummaryMessageId(),
                    buildSummaryMessage(session)
            );
        } catch (TelegramChatMigratedException exception) {
            session.setTelegramChatId(exception.getMigratedChatId());
            telegramBotApiClient.editMessageText(
                    session.getTelegramChatId(),
                    session.getTelegramSessionRatingSummaryMessageId(),
                    buildSummaryMessage(session)
            );
        } catch (RuntimeException exception) {
            log.warn("Failed to refresh session rating summary for session {}", session.getId(), exception);
        }
    }

    private String buildSummaryMessage(GameSession session) {
        return escape(sessionSummary(session))
                + "\nСредний рейтинг: " + averageLabel(session);
    }

    private String sessionSummary(GameSession session) {
        return session.getTitle()
                + " | " + dateTimeLabel(session)
                + " | " + nullToDash(session.getLocation());
    }

    private String dateTimeLabel(GameSession session) {
        String dayOfWeek = session.getSessionDate().getDayOfWeek().getDisplayName(TextStyle.SHORT, RU)
                .replace(".", "");
        String capitalizedDayOfWeek = dayOfWeek.substring(0, 1).toUpperCase(RU) + dayOfWeek.substring(1);
        String month = session.getSessionDate().getMonth().getDisplayName(TextStyle.FULL, RU);
        return capitalizedDayOfWeek
                + ", " + session.getSessionDate().getDayOfMonth()
                + " " + month
                + " " + session.getSessionTime().format(TIME_FORMAT);
    }

    private String averageLabel(GameSession session) {
        return session.getSessionRatingAverage() == null ? "-" : session.getSessionRatingAverage().toPlainString();
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
}
