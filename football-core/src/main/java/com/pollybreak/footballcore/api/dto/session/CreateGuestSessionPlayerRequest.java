package com.pollybreak.footballcore.api.dto.session;

import com.pollybreak.footballcore.domain.enums.PlayerPosition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateGuestSessionPlayerRequest(
        @NotBlank String name,
        @NotNull PlayerPosition position
) {
}
