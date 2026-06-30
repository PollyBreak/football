package com.pollybreak.footballcore.api.dto.match;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record RecordSubstitutionRequest(
        @NotNull Long teamId,
        @NotNull Long playerInId,
        @NotNull Long playerOutId,
        Long createdByUserId,
        Integer minuteInMatch,
        Integer secondInMatch,
        Map<String, Object> payload
) {
}
