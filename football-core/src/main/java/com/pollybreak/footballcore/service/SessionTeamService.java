package com.pollybreak.footballcore.service;

import com.pollybreak.footballcore.api.dto.session.AddPlayerToSessionTeamRequest;
import com.pollybreak.footballcore.api.dto.session.AddPlayersToSessionTeamRequest;
import com.pollybreak.footballcore.api.dto.session.CreateSessionTeamRequest;
import com.pollybreak.footballcore.api.dto.session.SessionTeamPlayerResponse;
import com.pollybreak.footballcore.api.dto.session.SessionTeamResponse;
import com.pollybreak.footballcore.domain.entity.GameSession;
import com.pollybreak.footballcore.domain.entity.Player;
import com.pollybreak.footballcore.domain.entity.SessionPlayer;
import com.pollybreak.footballcore.domain.entity.SessionTeam;
import com.pollybreak.footballcore.domain.entity.SessionTeamPlayer;
import com.pollybreak.footballcore.repository.GameSessionRepository;
import com.pollybreak.footballcore.repository.PlayerRepository;
import com.pollybreak.footballcore.repository.SessionPlayerRepository;
import com.pollybreak.footballcore.repository.SessionTeamPlayerRepository;
import com.pollybreak.footballcore.repository.SessionTeamRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionTeamService {

    private final GameSessionRepository gameSessionRepository;
    private final SessionTeamRepository sessionTeamRepository;
    private final SessionTeamPlayerRepository sessionTeamPlayerRepository;
    private final SessionPlayerRepository sessionPlayerRepository;
    private final PlayerRepository playerRepository;

    public List<SessionTeam> findBySessionId(Long sessionId) {
        return sessionTeamRepository.findAllBySessionIdOrderByDisplayOrderAsc(sessionId);
    }

    public SessionTeam getById(Long id) {
        return sessionTeamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Session team not found: " + id));
    }

    public List<SessionTeamResponse> getResponsesBySessionId(Long sessionId) {
        return findBySessionId(sessionId).stream()
                .map(SessionTeamResponse::fromEntity)
                .toList();
    }

    @Transactional
    public SessionTeamResponse create(Long sessionId, CreateSessionTeamRequest request) {
        GameSession session = gameSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Game session not found: " + sessionId));

        SessionTeam team = new SessionTeam();
        team.setSession(session);
        team.setName(request.name());
        team.setColor(request.color());
        team.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : findBySessionId(sessionId).size() + 1);

        return SessionTeamResponse.fromEntity(sessionTeamRepository.save(team));
    }

    @Transactional
    public SessionTeamPlayerResponse addPlayer(Long sessionTeamId, AddPlayerToSessionTeamRequest request) {
        SessionTeam sessionTeam = getById(sessionTeamId);
        Player player = playerRepository.findById(request.playerId())
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + request.playerId()));
        SessionPlayer sessionPlayer = sessionPlayerRepository
                .findBySessionIdAndPlayerIdAndActiveTrue(sessionTeam.getSession().getId(), request.playerId())
                .orElseThrow(() -> new IllegalArgumentException("Player must be registered for the session first"));

        sessionTeamPlayerRepository.findBySessionTeamIdAndPlayerIdAndActiveTrue(sessionTeamId, request.playerId())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Player is already active in this team");
                });

        SessionTeamPlayer teamPlayer = new SessionTeamPlayer();
        teamPlayer.setSessionTeam(sessionTeam);
        teamPlayer.setPlayer(player);
        teamPlayer.setPosition(request.position() != null ? request.position() : sessionPlayer.getPosition());
        teamPlayer.setActive(true);

        return SessionTeamPlayerResponse.fromEntity(sessionTeamPlayerRepository.save(teamPlayer));
    }

    @Transactional
    public List<SessionTeamPlayerResponse> addPlayers(Long sessionTeamId, AddPlayersToSessionTeamRequest request) {
        return request.playerIds().stream()
                .map(playerId -> addPlayer(sessionTeamId, new AddPlayerToSessionTeamRequest(playerId, null)))
                .toList();
    }

    public List<SessionTeamPlayerResponse> getActivePlayers(Long sessionTeamId) {
        return sessionTeamPlayerRepository.findAllBySessionTeamIdAndActiveTrue(sessionTeamId).stream()
                .map(SessionTeamPlayerResponse::fromEntity)
                .toList();
    }

    @Transactional
    public SessionTeam save(SessionTeam sessionTeam) {
        return sessionTeamRepository.save(sessionTeam);
    }
}
