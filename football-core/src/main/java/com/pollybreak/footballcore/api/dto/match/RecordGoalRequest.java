package com.pollybreak.footballcore.api.dto.match;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record RecordGoalRequest(
        @NotNull Long teamId,
        @NotNull Long scorerPlayerId,
        Long assistPlayerId,
        Long createdByUserId,
        Integer minuteInMatch,
        Integer secondInMatch,
        Map<String, Object> payload
) {
}
