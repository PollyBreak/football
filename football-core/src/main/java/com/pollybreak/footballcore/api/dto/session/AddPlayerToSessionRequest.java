package com.pollybreak.footballcore.api.dto.session;

import com.pollybreak.footballcore.domain.enums.PlayerPosition;
import jakarta.validation.constraints.NotNull;

public record AddPlayerToSessionRequest(
        @NotNull Long playerId,
        PlayerPosition position
) {
}
