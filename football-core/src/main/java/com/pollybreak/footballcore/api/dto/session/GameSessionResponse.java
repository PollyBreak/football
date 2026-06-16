package com.pollybreak.footballcore.api.dto.session;

import com.pollybreak.footballcore.domain.entity.GameSession;
import com.pollybreak.footballcore.domain.entity.SessionTeam;
import com.pollybreak.footballcore.domain.enums.SessionFormatType;
import com.pollybreak.footballcore.domain.enums.SessionStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

public record GameSessionResponse(
        Long id,
        String title,
        LocalDate sessionDate,
        LocalTime sessionTime,
        String location,
        String locationUrl,
        String broadcastUrl,
        SessionFormatType formatType,
        SessionStatus status,
        Integer plannedMatchDurationMinutes,
        Integer maxPlayers,
        String notes,
        Long createdByUserId,
        OffsetDateTime createdAt,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        List<SessionTeamResponse> teams
) {
    public static GameSessionResponse fromEntity(GameSession session, List<SessionTeam> teams) {
        return new GameSessionResponse(
                session.getId(),
                session.getTitle(),
                session.getSessionDate(),
                session.getSessionTime(),
                session.getLocation(),
                session.getLocationUrl(),
                session.getBroadcastUrl(),
                session.getFormatType(),
                session.getStatus(),
                session.getPlannedMatchDurationMinutes(),
                session.getMaxPlayers(),
                session.getNotes(),
                session.getCreatedBy() != null ? session.getCreatedBy().getId() : null,
                session.getCreatedAt(),
                session.getStartedAt(),
                session.getEndedAt(),
                teams.stream().map(SessionTeamResponse::fromEntity).toList()
        );
    }
}
