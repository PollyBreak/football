package com.pollybreak.footballcore.api.dto.match;

import jakarta.validation.constraints.Min;
import java.time.OffsetDateTime;

public record FinishSessionMatchRequest(
        @Min(0) Integer teamAScore,
        @Min(0) Integer teamBScore,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt
) {
}
