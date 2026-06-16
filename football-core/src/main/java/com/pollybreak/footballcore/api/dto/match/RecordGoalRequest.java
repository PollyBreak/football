package com.pollybreak.footballcore.api.dto.match;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record RecordGoalRequest(
        @NotNull Long teamId,
        @NotNull Long scorerPlayerId,
        Boolean ownGoal,
        Long assistPlayerId,
        Long createdByUserId,
        Integer minuteInMatch,
        Integer secondInMatch,
        Map<String, Object> payload
) {
}
