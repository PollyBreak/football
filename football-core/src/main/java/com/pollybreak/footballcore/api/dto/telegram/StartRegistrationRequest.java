package com.pollybreak.footballcore.api.dto.telegram;

import jakarta.validation.constraints.NotNull;

public record StartRegistrationRequest(
        @NotNull Long userId
) {
}
