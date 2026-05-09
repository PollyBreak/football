package com.pollybreak.footballcore.service;

import com.pollybreak.footballcore.api.dto.session.CreateGameSessionRequest;
import com.pollybreak.footballcore.api.dto.session.CreateSessionTeamRequest;
import com.pollybreak.footballcore.api.dto.session.GameSessionResponse;
import com.pollybreak.footballcore.api.dto.session.UpdateGameSessionRequest;
import com.pollybreak.footballcore.domain.entity.AppUser;
import com.pollybreak.footballcore.domain.entity.GameSession;
import com.pollybreak.footballcore.domain.entity.SessionTeam;
import com.pollybreak.footballcore.domain.enums.SessionStatus;
import com.pollybreak.footballcore.repository.AppUserRepository;
import com.pollybreak.footballcore.repository.GameSessionRepository;
import com.pollybreak.footballcore.repository.SessionTeamRepository;
import java.time.LocalDate;
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
    private final AppUserRepository appUserRepository;
    private final SessionPlayerService sessionPlayerService;

    public List<GameSession> findAll() {
        return gameSessionRepository.findAll();
    }

    public List<GameSession> findBySessionDate(LocalDate sessionDate) {
        return gameSessionRepository.findAllBySessionDateOrderByCreatedAtDesc(sessionDate);
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
        GameSession session = new GameSession();
        session.setTitle(request.title());
        session.setSessionDate(request.sessionDate());
        session.setLocation(request.location());
        session.setFormatType(request.formatType());
        session.setStatus(request.status() != null ? request.status() : SessionStatus.PLANNED);
        session.setPlannedMatchDurationMinutes(request.plannedMatchDurationMinutes());
        session.setMaxPlayers(request.maxPlayers());
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

        return GameSessionResponse.fromEntity(savedSession, teams);
    }

    @Transactional
    public GameSession save(GameSession gameSession) {
        return gameSessionRepository.save(gameSession);
    }

    @Transactional
    public GameSessionResponse update(Long sessionId, UpdateGameSessionRequest request) {
        GameSession session = getById(sessionId);
        if (request.maxPlayers() != null && request.maxPlayers() < 1) {
            throw new IllegalArgumentException("maxPlayers must be greater than zero");
        }
        session.setMaxPlayers(request.maxPlayers());
        sessionPlayerService.fillAvailableSlots(sessionId);
        return GameSessionResponse.fromEntity(session, sessionTeamRepository.findAllBySessionIdOrderByDisplayOrderAsc(sessionId));
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
}
