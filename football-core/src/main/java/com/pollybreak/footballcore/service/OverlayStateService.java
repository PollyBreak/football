package com.pollybreak.footballcore.service;

import com.pollybreak.footballcore.api.dto.match.MatchEventResponse;
import com.pollybreak.footballcore.api.dto.match.SessionMatchResponse;
import com.pollybreak.footballcore.api.dto.overlay.OverlayStateResponse;
import com.pollybreak.footballcore.api.dto.overlay.OverlayTeamResponse;
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
import java.util.List;
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

        return new OverlayStateResponse(
                sessionId,
                OffsetDateTime.now(),
                currentMatch,
                inProgressMatches,
                matchResponses,
                getTeams(sessionId),
                currentMatchEvents
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
}
