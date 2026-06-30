package com.pollybreak.footballcore.service;

import com.pollybreak.footballcore.domain.entity.MatchPlayer;
import com.pollybreak.footballcore.domain.entity.Player;
import com.pollybreak.footballcore.domain.entity.SessionMatch;
import com.pollybreak.footballcore.domain.entity.SessionTeam;
import com.pollybreak.footballcore.domain.entity.SessionTeamPlayer;
import com.pollybreak.footballcore.repository.MatchPlayerRepository;
import com.pollybreak.footballcore.repository.SessionTeamPlayerRepository;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchPlayerService {

    private final MatchPlayerRepository matchPlayerRepository;
    private final SessionTeamPlayerRepository sessionTeamPlayerRepository;

    public List<MatchPlayer> findByMatchId(Long matchId) {
        return matchPlayerRepository.findAllByMatchIdOrderByStartedAtAscIdAsc(matchId);
    }

    @Transactional
    public void snapshotStartingLineups(SessionMatch match, OffsetDateTime startedAt) {
        if (matchPlayerRepository.existsByMatchId(match.getId())) {
            return;
        }

        snapshotTeam(match, match.getTeamA(), startedAt);
        snapshotTeam(match, match.getTeamB(), startedAt);
    }

    @Transactional
    public void applySubstitution(
            SessionMatch match,
            SessionTeam team,
            Player playerIn,
            Player playerOut,
            OffsetDateTime substitutedAt
    ) {
        if (!matchPlayerRepository.existsByMatchId(match.getId())) {
            snapshotStartingLineups(match, match.getStartedAt() != null ? match.getStartedAt() : substitutedAt);
        }

        MatchPlayer outgoing = matchPlayerRepository
                .findByMatchIdAndTeamIdAndPlayerIdAndEndedAtIsNull(match.getId(), team.getId(), playerOut.getId())
                .orElseThrow(() -> new IllegalArgumentException("Outgoing player is not active in this match team"));
        outgoing.setEndedAt(substitutedAt);

        matchPlayerRepository.findByMatchIdAndTeamIdAndPlayerIdAndEndedAtIsNull(match.getId(), team.getId(), playerIn.getId())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Incoming player is already active in this match team");
                });

        MatchPlayer incoming = new MatchPlayer();
        incoming.setMatch(match);
        incoming.setTeam(team);
        incoming.setPlayer(playerIn);
        incoming.setStartedAt(substitutedAt);
        incoming.setSource(MatchPlayer.SOURCE_SUBSTITUTION);
        matchPlayerRepository.save(incoming);
    }

    private void snapshotTeam(SessionMatch match, SessionTeam team, OffsetDateTime startedAt) {
        List<SessionTeamPlayer> players = sessionTeamPlayerRepository.findAllBySessionTeamIdAndActiveTrue(team.getId());
        players.forEach(teamPlayer -> {
            MatchPlayer matchPlayer = new MatchPlayer();
            matchPlayer.setMatch(match);
            matchPlayer.setTeam(team);
            matchPlayer.setPlayer(teamPlayer.getPlayer());
            matchPlayer.setStartedAt(startedAt);
            matchPlayer.setSource(MatchPlayer.SOURCE_START_SNAPSHOT);
            matchPlayerRepository.save(matchPlayer);
        });
    }
}
