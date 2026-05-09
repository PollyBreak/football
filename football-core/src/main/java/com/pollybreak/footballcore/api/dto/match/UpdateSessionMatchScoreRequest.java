package com.pollybreak.footballcore.api.dto.match;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateSessionMatchScoreRequest(
        @NotNull @Min(0) Integer teamAScore,
        @NotNull @Min(0) Integer teamBScore
) {
}
