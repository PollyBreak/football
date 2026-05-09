package com.pollybreak.footballcore.api.dto.match;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record RecordAssistRequest(
        @NotNull Long teamId,
        @NotNull Long playerId,
        Long relatedPlayerId,
        Long createdByUserId,
        Integer minuteInMatch,
        Integer secondInMatch,
        Map<String, Object> payload
) {
}
