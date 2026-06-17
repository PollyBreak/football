package com.pollybreak.footballcore.api.dto.overlay;

import com.pollybreak.footballcore.api.dto.match.MatchEventResponse;
import java.time.OffsetDateTime;

public record OverlayEventResponse(
        String type,
        Long sessionId,
        Long matchId,
        OverlayStateResponse state,
        MatchEventResponse event,
        MatchEventResponse assistEvent,
        OffsetDateTime emittedAt
) {
}
