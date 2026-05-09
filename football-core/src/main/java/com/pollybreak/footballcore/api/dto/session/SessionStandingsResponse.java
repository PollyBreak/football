package com.pollybreak.footballcore.api.dto.session;

import java.util.List;

public record SessionStandingsResponse(
        Long sessionId,
        List<SessionStandingsRowResponse> standings
) {
}
