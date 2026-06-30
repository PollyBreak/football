package com.pollybreak.footballcore.api.dto.session;

import com.pollybreak.footballcore.domain.entity.MatchPlayer;
import com.pollybreak.footballcore.domain.entity.SessionTeamPlayer;
import com.pollybreak.footballcore.domain.enums.PlayerPosition;
import java.time.OffsetDateTime;

public record SessionTeamPlayerResponse(
        Long id,
        Long sessionTeamId,
        Long playerId,
        String playerName,
        String playerDisplayName,
        String playerUsername,
        String photoUrl,
        String telegramPhotoUrl,
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
                teamPlayer.getPlayer().getUser() != null ? teamPlayer.getPlayer().getUser().getDisplayName() : null,
                teamPlayer.getPlayer().getUser() != null ? teamPlayer.getPlayer().getUser().getUsername() : null,
                teamPlayer.getPlayer().getEffectivePhotoUrl(),
                teamPlayer.getPlayer().getTelegramPhotoUrl(),
                teamPlayer.getPosition(),
                teamPlayer.isActive(),
                teamPlayer.getJoinedAt(),
                teamPlayer.getLeftAt()
        );
    }

    public static SessionTeamPlayerResponse fromMatchPlayer(MatchPlayer matchPlayer) {
        String playerName = matchPlayer.getPlayer().getFirstName()
                + (matchPlayer.getPlayer().getLastName() != null ? " " + matchPlayer.getPlayer().getLastName() : "");

        return new SessionTeamPlayerResponse(
                matchPlayer.getId(),
                matchPlayer.getTeam().getId(),
                matchPlayer.getPlayer().getId(),
                playerName,
                matchPlayer.getPlayer().getUser() != null ? matchPlayer.getPlayer().getUser().getDisplayName() : null,
                matchPlayer.getPlayer().getUser() != null ? matchPlayer.getPlayer().getUser().getUsername() : null,
                matchPlayer.getPlayer().getEffectivePhotoUrl(),
                matchPlayer.getPlayer().getTelegramPhotoUrl(),
                null,
                matchPlayer.getEndedAt() == null,
                matchPlayer.getStartedAt(),
                matchPlayer.getEndedAt()
        );
    }
}
