package com.pollybreak.footballcore.service;

import com.pollybreak.footballcore.domain.entity.GameSession;
import com.pollybreak.footballcore.domain.enums.SessionStatus;
import com.pollybreak.footballcore.repository.GameSessionRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
public class SessionRegistrationScheduleService {

    public static final int DEFAULT_REGISTRATION_OPEN_HOURS_BEFORE = 120;

    private final GameSessionRepository gameSessionRepository;
    private final TelegramRegistrationService telegramRegistrationService;

    @Value("${app.time-zone:Asia/Almaty}")
    private String appTimeZone;

    @Scheduled(fixedDelayString = "${app.session-registration.fixed-delay-ms:1800000}")
    @Transactional
    public void startDueRegistrations() {
        for (GameSession session : gameSessionRepository.findRegistrationAutoStartCandidates()) {
            tryStartRegistrationIfDue(session);
        }
    }

    @Transactional
    public boolean startRegistrationIfDue(GameSession session) {
        if (!isDue(session)) {
            return false;
        }

        telegramRegistrationService.startRegistration(session.getId(), session.getCreatedBy().getId());
        return true;
    }

    @Transactional
    public boolean tryStartRegistrationIfDue(GameSession session) {
        try {
            return startRegistrationIfDue(session);
        } catch (RuntimeException exception) {
            log.warn("Failed to auto-start registration for session {}", session.getId(), exception);
            return false;
        }
    }

    private boolean isDue(GameSession session) {
        if (!session.isAutoStartRegistration()
                || session.getTelegramRegistrationMessageId() != null
                || session.getTelegramChatId() == null
                || session.getCreatedBy() == null
                || session.getStatus() != SessionStatus.PLANNED) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of(appTimeZone));
        LocalDateTime opensAt = session.getSessionDate()
                .atTime(session.getSessionTime())
                .minusHours(registrationOpenHoursBefore(session));
        return !opensAt.isAfter(now);
    }

    public int registrationOpenHoursBefore(GameSession session) {
        Integer value = session.getRegistrationOpenHoursBefore();
        return value == null ? DEFAULT_REGISTRATION_OPEN_HOURS_BEFORE : value;
    }
}
