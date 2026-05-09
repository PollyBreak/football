package com.pollybreak.footballcore.api.dto.session;

import com.pollybreak.footballcore.domain.entity.SessionTeam;
import java.time.OffsetDateTime;

public record SessionTeamResponse(
        Long id,
        Long sessionId,
        String name,
        String color,
        Integer displayOrder,
        OffsetDateTime createdAt
) {
    public static SessionTeamResponse fromEntity(SessionTeam team) {
        return new SessionTeamResponse(
                team.getId(),
                team.getSession().getId(),
                team.getName(),
                team.getColor(),
                team.getDisplayOrder(),
                team.getCreatedAt()
        );
    }
}
