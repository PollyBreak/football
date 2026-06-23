package com.pollybreak.footballcore.api.dto.mvp;

import jakarta.validation.constraints.NotNull;

public record SessionMvpVoteRequest(
        @NotNull Long userId,
        @NotNull Long playerId
) {
}
