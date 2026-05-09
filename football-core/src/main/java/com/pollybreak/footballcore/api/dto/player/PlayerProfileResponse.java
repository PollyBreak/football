package com.pollybreak.footballcore.api.dto.player;

import com.pollybreak.footballcore.domain.entity.Player;
import com.pollybreak.footballcore.domain.enums.PlayerPosition;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record PlayerProfileResponse(
        Long playerId,
        Long userId,
        Long telegramId,
        String username,
        String displayName,
        String firstName,
        String lastName,
        String nickname,
        String homeCity,
        LocalDate birthDate,
        PlayerPosition defaultPosition,
        boolean active,
        OffsetDateTime createdAt
) {
    public static PlayerProfileResponse fromEntity(Player player) {
        return new PlayerProfileResponse(
                player.getId(),
                player.getUser() != null ? player.getUser().getId() : null,
                player.getUser() != null ? player.getUser().getTelegramId() : null,
                player.getUser() != null ? player.getUser().getUsername() : null,
                player.getUser() != null ? player.getUser().getDisplayName() : null,
                player.getFirstName(),
                player.getLastName(),
                player.getNickname(),
                player.getHomeCity(),
                player.getBirthDate(),
                player.getDefaultPosition(),
                player.isActive(),
                player.getCreatedAt()
        );
    }
}
