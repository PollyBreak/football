package com.pollybreak.footballcore.api.dto.telegram;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateContributionStatusRequest(
        @NotNull Long userId,
        @NotEmpty List<@NotNull Long> playerIds,
        @NotNull Boolean paid
) {
}
