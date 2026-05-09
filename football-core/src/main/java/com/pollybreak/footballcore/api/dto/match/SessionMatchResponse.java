package com.pollybreak.footballcore.api.dto.match;

import com.pollybreak.footballcore.domain.entity.SessionMatch;
import com.pollybreak.footballcore.domain.enums.MatchStatus;
import java.time.OffsetDateTime;

public record SessionMatchResponse(
        Long id,
        Long sessionId,
        Long teamAId,
        String teamAName,
        Long teamBId,
        String teamBName,
        Integer matchNumber,
        MatchStatus status,
        Integer plannedDurationMinutes,
        Integer teamAScore,
        Integer teamBScore,
        Long winningTeamId,
        OffsetDateTime createdAt,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt
) {
    public static SessionMatchResponse fromEntity(SessionMatch match) {
        return new SessionMatchResponse(
                match.getId(),
                match.getSession().getId(),
                match.getTeamA().getId(),
                match.getTeamA().getName(),
                match.getTeamB().getId(),
                match.getTeamB().getName(),
                match.getMatchNumber(),
                match.getStatus(),
                match.getPlannedDurationMinutes(),
                match.getTeamAScore(),
                match.getTeamBScore(),
                match.getWinningTeam() != null ? match.getWinningTeam().getId() : null,
                match.getCreatedAt(),
                match.getStartedAt(),
                match.getEndedAt()
        );
    }
}
