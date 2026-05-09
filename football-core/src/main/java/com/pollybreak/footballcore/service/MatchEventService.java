package com.pollybreak.footballcore.service;

import com.pollybreak.footballcore.api.dto.match.GoalRecordedResponse;
import com.pollybreak.footballcore.api.dto.match.MatchEventResponse;
import com.pollybreak.footballcore.api.dto.match.RecordAssistRequest;
import com.pollybreak.footballcore.api.dto.match.RecordGoalRequest;
import com.pollybreak.footballcore.domain.entity.AppUser;
import com.pollybreak.footballcore.domain.entity.MatchEvent;
import com.pollybreak.footballcore.domain.entity.Player;
import com.pollybreak.footballcore.domain.entity.SessionMatch;
import com.pollybreak.footballcore.domain.entity.SessionTeam;
import com.pollybreak.footballcore.domain.enums.MatchEventType;
import com.pollybreak.footballcore.repository.AppUserRepository;
import com.pollybreak.footballcore.repository.MatchEventRepository;
import com.pollybreak.footballcore.repository.PlayerRepository;
import com.pollybreak.footballcore.repository.SessionMatchRepository;
import com.pollybreak.footballcore.repository.SessionTeamRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchEventService {

    private final MatchEventRepository matchEventRepository;
    private final SessionMatchRepository sessionMatchRepository;
    private final SessionTeamRepository sessionTeamRepository;
    private final PlayerRepository playerRepository;
    private final AppUserRepository appUserRepository;

    public List<MatchEvent> findByMatchId(Long matchId) {
        return matchEventRepository.findAllByMatchIdOrderByEventTimeAscIdAsc(matchId);
    }

    public List<MatchEventResponse> findResponsesByMatchId(Long matchId) {
        return findByMatchId(matchId).stream()
                .map(MatchEventResponse::fromEntity)
                .toList();
    }

    public MatchEvent getById(Long id) {
        return matchEventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Match event not found: " + id));
    }

    @Transactional
    public GoalRecordedResponse recordGoal(Long matchId, RecordGoalRequest request) {
        SessionMatch match = getMatch(matchId);
        SessionTeam team = getTeamForMatch(match, request.teamId());
        Player scorer = getPlayer(request.scorerPlayerId());
        AppUser createdBy = getUserOrNull(request.createdByUserId());

        MatchEvent goalEvent = new MatchEvent();
        goalEvent.setMatch(match);
        goalEvent.setEventType(MatchEventType.GOAL);
        goalEvent.setTeam(team);
        goalEvent.setPlayer(scorer);
        goalEvent.setMinuteInMatch(request.minuteInMatch());
        goalEvent.setSecondInMatch(request.secondInMatch());
        goalEvent.setCreatedBy(createdBy);
        goalEvent.setPayload(request.payload());
        MatchEvent savedGoalEvent = matchEventRepository.save(goalEvent);

        incrementScore(match, team);

        MatchEvent savedAssistEvent = null;
        if (request.assistPlayerId() != null) {
            Player assistPlayer = getPlayer(request.assistPlayerId());
            MatchEvent assistEvent = new MatchEvent();
            assistEvent.setMatch(match);
            assistEvent.setEventType(MatchEventType.ASSIST);
            assistEvent.setTeam(team);
            assistEvent.setPlayer(assistPlayer);
            assistEvent.setRelatedPlayer(scorer);
            assistEvent.setLinkedEvent(savedGoalEvent);
            assistEvent.setMinuteInMatch(request.minuteInMatch());
            assistEvent.setSecondInMatch(request.secondInMatch());
            assistEvent.setCreatedBy(createdBy);
            assistEvent.setPayload(request.payload());
            savedAssistEvent = matchEventRepository.save(assistEvent);
        }

        return new GoalRecordedResponse(
                com.pollybreak.footballcore.api.dto.match.SessionMatchResponse.fromEntity(match),
                MatchEventResponse.fromEntity(savedGoalEvent),
                savedAssistEvent != null ? MatchEventResponse.fromEntity(savedAssistEvent) : null
        );
    }

    @Transactional
    public MatchEventResponse recordAssist(Long matchId, RecordAssistRequest request) {
        SessionMatch match = getMatch(matchId);
        SessionTeam team = getTeamForMatch(match, request.teamId());
        Player player = getPlayer(request.playerId());

        MatchEvent assistEvent = new MatchEvent();
        assistEvent.setMatch(match);
        assistEvent.setEventType(MatchEventType.ASSIST);
        assistEvent.setTeam(team);
        assistEvent.setPlayer(player);
        assistEvent.setMinuteInMatch(request.minuteInMatch());
        assistEvent.setSecondInMatch(request.secondInMatch());
        assistEvent.setCreatedBy(getUserOrNull(request.createdByUserId()));
        assistEvent.setPayload(request.payload());

        if (request.relatedPlayerId() != null) {
            assistEvent.setRelatedPlayer(getPlayer(request.relatedPlayerId()));
        }

        return MatchEventResponse.fromEntity(matchEventRepository.save(assistEvent));
    }

    @Transactional
    public void deleteEvent(Long matchId, Long eventId) {
        SessionMatch match = getMatch(matchId);
        MatchEvent event = matchEventRepository.findByIdAndMatchId(eventId, matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match event not found for this match"));

        if (event.getEventType() == MatchEventType.GOAL) {
            rollbackScore(match, event.getTeam());
            matchEventRepository.findAllByLinkedEventId(event.getId())
                    .forEach(matchEventRepository::delete);
        }

        matchEventRepository.delete(event);
    }

    @Transactional
    public MatchEvent save(MatchEvent matchEvent) {
        return matchEventRepository.save(matchEvent);
    }

    private SessionMatch getMatch(Long matchId) {
        return sessionMatchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Session match not found: " + matchId));
    }

    private SessionTeam getTeamForMatch(SessionMatch match, Long teamId) {
        SessionTeam team = sessionTeamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Session team not found: " + teamId));
        boolean belongsToMatch = teamId.equals(match.getTeamA().getId()) || teamId.equals(match.getTeamB().getId());
        if (!belongsToMatch) {
            throw new IllegalArgumentException("Team does not belong to this match");
        }
        return team;
    }

    private Player getPlayer(Long playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));
    }

    private AppUser getUserOrNull(Long userId) {
        if (userId == null) {
            return null;
        }
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private void incrementScore(SessionMatch match, SessionTeam scoringTeam) {
        if (scoringTeam.getId().equals(match.getTeamA().getId())) {
            match.setTeamAScore(match.getTeamAScore() + 1);
            return;
        }
        if (scoringTeam.getId().equals(match.getTeamB().getId())) {
            match.setTeamBScore(match.getTeamBScore() + 1);
            return;
        }
        throw new IllegalArgumentException("Scoring team does not belong to this match");
    }

    private void rollbackScore(SessionMatch match, SessionTeam scoringTeam) {
        if (scoringTeam == null) {
            throw new IllegalArgumentException("Goal event has no team and score cannot be rolled back");
        }
        if (scoringTeam.getId().equals(match.getTeamA().getId())) {
            match.setTeamAScore(Math.max(0, match.getTeamAScore() - 1));
            return;
        }
        if (scoringTeam.getId().equals(match.getTeamB().getId())) {
            match.setTeamBScore(Math.max(0, match.getTeamBScore() - 1));
            return;
        }
        throw new IllegalArgumentException("Scoring team does not belong to this match");
    }
}
