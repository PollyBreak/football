package com.pollybreak.footballcore.api.dto.session;

import com.pollybreak.footballcore.domain.entity.SessionPlayer;
import com.pollybreak.footballcore.domain.enums.PlayerPosition;
import java.time.OffsetDateTime;

public record SessionPlayerResponse(
        Long id,
        Long sessionId,
        Long playerId,
        String firstName,
        String lastName,
        String nickname,
        String photoUrl,
        PlayerPosition position,
        boolean active,
        OffsetDateTime joinedAt,
        OffsetDateTime leftAt
) {
    public static SessionPlayerResponse fromEntity(SessionPlayer sessionPlayer) {
        return new SessionPlayerResponse(
                sessionPlayer.getId(),
                sessionPlayer.getSession().getId(),
                sessionPlayer.getPlayer().getId(),
                sessionPlayer.getPlayer().getFirstName(),
                sessionPlayer.getPlayer().getLastName(),
                sessionPlayer.getPlayer().getNickname(),
                sessionPlayer.getPlayer().getUser() != null ? sessionPlayer.getPlayer().getUser().getPhotoUrl() : null,
                sessionPlayer.getPosition(),
                sessionPlayer.isActive(),
                sessionPlayer.getJoinedAt(),
                sessionPlayer.getLeftAt()
        );
    }
}
