package com.pollybreak.footballcore.service;

import com.pollybreak.footballcore.api.dto.telegram.ContributionReminderResponse;
import com.pollybreak.footballcore.domain.entity.GameSession;
import com.pollybreak.footballcore.domain.entity.SessionContributionReminder;
import com.pollybreak.footballcore.domain.enums.SessionStatus;
import com.pollybreak.footballcore.repository.GameSessionRepository;
import com.pollybreak.footballcore.repository.SessionContributionReminderRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
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
public class SessionContributionReminderService {

    private final SessionContributionReminderRepository reminderRepository;
    private final GameSessionRepository gameSessionRepository;
    private final TelegramContributionService telegramContributionService;

    @Value("${app.time-zone:Asia/Almaty}")
    private String appTimeZone;

    public List<ContributionReminderResponse> getReminders(Long sessionId) {
        return reminderRepository.findAllForSession(sessionId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ContributionReminderResponse createReminder(Long sessionId, Integer hoursBefore) {
        if (hoursBefore == null || hoursBefore < 1) {
            throw new IllegalArgumentException("Reminder hours must be greater than zero");
        }

        GameSession session = gameSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Game session not found: " + sessionId));

        SessionContributionReminder reminder = reminderRepository
                .findForSessionAndHoursBefore(sessionId, hoursBefore)
                .orElseGet(() -> {
                    SessionContributionReminder created = new SessionContributionReminder();
                    created.setSession(session);
                    created.setHoursBefore(hoursBefore);
                    return created;
                });
        reminder.setSentAt(null);
        return toResponse(reminderRepository.save(reminder));
    }

    @Transactional
    public void deleteReminder(Long sessionId, Integer hoursBefore) {
        reminderRepository.findForSessionAndHoursBefore(sessionId, hoursBefore)
                .ifPresent(reminderRepository::delete);
    }

    @Scheduled(fixedDelayString = "${app.contribution-reminders.fixed-delay-ms:1800000}")
    @Transactional
    public void sendDueReminders() {
        ZoneId zoneId = ZoneId.of(appTimeZone);
        OffsetDateTime now = OffsetDateTime.now(zoneId);
        LocalDateTime localNow = now.toLocalDateTime();

        for (SessionContributionReminder reminder : reminderRepository.findPendingWithSession()) {
            GameSession session = reminder.getSession();
            if (isClosed(session)) {
                reminder.setSentAt(now);
                continue;
            }
            if (session.getTelegramChatId() == null || session.getTelegramContributionMessageId() == null) {
                continue;
            }

            LocalDateTime sessionStart = session.getSessionDate().atTime(session.getSessionTime());
            long minutesUntilStart = Duration.between(localNow, sessionStart).toMinutes();
            if (minutesUntilStart < 0) {
                reminder.setSentAt(now);
                continue;
            }
            if (minutesUntilStart > reminder.getHoursBefore() * 60L) {
                continue;
            }

            try {
                telegramContributionService.sendContributionReminder(session.getId(), reminder.getHoursBefore());
                reminder.setSentAt(now);
            } catch (RuntimeException exception) {
                log.warn(
                        "Failed to send contribution reminder for session {} ({} hours before)",
                        session.getId(),
                        reminder.getHoursBefore(),
                        exception
                );
            }
        }
    }

    private boolean isClosed(GameSession session) {
        return session.getStatus() == SessionStatus.FINISHED || session.getStatus() == SessionStatus.CANCELLED;
    }

    private ContributionReminderResponse toResponse(SessionContributionReminder reminder) {
        return new ContributionReminderResponse(
                reminder.getId(),
                reminder.getSession().getId(),
                reminder.getHoursBefore(),
                reminder.getSentAt()
        );
    }
}
