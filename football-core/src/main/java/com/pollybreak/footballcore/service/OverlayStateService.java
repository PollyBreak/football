package com.pollybreak.footballcore.service;

import com.pollybreak.footballcore.api.dto.match.MatchEventResponse;
import com.pollybreak.footballcore.api.dto.match.SessionMatchResponse;
import com.pollybreak.footballcore.api.dto.overlay.OverlayStateResponse;
import com.pollybreak.footballcore.api.dto.overlay.OverlayTeamResponse;
import com.pollybreak.footballcore.api.dto.session.SessionStandingsRowResponse;
import com.pollybreak.footballcore.api.dto.session.SessionTeamPlayerResponse;
import com.pollybreak.footballcore.domain.entity.SessionMatch;
import com.pollybreak.footballcore.domain.entity.SessionTeam;
import com.pollybreak.footballcore.domain.enums.MatchStatus;
import com.pollybreak.footballcore.repository.GameSessionRepository;
import com.pollybreak.footballcore.repository.MatchEventRepository;
import com.pollybreak.footballcore.repository.SessionMatchRepository;
import com.pollybreak.footballcore.repository.SessionTeamPlayerRepository;
import com.pollybreak.footballcore.repository.SessionTeamRepository;
import java.time.OffsetDateTime;
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
public class OverlayStateService {

    private final GameSessionRepository gameSessionRepository;
    private final SessionMatchRepository sessionMatchRepository;
    private final SessionTeamRepository sessionTeamRepository;
    private final SessionTeamPlayerRepository sessionTeamPlayerRepository;
    private final MatchEventRepository matchEventRepository;

    public OverlayStateResponse getState(Long sessionId, Long preferredMatchId) {
        if (!gameSessionRepository.existsById(sessionId)) {
            throw new IllegalArgumentException("Game session not found: " + sessionId);
        }

        List<SessionMatch> matches = sessionMatchRepository.findAllBySessionIdOrderByMatchNumberAsc(sessionId);
        List<SessionMatchResponse> matchResponses = matches.stream()
                .map(SessionMatchResponse::fromEntity)
                .toList();

        List<SessionMatchResponse> inProgressMatches = matchResponses.stream()
                .filter(match -> match.status() == MatchStatus.IN_PROGRESS)
                .toList();

        SessionMatchResponse currentMatch = resolveCurrentMatch(matchResponses, preferredMatchId);
        List<MatchEventResponse> currentMatchEvents = currentMatch == null
                ? List.of()
                : matchEventRepository.findAllByMatchIdOrderByEventTimeAscIdAsc(currentMatch.id()).stream()
                .map(MatchEventResponse::fromEntity)
                .toList();
        List<MatchEventResponse> sessionEvents = matchEventRepository.findAllByMatchSessionIdOrderByEventTimeAscIdAsc(sessionId).stream()
                .map(MatchEventResponse::fromEntity)
                .toList();
        List<SessionStandingsRowResponse> standings = buildStandings(sessionId, matches);
        SessionMatchResponse lastFinishedMatch = resolveLastFinishedMatch(matchResponses);
        SessionMatchResponse nextMatch = resolveNextMatch(matchResponses);
        List<MatchEventResponse> lastFinishedMatchEvents = lastFinishedMatch == null
                ? List.of()
                : matchEventRepository.findAllByMatchIdOrderByEventTimeAscIdAsc(lastFinishedMatch.id()).stream()
                .map(MatchEventResponse::fromEntity)
                .toList();

        return new OverlayStateResponse(
                sessionId,
                OffsetDateTime.now(),
                currentMatch,
                inProgressMatches,
                matchResponses,
                getTeams(sessionId),
                currentMatchEvents,
                sessionEvents,
                standings,
                lastFinishedMatch,
                nextMatch,
                lastFinishedMatchEvents
        );
    }

