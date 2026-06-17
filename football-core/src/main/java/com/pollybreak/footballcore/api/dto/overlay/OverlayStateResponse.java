package com.pollybreak.footballcore.api.dto.overlay;

import com.pollybreak.footballcore.api.dto.match.MatchEventResponse;
import com.pollybreak.footballcore.api.dto.match.SessionMatchResponse;
import com.pollybreak.footballcore.api.dto.session.SessionStandingsRowResponse;
import java.time.OffsetDateTime;
import java.util.List;

public record OverlayStateResponse(
        Long sessionId,
        OffsetDateTime serverTime,
        SessionMatchResponse currentMatch,
        List<SessionMatchResponse> inProgressMatches,
        List<SessionMatchResponse> matches,
        List<OverlayTeamResponse> teams,
        List<MatchEventResponse> currentMatchEvents,
        List<MatchEventResponse> sessionEvents,
        List<SessionStandingsRowResponse> standings,
        SessionMatchResponse lastFinishedMatch,
        SessionMatchResponse nextMatch,
        List<MatchEventResponse> lastFinishedMatchEvents
) {
}
