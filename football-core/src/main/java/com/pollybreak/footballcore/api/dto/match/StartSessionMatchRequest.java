package com.pollybreak.footballcore.api.dto.match;

import java.time.OffsetDateTime;

public record StartSessionMatchRequest(
        OffsetDateTime startedAt
) {
}
