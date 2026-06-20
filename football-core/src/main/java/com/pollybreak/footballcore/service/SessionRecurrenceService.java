package com.pollybreak.footballcore.service;

import com.pollybreak.footballcore.api.dto.session.CreateGameSessionRequest;
import com.pollybreak.footballcore.domain.entity.GameSession;
import com.pollybreak.footballcore.domain.entity.SessionContributionReminder;
import com.pollybreak.footballcore.domain.entity.SessionRecurrenceRule;
import com.pollybreak.footballcore.domain.entity.SessionTeam;
import com.pollybreak.footballcore.domain.enums.SessionRecurrenceType;
import com.pollybreak.footballcore.domain.enums.SessionStatus;
import com.pollybreak.footballcore.repository.GameSessionRepository;
import com.pollybreak.footballcore.repository.SessionContributionReminderRepository;
import com.pollybreak.footballcore.repository.SessionRecurrenceRuleRepository;
import com.pollybreak.footballcore.repository.SessionTeamRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SessionRecurrenceService {

    private final SessionRecurrenceRuleRepository recurrenceRuleRepository;
    private final GameSessionRepository gameSessionRepository;
    private final SessionTeamRepository sessionTeamRepository;
    private final SessionContributionReminderRepository reminderRepository;
    private final TelegramRegistrationService telegramRegistrationService;

    @Value("${app.time-zone:Asia/Almaty}")
    private String appTimeZone;

    @Transactional
    public void attachRecurrenceRule(GameSession session, CreateGameSessionRequest request) {
        if (request.recurrenceType() == null) {
            return;
        }

        SessionRecurrenceRule rule = new SessionRecurrenceRule();
        rule.setRecurrenceType(request.recurrenceType());
        rule.setIntervalDays(request.recurrenceType() == SessionRecurrenceType.DAYS ? request.recurrenceIntervalDays() : null);
        rule.setDayOfMonth(request.recurrenceType() == SessionRecurrenceType.MONTHLY ? request.recurrenceDayOfMonth() : null);
        rule.setAutoStartRegistration(Boolean.TRUE.equals(request.autoStartRegistration()));
        rule.setAutoStartContributionCollection(Boolean.TRUE.equals(request.autoStartContributionCollection()));
        rule.setCurrentSession(session);
        SessionRecurrenceRule savedRule = recurrenceRuleRepository.save(rule);
        session.setRecurrenceRule(savedRule);
        gameSessionRepository.save(session);
    }

    @Transactional
    public GameSession createNextSessionIfDue(Long sessionId) {
        GameSession session = gameSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Game session not found: " + sessionId));
        return createNextSessionIfDue(session);
    }

    @Transactional
    public GameSession createNextSessionIfDue(GameSession session) {
        SessionRecurrenceRule rule = session.getRecurrenceRule();
        if (rule == null || !rule.isActive() || rule.getCurrentSession() == null) {
            return null;
        }
        if (session.getStatus() != SessionStatus.FINISHED) {
            return null;
        }
        if (!session.getId().equals(rule.getCurrentSession().getId())) {
            return null;
        }

        LocalDate nextDate = computeNextFutureDate(session, rule);
        GameSession nextSession = cloneSession(session, rule, nextDate);
        rule.setCurrentSession(nextSession);
        recurrenceRuleRepository.save(rule);

        tryAutoStartRegistration(rule, nextSession);
        return nextSession;
    }

    @Scheduled(fixedDelayString = "${app.session-recurrence.fixed-delay-ms:1800000}")
    @Transactional
    public void generateDueSessions() {
        for (SessionRecurrenceRule rule : recurrenceRuleRepository.findAllByActiveTrue()) {
            GameSession currentSession = rule.getCurrentSession();
            if (currentSession == null || currentSession.getStatus() != SessionStatus.FINISHED) {
                continue;
            }

            try {
                createNextSessionIfDue(currentSession);
            } catch (RuntimeException exception) {
                log.warn("Failed to generate next recurring session for rule {}", rule.getId(), exception);
            }
        }
    }

    private LocalDate computeNextFutureDate(GameSession session, SessionRecurrenceRule rule) {
        ZoneId zoneId = ZoneId.of(appTimeZone);
        LocalDateTime now = LocalDateTime.now(zoneId);
        LocalDate candidate = nextDate(session.getSessionDate(), rule);

        while (!candidate.atTime(session.getSessionTime()).isAfter(now)) {
            candidate = nextDate(candidate, rule);
        }
        return candidate;
    }

    private LocalDate nextDate(LocalDate sourceDate, SessionRecurrenceRule rule) {
        if (rule.getRecurrenceType() == SessionRecurrenceType.DAYS) {
            return sourceDate.plusDays(rule.getIntervalDays());
        }

        LocalDate nextMonth = sourceDate.plusMonths(1);
        int day = Math.min(rule.getDayOfMonth(), nextMonth.lengthOfMonth());
        return nextMonth.withDayOfMonth(day);
    }

    private GameSession cloneSession(GameSession source, SessionRecurrenceRule rule, LocalDate nextDate) {
        GameSession nextSession = new GameSession();
        nextSession.setTitle(source.getTitle());
        nextSession.setSessionDate(nextDate);
        nextSession.setSessionTime(source.getSessionTime());
        nextSession.setLocation(source.getLocation());
        nextSession.setLocationAddress(source.getLocationAddress());
        nextSession.setLocationUrl(source.getLocationUrl());
        nextSession.setVenue(source.getVenue());
        nextSession.setBroadcastUrl(source.getBroadcastUrl());
        nextSession.setTelegramChatId(source.getTelegramChatId());
        nextSession.setTelegramChatTitle(source.getTelegramChatTitle());
        nextSession.setFeeAmount(source.getFeeAmount());
        nextSession.setFeeRecipient(source.getFeeRecipient());
        nextSession.setFormatType(source.getFormatType());
        nextSession.setStatus(SessionStatus.PLANNED);
        nextSession.setPlannedMatchDurationMinutes(source.getPlannedMatchDurationMinutes());
        nextSession.setSessionDurationMinutes(source.getSessionDurationMinutes());
        nextSession.setMaxPlayers(source.getMaxPlayers());
        nextSession.setPlayerFormat(source.getPlayerFormat());
        nextSession.setNotes(source.getNotes());
        nextSession.setCreatedBy(source.getCreatedBy());
        nextSession.setRecurrenceRule(rule);
        GameSession savedSession = gameSessionRepository.save(nextSession);

        cloneTeams(source, savedSession);
        cloneContributionReminders(source, savedSession);
        return savedSession;
    }

    private void cloneTeams(GameSession source, GameSession target) {
        List<SessionTeam> teams = sessionTeamRepository.findAllBySessionIdOrderByDisplayOrderAsc(source.getId());
        for (SessionTeam team : teams) {
            SessionTeam copy = new SessionTeam();
            copy.setSession(target);
            copy.setName(team.getName());
            copy.setColor(team.getColor());
            copy.setDisplayOrder(team.getDisplayOrder());
            sessionTeamRepository.save(copy);
        }
    }

    private void cloneContributionReminders(GameSession source, GameSession target) {
        List<SessionContributionReminder> reminders = reminderRepository.findAllForSession(source.getId());
        for (SessionContributionReminder reminder : reminders) {
            SessionContributionReminder copy = new SessionContributionReminder();
            copy.setSession(target);
            copy.setHoursBefore(reminder.getHoursBefore());
            copy.setSentAt(null);
            reminderRepository.save(copy);
        }
    }

    private void tryAutoStartRegistration(SessionRecurrenceRule rule, GameSession session) {
        if (!rule.isAutoStartRegistration() || session.getCreatedBy() == null || session.getTelegramChatId() == null) {
            return;
        }

        try {
            telegramRegistrationService.startRegistration(session.getId(), session.getCreatedBy().getId());
        } catch (RuntimeException exception) {
            log.warn("Failed to auto-start registration for recurring session {}", session.getId(), exception);
        }
    }
}
