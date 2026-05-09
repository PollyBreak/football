package com.pollybreak.footballcore.api.dto.session;

import com.pollybreak.footballcore.domain.entity.SessionWaitlistEntry;
import com.pollybreak.footballcore.domain.enums.PlayerPosition;
import java.time.OffsetDateTime;

public record SessionWaitlistResponse(
        Long id,
        Long sessionId,
        Long playerId,
        String firstName,
        String lastName,
        String nickname,
        String photoUrl,
        PlayerPosition position,
        boolean active,
        OffsetDateTime queuedAt,
        OffsetDateTime leftAt
) {
    public static SessionWaitlistResponse fromEntity(SessionWaitlistEntry entry) {
        return new SessionWaitlistResponse(
                entry.getId(),
                entry.getSession().getId(),
                entry.getPlayer().getId(),
                entry.getPlayer().getFirstName(),
                entry.getPlayer().getLastName(),
                entry.getPlayer().getNickname(),
                entry.getPlayer().getUser() != null ? entry.getPlayer().getUser().getPhotoUrl() : null,
                entry.getPosition(),
                entry.isActive(),
                entry.getQueuedAt(),
                entry.getLeftAt()
        );
    }
}
