package com.pollybreak.footballcore.service;

import com.pollybreak.footballcore.api.dto.session.CreateGameSessionRequest;
import com.pollybreak.footballcore.api.dto.session.CreateSessionVenueRequest;
import com.pollybreak.footballcore.api.dto.session.CreateSessionTeamRequest;
import com.pollybreak.footballcore.api.dto.session.GameSessionResponse;
import com.pollybreak.footballcore.api.dto.session.UpdateGameSessionRequest;
import com.pollybreak.footballcore.domain.entity.AppUser;
import com.pollybreak.footballcore.domain.entity.GameSession;
import com.pollybreak.footballcore.domain.entity.SessionVenue;
import com.pollybreak.footballcore.domain.entity.SessionTeam;
import com.pollybreak.footballcore.domain.enums.SessionRecurrenceType;
import com.pollybreak.footballcore.domain.enums.SessionStatus;
import com.pollybreak.footballcore.repository.AppUserRepository;
import com.pollybreak.footballcore.repository.GameSessionRepository;
import com.pollybreak.footballcore.repository.MatchEventRepository;
import com.pollybreak.footballcore.repository.SessionMatchRepository;
import com.pollybreak.footballcore.repository.SessionTeamRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameSessionService {

    private final GameSessionRepository gameSessionRepository;
    private final SessionTeamRepository sessionTeamRepository;
    private final SessionMatchRepository sessionMatchRepository;
    private final MatchEventRepository matchEventRepository;
    private final AppUserRepository appUserRepository;
    private final SessionPlayerService sessionPlayerService;
    private final SessionVenueService sessionVenueService;
    private final SessionContributionReminderService sessionContributionReminderService;
    private final SessionRecurrenceService sessionRecurrenceService;
    private final SessionRegistrationScheduleService sessionRegistrationScheduleService;

    public List<GameSession> findAll() {
        return gameSessionRepository.findAllByOrderBySessionDateDescSessionTimeDescCreatedAtDesc();
    }

    public List<GameSession> findBySessionDate(LocalDate sessionDate) {
        return gameSessionRepository.findAllBySessionDateOrderBySessionTimeDescCreatedAtDesc(sessionDate);
    }

    public GameSession getById(Long id) {
        return gameSessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Game session not found: " + id));
    }

    public GameSessionResponse getResponseById(Long id) {
        return GameSessionResponse.fromEntity(getById(id), sessionTeamRepository.findAllBySessionIdOrderByDisplayOrderAsc(id));
    }

    @Transactional
    public GameSessionResponse create(CreateGameSessionRequest request) {
        validateRecurrenceRequest(request);
        if (Boolean.TRUE.equals(request.autoStartRegistration())) {
            if (request.createdByUserId() == null) {
                throw new IllegalArgumentException("createdByUserId is required for autoStartRegistration");
            }
            if (request.telegramChatId() == null) {
                throw new IllegalArgumentException("telegramChatId is required for autoStartRegistration");
            }
        }
        if (request.registrationOpenHoursBefore() != null && request.registrationOpenHoursBefore() < 0) {
            throw new IllegalArgumentException("registrationOpenHoursBefore must not be negative");
        }
        if (Boolean.TRUE.equals(request.autoStartContributionCollection()) && request.telegramChatId() == null) {
            throw new IllegalArgumentException("telegramChatId is required for autoStartContributionCollection");
        }

        GameSession session = new GameSession();
        session.setTitle(request.title());
        session.setSessionDate(request.sessionDate());
        session.setSessionTime(request.sessionTime());
        applyVenue(session, request);
        session.setBroadcastUrl(request.broadcastUrl());
        session.setTelegramChatId(request.telegramChatId());
        session.setTelegramChatTitle(request.telegramChatTitle());
        session.setAutoStartRegistration(Boolean.TRUE.equals(request.autoStartRegistration()));
        session.setRegistrationOpenHoursBefore(resolveRegistrationOpenHoursBefore(request));
        session.setFeeAmount(request.feeAmount());
        session.setFeeRecipient(request.feeRecipient());
        session.setFormatType(request.formatType());
        session.setStatus(request.status() != null ? request.status() : SessionStatus.PLANNED);
        session.setPlannedMatchDurationMinutes(request.plannedMatchDurationMinutes());
        session.setSessionDurationMinutes(request.sessionDurationMinutes());
        session.setMaxPlayers(request.maxPlayers());
        session.setPlayerFormat(request.playerFormat());
        session.setNotes(request.notes());

        if (request.createdByUserId() != null) {
            AppUser createdBy = appUserRepository.findById(request.createdByUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + request.createdByUserId()));
            session.setCreatedBy(createdBy);
        }

        GameSession savedSession = gameSessionRepository.save(session);

        List<SessionTeam> teams = request.teams() == null ? List.of() : request.teams().stream()
                .sorted(Comparator.comparingInt(team -> team.displayOrder() == null ? Integer.MAX_VALUE : team.displayOrder()))
                .map(teamRequest -> toSessionTeam(savedSession, teamRequest))
                .map(sessionTeamRepository::save)
                .toList();

        sessionRecurrenceService.attachRecurrenceRule(savedSession, request);

        if (Boolean.TRUE.equals(request.autoStartRegistration())) {
            sessionRegistrationScheduleService.startRegistrationIfDue(savedSession);
        }
        if (Boolean.TRUE.equals(request.autoStartContributionCollection())) {
            sessionContributionReminderService.createReminder(savedSession.getId(), 48);
        }

        return GameSessionResponse.fromEntity(savedSession, teams);
    }

    @Transactional
    public GameSession save(GameSession gameSession) {
        return gameSessionRepository.save(gameSession);
    }

    @Transactional
    public GameSessionResponse update(Long sessionId, UpdateGameSessionRequest request) {
        GameSession session = getById(sessionId);
        if (request.title() != null && request.title().isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (request.plannedMatchDurationMinutes() != null && request.plannedMatchDurationMinutes() < 1) {
            throw new IllegalArgumentException("plannedMatchDurationMinutes must be greater than zero");
        }
        if (request.sessionDurationMinutes() != null && request.sessionDurationMinutes() < 1) {
            throw new IllegalArgumentException("sessionDurationMinutes must be greater than zero");
        }
        if (request.maxPlayers() != null && request.maxPlayers() < 1) {
            throw new IllegalArgumentException("maxPlayers must be greater than zero");
        }
        if (request.registrationOpenHoursBefore() != null && request.registrationOpenHoursBefore() < 0) {
            throw new IllegalArgumentException("registrationOpenHoursBefore must not be negative");
        }

        if (request.title() != null) {
            session.setTitle(request.title());
        }
        if (request.sessionDate() != null) {
            session.setSessionDate(request.sessionDate());
        }
        if (request.sessionTime() != null) {
            session.setSessionTime(request.sessionTime());
        }
        session.setLocation(cleanOptional(request.location()));
        session.setLocationAddress(cleanOptional(request.locationAddress()));
        session.setLocationUrl(cleanOptional(request.locationUrl()));
        session.setBroadcastUrl(request.broadcastUrl());
        session.setTelegramChatId(request.telegramChatId());
        session.setTelegramChatTitle(request.telegramChatTitle());
        if (request.autoStartRegistration() != null) {
            session.setAutoStartRegistration(request.autoStartRegistration());
        }
        if (request.registrationOpenHoursBefore() != null) {
            session.setRegistrationOpenHoursBefore(request.registrationOpenHoursBefore());
        }
        if (session.isAutoStartRegistration() && session.getRegistrationOpenHoursBefore() == null) {
            session.setRegistrationOpenHoursBefore(SessionRegistrationScheduleService.DEFAULT_REGISTRATION_OPEN_HOURS_BEFORE);
        }
        validateAutoStartRegistration(session);
        session.setFeeAmount(request.feeAmount());
        session.setFeeRecipient(request.feeRecipient());
        if (request.status() != null) {
            session.setStatus(request.status());
            if (request.status() == SessionStatus.FINISHED && session.getEndedAt() == null) {
                session.setEndedAt(OffsetDateTime.now());
            }
        }
        session.setPlannedMatchDurationMinutes(request.plannedMatchDurationMinutes());
        session.setSessionDurationMinutes(request.sessionDurationMinutes());
        session.setNotes(request.notes());
        session.setMaxPlayers(request.maxPlayers());
        session.setPlayerFormat(request.playerFormat());
        if (request.recurrenceActive() != null && session.getRecurrenceRule() != null) {
            session.getRecurrenceRule().setActive(request.recurrenceActive());
        }
        sessionPlayerService.fillAvailableSlots(sessionId);
        if (session.getStatus() == SessionStatus.FINISHED) {
            sessionRecurrenceService.createNextSessionIfDue(session);
        }
        sessionRegistrationScheduleService.startRegistrationIfDue(session);
        return GameSessionResponse.fromEntity(session, sessionTeamRepository.findAllBySessionIdOrderByDisplayOrderAsc(sessionId));
    }

    @Transactional
    public void deleteById(Long sessionId) {
        if (!gameSessionRepository.existsById(sessionId)) {
            throw new IllegalArgumentException("Game session not found: " + sessionId);
        }

        matchEventRepository.deleteAllBySessionId(sessionId);
        sessionMatchRepository.deleteAllBySessionId(sessionId);
        gameSessionRepository.deleteById(sessionId);
    }

    private SessionTeam toSessionTeam(GameSession session, CreateSessionTeamRequest request) {
        SessionTeam team = new SessionTeam();
        team.setSession(session);
        team.setName(request.name());
        team.setColor(request.color());
        team.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : nextDisplayOrder(session.getId()));
        return team;
    }

    private int nextDisplayOrder(Long sessionId) {
        return sessionTeamRepository.findAllBySessionIdOrderByDisplayOrderAsc(sessionId).size() + 1;
    }

    private void applyVenue(GameSession session, CreateGameSessionRequest request) {
        if (request.venueId() != null) {
            applyExistingVenue(session, request.venueId());
            return;
        }

        session.setLocation(cleanOptional(request.location()));
        session.setLocationAddress(cleanOptional(request.locationAddress()));
        session.setLocationUrl(cleanOptional(request.locationUrl()));

        if (Boolean.TRUE.equals(request.saveVenue())) {
            SessionVenue venue = sessionVenueService.createVenue(new CreateSessionVenueRequest(
                    request.location(),
                    request.locationAddress(),
                    request.locationUrl(),
                    request.venuePhotoUrl()
            ));
            session.setVenue(venue);
            applyVenueFields(session, venue);
        }
    }

    private void applyExistingVenue(GameSession session, Long venueId) {
        SessionVenue venue = sessionVenueService.getById(venueId);
        session.setVenue(venue);
        applyVenueFields(session, venue);
    }

    private void applyVenueFields(GameSession session, SessionVenue venue) {
        session.setLocation(venue.getName());
        session.setLocationAddress(venue.getAddress());
        session.setLocationUrl(venue.getGisUrl());
    }

    private void validateRecurrenceRequest(CreateGameSessionRequest request) {
        if (request.recurrenceType() == null) {
            if (request.recurrenceIntervalDays() != null || request.recurrenceDayOfMonth() != null) {
                throw new IllegalArgumentException("recurrenceType is required when recurrence values are provided");
            }
            return;
        }

        if (request.recurrenceType() == SessionRecurrenceType.DAYS) {
            if (request.recurrenceIntervalDays() == null || request.recurrenceIntervalDays() < 1) {
                throw new IllegalArgumentException("recurrenceIntervalDays must be greater than zero");
            }
            if (request.recurrenceDayOfMonth() != null) {
                throw new IllegalArgumentException("recurrenceDayOfMonth must be empty for DAYS recurrence");
            }
            return;
        }

        if (request.recurrenceDayOfMonth() == null || request.recurrenceDayOfMonth() < 1 || request.recurrenceDayOfMonth() > 31) {
            throw new IllegalArgumentException("recurrenceDayOfMonth must be between 1 and 31");
        }
        if (request.recurrenceIntervalDays() != null) {
            throw new IllegalArgumentException("recurrenceIntervalDays must be empty for MONTHLY recurrence");
        }
    }

    private Integer resolveRegistrationOpenHoursBefore(CreateGameSessionRequest request) {
        if (!Boolean.TRUE.equals(request.autoStartRegistration())) {
            return request.registrationOpenHoursBefore();
        }
        return request.registrationOpenHoursBefore() == null
                ? SessionRegistrationScheduleService.DEFAULT_REGISTRATION_OPEN_HOURS_BEFORE
                : request.registrationOpenHoursBefore();
    }

    private void validateAutoStartRegistration(GameSession session) {
        if (!session.isAutoStartRegistration()) {
            return;
        }
        if (session.getCreatedBy() == null) {
            throw new IllegalArgumentException("createdByUserId is required for autoStartRegistration");
        }
        if (session.getTelegramChatId() == null) {
            throw new IllegalArgumentException("telegramChatId is required for autoStartRegistration");
        }
    }

    private String cleanOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
