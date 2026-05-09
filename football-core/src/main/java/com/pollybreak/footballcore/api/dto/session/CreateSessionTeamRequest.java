package com.pollybreak.footballcore.api.dto.session;

import jakarta.validation.constraints.NotBlank;

public record CreateSessionTeamRequest(
        @NotBlank String name,
        String color,
        Integer displayOrder
) {
}
