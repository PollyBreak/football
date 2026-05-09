package com.pollybreak.footballcore.api.dto.match;

import com.pollybreak.footballcore.domain.enums.MatchStatus;
import jakarta.validation.constraints.NotNull;

public record CreateSessionMatchRequest(
        @NotNull Long teamAId,
        @NotNull Long teamBId,
        Integer matchNumber,
        MatchStatus status,
        Integer plannedDurationMinutes
) {
}
