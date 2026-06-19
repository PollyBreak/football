package com.pollybreak.footballcore.api.dto.session;

import jakarta.validation.constraints.NotBlank;

public record CreateSessionVenueRequest(
        @NotBlank String name,
        String address,
        String gisUrl,
        String photoUrl
) {
}