    private SessionMatchResponse resolveCurrentMatch(List<SessionMatchResponse> matches, Long preferredMatchId) {
        if (preferredMatchId != null) {
            return matches.stream()
                    .filter(match -> match.id().equals(preferredMatchId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Match does not belong to this session"));
        }

        return matches.stream()
                .filter(match -> match.status() == MatchStatus.IN_PROGRESS)
                .max(Comparator
                        .comparing(SessionMatchResponse::startedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(SessionMatchResponse::id))
                .orElse(null);
    }

    private SessionMatchResponse resolveLastFinishedMatch(List<SessionMatchResponse> matches) {
        return matches.stream()
                .filter(match -> match.status() == MatchStatus.FINISHED)
                .max(Comparator
                        .comparing(SessionMatchResponse::endedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(SessionMatchResponse::id))
                .orElse(null);
    }

    private SessionMatchResponse resolveNextMatch(List<SessionMatchResponse> matches) {
        return matches.stream()
                .filter(match -> match.status() == MatchStatus.PLANNED)
                .min(Comparator
                        .comparing(SessionMatchResponse::roundNumber, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(SessionMatchResponse::matchNumber, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(SessionMatchResponse::id))
                .orElse(null);
    }

    private List<SessionStandingsRowResponse> buildStandings(Long sessionId, List<SessionMatch> matches) {
        List<SessionTeam> teams = sessionTeamRepository.findAllBySessionIdOrderByDisplayOrderAsc(sessionId);
        Map<Long, MutableStanding> table = new LinkedHashMap<>();
        for (SessionTeam team : teams) {
            table.put(team.getId(), new MutableStanding(team.getId(), team.getName(), team.getColor()));
        }

        matches.stream()
                .filter(match -> match.getStatus() == MatchStatus.FINISHED)
                .forEach(match -> applyMatchResult(table, match));

        return table.values().stream()
                .map(MutableStanding::toResponse)
                .sorted(Comparator
                        .comparingInt(SessionStandingsRowResponse::points).reversed()
                        .thenComparingInt(SessionStandingsRowResponse::goalDifference).reversed()
                        .thenComparingInt(SessionStandingsRowResponse::goalsFor).reversed()
                        .thenComparing(SessionStandingsRowResponse::teamName))
                .toList();
    }

    private void applyMatchResult(Map<Long, MutableStanding> table, SessionMatch match) {
        MutableStanding teamA = table.get(match.getTeamA().getId());
        MutableStanding teamB = table.get(match.getTeamB().getId());
        if (teamA == null || teamB == null) {
            return;
        }

        int teamAScore = scoreOrZero(match.getTeamAScore());
        int teamBScore = scoreOrZero(match.getTeamBScore());
        teamA.apply(teamAScore, teamBScore);
        teamB.apply(teamBScore, teamAScore);
    }

    private int scoreOrZero(Integer score) {
        return score == null ? 0 : score;
    }

    private List<OverlayTeamResponse> getTeams(Long sessionId) {
        return sessionTeamRepository.findAllBySessionIdOrderByDisplayOrderAsc(sessionId).stream()
                .map(this::toOverlayTeam)
                .toList();
    }

    private OverlayTeamResponse toOverlayTeam(SessionTeam team) {
        List<SessionTeamPlayerResponse> players = sessionTeamPlayerRepository
                .findAllBySessionTeamIdAndActiveTrue(team.getId())
                .stream()
                .map(SessionTeamPlayerResponse::fromEntity)
                .toList();

        return new OverlayTeamResponse(
                team.getId(),
                team.getName(),
                team.getColor(),
                team.getDisplayOrder(),
                players
        );
    }

    private static class MutableStanding {
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

        private void apply(int scored, int conceded) {
            played++;
            goalsFor += scored;
            goalsAgainst += conceded;

            if (scored > conceded) {
                wins++;
                points += 3;
            } else if (scored == conceded) {
                draws++;
                points++;
            } else {
                losses++;
            }
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
