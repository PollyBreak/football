package com.pollybreak.footballcore.api.dto.match;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record RecordPenaltyRequest(
        @NotNull Long teamId,
        Long playerId,
        Integer minuteInMatch,
        Integer secondInMatch,
        Long createdByUserId,
        Map<String, Object> payload
) {
}
