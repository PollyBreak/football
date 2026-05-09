package com.pollybreak.footballcore.api.dto.session;

import com.pollybreak.footballcore.domain.entity.SessionTeamPlayer;
import com.pollybreak.footballcore.domain.enums.PlayerPosition;
import java.time.OffsetDateTime;

public record SessionTeamPlayerResponse(
        Long id,
        Long sessionTeamId,
        Long playerId,
        String playerName,
        String photoUrl,
        PlayerPosition position,
        boolean active,
        OffsetDateTime joinedAt,
        OffsetDateTime leftAt
) {
    public static SessionTeamPlayerResponse fromEntity(SessionTeamPlayer teamPlayer) {
        String playerName = teamPlayer.getPlayer().getFirstName()
                + (teamPlayer.getPlayer().getLastName() != null ? " " + teamPlayer.getPlayer().getLastName() : "");

        return new SessionTeamPlayerResponse(
                teamPlayer.getId(),
                teamPlayer.getSessionTeam().getId(),
                teamPlayer.getPlayer().getId(),
                playerName,
                teamPlayer.getPlayer().getUser() != null ? teamPlayer.getPlayer().getUser().getPhotoUrl() : null,
                teamPlayer.getPosition(),
                teamPlayer.isActive(),
                teamPlayer.getJoinedAt(),
                teamPlayer.getLeftAt()
        );
    }
}
