package com.pollybreak.footballcore.service;

import com.pollybreak.footballcore.api.dto.match.CreateNextSessionMatchRequest;
import com.pollybreak.footballcore.api.dto.match.CreateSessionMatchRequest;
import com.pollybreak.footballcore.api.dto.match.FinishSessionMatchRequest;
import com.pollybreak.footballcore.api.dto.match.StartSessionMatchRequest;
import com.pollybreak.footballcore.api.dto.match.UpdateSessionMatchScoreRequest;
import com.pollybreak.footballcore.api.dto.match.SessionMatchResponse;
import com.pollybreak.footballcore.api.dto.session.SessionStandingsResponse;
import com.pollybreak.footballcore.api.dto.session.SessionStandingsRowResponse;
import com.pollybreak.footballcore.domain.entity.GameSession;
import com.pollybreak.footballcore.domain.entity.MatchEvent;
import com.pollybreak.footballcore.domain.entity.SessionMatch;
import com.pollybreak.footballcore.domain.entity.SessionTeam;
import com.pollybreak.footballcore.domain.enums.MatchEventType;
import com.pollybreak.footballcore.domain.enums.MatchStatus;
import com.pollybreak.footballcore.domain.enums.SessionFormatType;
import com.pollybreak.footballcore.domain.enums.SessionStatus;
import com.pollybreak.footballcore.repository.GameSessionRepository;
import com.pollybreak.footballcore.repository.MatchEventRepository;
import com.pollybreak.footballcore.repository.SessionMatchRepository;
import com.pollybreak.footballcore.repository.SessionTeamRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final OverlayEventService overlayEventService;
    private final MatchEventRepository matchEventRepository;
    private final StreamBroadcastService streamBroadcastService;
    private final MatchPlayerService matchPlayerService;

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
        int matchNumber = request.matchNumber() != null ? request.matchNumber() : getNextMatchNumber(session.getId());
        match.setMatchNumber(matchNumber);
        match.setRoundNumber(resolveRoundNumber(session.getId(), matchNumber));
        match.setStatus(request.status() != null ? request.status() : MatchStatus.PLANNED);
        match.setPlannedDurationMinutes(request.plannedDurationMinutes() != null
                ? request.plannedDurationMinutes()
                : session.getPlannedMatchDurationMinutes());

        return SessionMatchResponse.fromEntity(sessionMatchRepository.save(match));
    }

    @Transactional
    public SessionMatchResponse createNext(Long sessionId, CreateNextSessionMatchRequest request) {
        GameSession session = gameSessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Game session not found: " + sessionId));
        List<SessionTeam> teams = sessionTeamRepository.findAllBySessionIdOrderByDisplayOrderAsc(sessionId);
        if (teams.size() < 2) {
            throw new IllegalArgumentException("At least two teams are required to create a match");
        }
        if (session.getFormatType() == SessionFormatType.DUEL) {
            return createNextDuelMatch(session, teams);
        }
        if (session.getFormatType() != SessionFormatType.ROUND_ROBIN) {
            throw new IllegalArgumentException("Automatic next match creation is only supported for round-robin and duel sessions");
        }

        List<TeamPair> pairs = orderRoundRobinPairs(buildTeamPairs(teams), request);
        int nextMatchNumber = getNextMatchNumber(sessionId);
        int roundNumber = resolveRoundNumber(sessionId, nextMatchNumber);
        List<SessionMatch> existingMatches = findBySessionId(sessionId);
        List<SessionMatch> currentRoundMatches = existingMatches.stream()
                .filter(match -> Objects.equals(
                        match.getRoundNumber() != null ? match.getRoundNumber() : resolveRoundNumber(sessionId, match.getMatchNumber()),
                        roundNumber
                ))
                .toList();
        List<TeamPair> availablePairs = pairs.stream()
                .filter(pair -> currentRoundMatches.stream().noneMatch(pair::matches))
                .toList();
        List<TeamPair> selectablePairs = availablePairs.isEmpty() ? pairs : availablePairs;

        TeamPair pair = resolveNextRoundRobinPair(nextMatchNumber, pairs, selectablePairs, existingMatches);

        SessionMatch match = new SessionMatch();
        match.setSession(session);
        match.setTeamA(pair.teamA());
        match.setTeamB(pair.teamB());
        match.setMatchNumber(nextMatchNumber);
        match.setRoundNumber(roundNumber);
        match.setStatus(MatchStatus.PLANNED);
        match.setPlannedDurationMinutes(session.getPlannedMatchDurationMinutes());

        return SessionMatchResponse.fromEntity(sessionMatchRepository.save(match));
    }

    private SessionMatchResponse createNextDuelMatch(GameSession session, List<SessionTeam> teams) {
        if (teams.size() != 2) {
            throw new IllegalArgumentException("Duel sessions require exactly two teams");
        }

        int nextMatchNumber = getNextMatchNumber(session.getId());
        boolean reverseTeams = nextMatchNumber % 2 == 0;
        SessionTeam teamA = reverseTeams ? teams.get(1) : teams.get(0);
        SessionTeam teamB = reverseTeams ? teams.get(0) : teams.get(1);

        SessionMatch match = new SessionMatch();
        match.setSession(session);
        match.setTeamA(teamA);
        match.setTeamB(teamB);
        match.setMatchNumber(nextMatchNumber);
        match.setRoundNumber(1);
        match.setStatus(MatchStatus.PLANNED);
        match.setPlannedDurationMinutes(session.getPlannedMatchDurationMinutes());

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
        OffsetDateTime startedAt = request.startedAt() != null ? request.startedAt() : OffsetDateTime.now();
        match.setStatus(MatchStatus.IN_PROGRESS);
        match.setStartedAt(startedAt);
        match.setEndedAt(null);
        markSessionInProgress(match.getSession(), startedAt);
        matchPlayerService.snapshotStartingLineups(match, startedAt);
        saveSystemEvent(match, MatchEventType.MATCH_STARTED);
        SessionMatchResponse response = SessionMatchResponse.fromEntity(match);
        overlayEventService.publishAfterCommit(
                OverlayEventService.MATCH_STARTED,
                match.getSession().getId(),
                match.getId()
        );
        return response;
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
        saveSystemEvent(match, MatchEventType.MATCH_FINISHED);
        SessionMatchResponse response = SessionMatchResponse.fromEntity(match);
        overlayEventService.publishAfterCommit(
                OverlayEventService.MATCH_FINISHED,
                match.getSession().getId(),
                match.getId()
        );
        return response;
    }

    private void markSessionInProgress(GameSession session, OffsetDateTime startedAt) {
        if (session.getStatus() != SessionStatus.PLANNED) {
            return;
        }
        session.setStatus(SessionStatus.IN_PROGRESS);
        if (session.getStartedAt() == null) {
            session.setStartedAt(startedAt);
        }
        session.setEndedAt(null);
    }

    @Transactional
    public SessionMatchResponse pause(Long matchId) {
        SessionMatch match = getById(matchId);
        if (match.getStatus() != MatchStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Only in-progress matches can be paused");
        }

        match.setStatus(MatchStatus.PAUSED);
        match.setEndedAt(OffsetDateTime.now());
        saveSystemEvent(match, MatchEventType.MATCH_PAUSED);
        SessionMatchResponse response = SessionMatchResponse.fromEntity(match);
        overlayEventService.publishAfterCommit(
                OverlayEventService.MATCH_PAUSED,
                match.getSession().getId(),
                match.getId()
        );
        return response;
    }

    @Transactional
    public SessionMatchResponse resume(Long matchId) {
        SessionMatch match = getById(matchId);
        if (match.getStatus() != MatchStatus.FINISHED && match.getStatus() != MatchStatus.PAUSED) {
            throw new IllegalArgumentException("Only finished or paused matches can be resumed");
        }

        OffsetDateTime resumedAt = OffsetDateTime.now();
        Duration elapsedBeforeFinish = resolveElapsedBeforeFinish(match, resumedAt);
        match.setStatus(MatchStatus.IN_PROGRESS);
        match.setStartedAt(resumedAt.minus(elapsedBeforeFinish));
        match.setEndedAt(null);
        match.setWinningTeam(null);
        saveSystemEvent(match, MatchEventType.MATCH_RESUMED);

        SessionMatchResponse response = SessionMatchResponse.fromEntity(match);
        overlayEventService.publishAfterCommit(
                OverlayEventService.MATCH_RESUMED,
                match.getSession().getId(),
                match.getId()
        );
        return response;
    }

    @Transactional
    public SessionMatch save(SessionMatch sessionMatch) {
        return sessionMatchRepository.save(sessionMatch);
    }

    private int resolveRoundNumber(Long sessionId, int matchNumber) {
        long teamCount = sessionTeamRepository.countBySessionId(sessionId);
        long matchesPerRound = Math.max(1, teamCount * (teamCount - 1) / 2);
        return (int) (((Math.max(1, matchNumber) - 1) / matchesPerRound) + 1);
    }

    private List<TeamPair> buildTeamPairs(List<SessionTeam> teams) {
        List<TeamPair> pairs = new ArrayList<>();
        for (int left = 0; left < teams.size(); left++) {
            for (int right = left + 1; right < teams.size(); right++) {
                pairs.add(new TeamPair(teams.get(left), teams.get(right)));
            }
        }
        return pairs;
    }

    private List<TeamPair> orderRoundRobinPairs(List<TeamPair> pairs, CreateNextSessionMatchRequest request) {
        List<TeamPair> orderedPairs = rotatePairToStart(pairs, request.firstPairKey());
        if (request.secondPairKey() == null || request.secondPairKey().isBlank()) {
            return orderedPairs;
        }

        int secondPairIndex = findPairIndex(orderedPairs, request.secondPairKey());
        if (secondPairIndex < 1) {
            return orderedPairs;
        }

        List<TeamPair> reorderedPairs = new ArrayList<>();
        reorderedPairs.add(orderedPairs.get(0));
        reorderedPairs.add(orderedPairs.get(secondPairIndex));
        for (int index = 1; index < orderedPairs.size(); index++) {
            if (index != secondPairIndex) {
                reorderedPairs.add(orderedPairs.get(index));
            }
        }
        return reorderedPairs;
    }

    private List<TeamPair> rotatePairToStart(List<TeamPair> pairs, String pairKey) {
        int selectedIndex = findPairIndex(pairs, pairKey);
        if (selectedIndex < 1) {
            return pairs;
        }

        List<TeamPair> rotatedPairs = new ArrayList<>(pairs.size());
        rotatedPairs.addAll(pairs.subList(selectedIndex, pairs.size()));
        rotatedPairs.addAll(pairs.subList(0, selectedIndex));
        return rotatedPairs;
    }

    private int findPairIndex(List<TeamPair> pairs, String pairKey) {
        if (pairKey == null || pairKey.isBlank()) {
            return -1;
        }
        for (int index = 0; index < pairs.size(); index++) {
            if (pairs.get(index).key().equals(pairKey)) {
                return index;
            }
        }
        return -1;
    }

    private TeamPair resolveNextRoundRobinPair(
            int nextMatchNumber,
            List<TeamPair> orderedPairs,
            List<TeamPair> selectablePairs,
            List<SessionMatch> existingMatches
    ) {
        TeamPair scheduledPair = orderedPairs.get((nextMatchNumber - 1) % orderedPairs.size());
        if (selectablePairs.stream().anyMatch(pair -> pair.key().equals(scheduledPair.key()))
                && !matchesPreviousMatch(scheduledPair, existingMatches)) {
            return scheduledPair;
        }

        return selectablePairs.stream()
                .filter(pair -> !matchesPreviousMatch(pair, existingMatches))
                .findFirst()
                .orElse(selectablePairs.get(0));
    }

    private boolean matchesPreviousMatch(TeamPair pair, List<SessionMatch> existingMatches) {
        return existingMatches.stream()
                .max(Comparator.comparing(SessionMatch::getMatchNumber))
                .map(pair::matches)
                .orElse(false);
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

    private Duration resolveElapsedBeforeFinish(SessionMatch match, OffsetDateTime fallbackEnd) {
        if (match.getStartedAt() == null) {
            return Duration.ZERO;
        }

        OffsetDateTime end = match.getEndedAt() != null ? match.getEndedAt() : fallbackEnd;
        Duration elapsed = Duration.between(match.getStartedAt(), end);
        return elapsed.isNegative() ? Duration.ZERO : elapsed;
    }

    private MatchEvent saveSystemEvent(SessionMatch match, MatchEventType eventType) {
        MatchEvent event = new MatchEvent();
        event.setMatch(match);
        event.setEventType(eventType);
        streamBroadcastService.attachActiveStreamTimecode(event, match.getSession().getId());
        return matchEventRepository.save(event);
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

    private record TeamPair(SessionTeam teamA, SessionTeam teamB) {
        private boolean matches(SessionMatch match) {
            return key().equals(pairKey(match.getTeamA().getId(), match.getTeamB().getId()));
        }

        private String key() {
            return pairKey(teamA.getId(), teamB.getId());
        }
    }

    private static String pairKey(Long teamAId, Long teamBId) {
        List<Long> ids = new ArrayList<>(List.of(teamAId, teamBId));
        Collections.sort(ids);
        return ids.get(0) + ":" + ids.get(1);
    }
}
