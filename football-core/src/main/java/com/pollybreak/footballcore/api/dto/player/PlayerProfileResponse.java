package com.pollybreak.footballcore.api.dto.player;

import com.pollybreak.footballcore.domain.entity.Player;
import com.pollybreak.footballcore.domain.enums.PlayerPosition;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record PlayerProfileResponse(
        Long playerId,
        Long userId,
        Long telegramId,
        String username,
        String displayName,
        String firstName,
        String lastName,
        String nickname,
        String photoUrl,
        String manualPhotoUrl,
        String telegramPhotoUrl,
        String homeCity,
        LocalDate birthDate,
        PlayerPosition defaultPosition,
        Integer rating,
        PlayerStatsResponse stats,
        List<PlayerSessionSummaryResponse> sessions,
        boolean active,
        OffsetDateTime createdAt
) {
    public static PlayerProfileResponse fromEntity(Player player) {
        return fromEntity(player, new PlayerStatsResponse(0, 0), List.of());
    }

    public static PlayerProfileResponse fromEntity(
            Player player,
            PlayerStatsResponse stats,
            List<PlayerSessionSummaryResponse> sessions
    ) {
        return new PlayerProfileResponse(
                player.getId(),
                player.getUser() != null ? player.getUser().getId() : null,
                player.getUser() != null ? player.getUser().getTelegramId() : null,
                player.getUser() != null ? player.getUser().getUsername() : null,
                player.getUser() != null ? player.getUser().getDisplayName() : null,
                player.getFirstName(),
                player.getLastName(),
                player.getNickname(),
                player.getEffectivePhotoUrl(),
                player.getManualPhotoUrl(),
                player.getTelegramPhotoUrl(),
                player.getHomeCity(),
                player.getBirthDate(),
                player.getDefaultPosition(),
                player.getRating(),
                stats,
                sessions,
                player.isActive(),
                player.getCreatedAt()
        );
    }
}
