package com.pollybreak.footballcore.service;

import com.pollybreak.footballcore.api.dto.match.CreateSessionMatchRequest;
import com.pollybreak.footballcore.api.dto.match.FinishSessionMatchRequest;
import com.pollybreak.footballcore.api.dto.match.StartSessionMatchRequest;
import com.pollybreak.footballcore.api.dto.match.UpdateSessionMatchScoreRequest;
import com.pollybreak.footballcore.api.dto.match.SessionMatchResponse;
import com.pollybreak.footballcore.api.dto.session.SessionStandingsResponse;
import com.pollybreak.footballcore.api.dto.session.SessionStandingsRowResponse;
import com.pollybreak.footballcore.domain.entity.GameSession;
import com.pollybreak.footballcore.domain.entity.SessionMatch;
import com.pollybreak.footballcore.domain.entity.SessionTeam;
import com.pollybreak.footballcore.domain.enums.MatchStatus;
import java.time.OffsetDateTime;
import com.pollybreak.footballcore.repository.GameSessionRepository;
import com.pollybreak.footballcore.repository.SessionMatchRepository;
import com.pollybreak.footballcore.repository.SessionTeamRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionMatchService {

    private final SessionMatchRepository sessionMatchRepository;
    private final GameSessionRepository gameSessionRepository;
    private final SessionTeamRepository sessionTeamRepository;

    public List<SessionMatch> findBySessionId(Long sessionId) {
        return sessionMatchRepository.findAllBySessionIdOrderByMatchNumberAsc(sessionId);
    }

    public SessionMatch getById(Long id) {
        return sessionMatchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Session match not found: " + id));
    }

    public SessionMatchResponse getResponseById(Long id) {
        return SessionMatchResponse.fromEntity(getById(id));
    }

    public List<SessionMatchResponse> getResponsesBySessionId(Long sessionId) {
        return findBySessionId(sessionId).stream()
                .map(SessionMatchResponse::fromEntity)
                .toList();
    }

    public SessionStandingsResponse getStandings(Long sessionId) {
        List<SessionTeam> teams = sessionTeamRepository.findAllBySessionIdOrderByDisplayOrderAsc(sessionId);
        if (teams.isEmpty()) {
            throw new IllegalArgumentException("No teams found for session: " + sessionId);
        }

        Map<Long, MutableStanding> table = new LinkedHashMap<>();
        for (SessionTeam team : teams) {
            table.put(team.getId(), new MutableStanding(team.getId(), team.getName(), team.getColor()));
        }

        findBySessionId(sessionId).stream()
                .filter(match -> match.getStatus() == MatchStatus.FINISHED)
                .forEach(match -> applyMatchResult(table, match));

        List<SessionStandingsRowResponse> rows = table.values().stream()
                .map(MutableStanding::toResponse)
                .sorted(Comparator
                        .comparingInt(SessionStandingsRowResponse::points).reversed()
                        .thenComparingInt(SessionStandingsRowResponse::goalDifference).reversed()
                        .thenComparingInt(SessionStandingsRowResponse::goalsFor).reversed()
                        .thenComparing(SessionStandingsRowResponse::teamName))
                .toList();

        return new SessionStandingsResponse(sessionId, rows);
    }

    public int getNextMatchNumber(Long sessionId) {
        return sessionMatchRepository.findTopBySessionIdOrderByMatchNumberDesc(sessionId)
                .map(SessionMatch::getMatchNumber)
                .orElse(0) + 1;
    }

    @Transactional
    public SessionMatchResponse create(Long sessionId, CreateSessionMatchRequest request) {
        GameSession session = gameSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Game session not found: " + sessionId));
        SessionTeam teamA = sessionTeamRepository.findById(request.teamAId())
                .orElseThrow(() -> new IllegalArgumentException("Team A not found: " + request.teamAId()));
        SessionTeam teamB = sessionTeamRepository.findById(request.teamBId())
                .orElseThrow(() -> new IllegalArgumentException("Team B not found: " + request.teamBId()));

        if (!teamA.getSession().getId().equals(session.getId()) || !teamB.getSession().getId().equals(session.getId())) {
            throw new IllegalArgumentException("Both teams must belong to the same session");
        }
        if (teamA.getId().equals(teamB.getId())) {
            throw new IllegalArgumentException("A match requires two different teams");
        }

        SessionMatch match = new SessionMatch();
        match.setSession(session);
        match.setTeamA(teamA);
        match.setTeamB(teamB);
        match.setMatchNumber(request.matchNumber() != null ? request.matchNumber() : getNextMatchNumber(session.getId()));
        match.setStatus(request.status() != null ? request.status() : MatchStatus.PLANNED);
        match.setPlannedDurationMinutes(request.plannedDurationMinutes() != null
                ? request.plannedDurationMinutes()
                : session.getPlannedMatchDurationMinutes());

        return SessionMatchResponse.fromEntity(sessionMatchRepository.save(match));
    }

    @Transactional
    public SessionMatchResponse updateScore(Long matchId, UpdateSessionMatchScoreRequest request) {
        SessionMatch match = getById(matchId);
        match.setTeamAScore(request.teamAScore());
        match.setTeamBScore(request.teamBScore());
        syncWinningTeam(match);
        return SessionMatchResponse.fromEntity(match);
    }

    @Transactional
    public SessionMatchResponse start(Long matchId, StartSessionMatchRequest request) {
        SessionMatch match = getById(matchId);
        match.setStatus(MatchStatus.IN_PROGRESS);
        match.setStartedAt(request.startedAt() != null ? request.startedAt() : OffsetDateTime.now());
        return SessionMatchResponse.fromEntity(match);
    }

    @Transactional
    public SessionMatchResponse finish(Long matchId, FinishSessionMatchRequest request) {
        SessionMatch match = getById(matchId);
        match.setStatus(MatchStatus.FINISHED);
        match.setEndedAt(request != null && request.endedAt() != null ? request.endedAt() : OffsetDateTime.now());
        if (match.getStartedAt() == null) {
            match.setStartedAt(request != null && request.startedAt() != null ? request.startedAt() : match.getCreatedAt());
        } else if (request != null && request.startedAt() != null) {
            match.setStartedAt(request.startedAt());
        }

        if (request != null && request.teamAScore() != null) {
            match.setTeamAScore(request.teamAScore());
        }
        if (request != null && request.teamBScore() != null) {
            match.setTeamBScore(request.teamBScore());
        }

        syncWinningTeam(match);
        return SessionMatchResponse.fromEntity(match);
    }

    @Transactional
    public SessionMatch save(SessionMatch sessionMatch) {
        return sessionMatchRepository.save(sessionMatch);
    }

    private void syncWinningTeam(SessionMatch match) {
        if (match.getTeamAScore() > match.getTeamBScore()) {
            match.setWinningTeam(match.getTeamA());
            return;
        }
        if (match.getTeamBScore() > match.getTeamAScore()) {
            match.setWinningTeam(match.getTeamB());
            return;
        }
        match.setWinningTeam(null);
    }

    private void applyMatchResult(Map<Long, MutableStanding> table, SessionMatch match) {
        MutableStanding teamA = table.get(match.getTeamA().getId());
        MutableStanding teamB = table.get(match.getTeamB().getId());
        if (teamA == null || teamB == null) {
            throw new IllegalArgumentException("Match contains team outside the session standings scope");
        }

        int scoreA = match.getTeamAScore();
        int scoreB = match.getTeamBScore();

        teamA.played++;
        teamB.played++;
        teamA.goalsFor += scoreA;
        teamA.goalsAgainst += scoreB;
        teamB.goalsFor += scoreB;
        teamB.goalsAgainst += scoreA;

        if (scoreA > scoreB) {
            teamA.wins++;
            teamA.points += 3;
            teamB.losses++;
            return;
        }
        if (scoreB > scoreA) {
            teamB.wins++;
            teamB.points += 3;
            teamA.losses++;
            return;
        }

        teamA.draws++;
        teamB.draws++;
        teamA.points++;
        teamB.points++;
    }

    private static final class MutableStanding {
        private final Long teamId;
        private final String teamName;
        private final String teamColor;
        private int played;
        private int wins;
        private int draws;
        private int losses;
        private int goalsFor;
        private int goalsAgainst;
        private int points;

        private MutableStanding(Long teamId, String teamName, String teamColor) {
            this.teamId = teamId;
            this.teamName = teamName;
            this.teamColor = teamColor;
        }

        private SessionStandingsRowResponse toResponse() {
            return new SessionStandingsRowResponse(
                    teamId,
                    teamName,
                    teamColor,
                    played,
                    wins,
                    draws,
                    losses,
                    goalsFor,
                    goalsAgainst,
                    goalsFor - goalsAgainst,
                    points
            );
        }
    }
}
