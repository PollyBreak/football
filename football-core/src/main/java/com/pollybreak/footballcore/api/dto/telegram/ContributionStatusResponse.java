package com.pollybreak.footballcore.api.dto.telegram;

public record ContributionStatusResponse(
        Long playerId,
        String displayName,
        boolean paid
) {
}
