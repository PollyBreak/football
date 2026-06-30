package com.pollybreak.footballcore.api.dto.match;

import com.pollybreak.footballcore.domain.entity.MatchPlayer;
import java.time.OffsetDateTime;

public record MatchPlayerResponse(
        Long id,
        Long matchId,
        Long teamId,
        String teamName,
        Long playerId,
        String playerName,
        String playerDisplayName,
        String playerUsername,
        String playerPhotoUrl,
        String playerTelegramPhotoUrl,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        String source,
        OffsetDateTime createdAt
) {
    public static MatchPlayerResponse fromEntity(MatchPlayer matchPlayer) {
        String playerName = matchPlayer.getPlayer().getFirstName()
                + (matchPlayer.getPlayer().getLastName() != null ? " " + matchPlayer.getPlayer().getLastName() : "");

        return new MatchPlayerResponse(
                matchPlayer.getId(),
                matchPlayer.getMatch().getId(),
                matchPlayer.getTeam().getId(),
                matchPlayer.getTeam().getName(),
                matchPlayer.getPlayer().getId(),
                playerName,
                matchPlayer.getPlayer().getUser() != null ? matchPlayer.getPlayer().getUser().getDisplayName() : null,
                matchPlayer.getPlayer().getUser() != null ? matchPlayer.getPlayer().getUser().getUsername() : null,
                matchPlayer.getPlayer().getEffectivePhotoUrl(),
                matchPlayer.getPlayer().getTelegramPhotoUrl(),
                matchPlayer.getStartedAt(),
                matchPlayer.getEndedAt(),
                matchPlayer.getSource(),
                matchPlayer.getCreatedAt()
        );
    }
}
