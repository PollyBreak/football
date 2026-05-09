package com.pollybreak.footballcore.api.dto.match;

public record GoalRecordedResponse(
        SessionMatchResponse match,
        MatchEventResponse goalEvent,
        MatchEventResponse assistEvent
) {
}
