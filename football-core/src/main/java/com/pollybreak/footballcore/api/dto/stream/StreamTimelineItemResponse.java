package com.pollybreak.footballcore.api.dto.stream;

import com.pollybreak.footballcore.domain.enums.MatchEventType;

public record StreamTimelineItemResponse(
        Long eventId,
        Long matchId,
        Integer matchNumber,
        Integer roundNumber,
        MatchEventType eventType,
        Integer streamOffsetSeconds,
        Integer adjustedStreamOffsetSeconds,
        String timecode,
        String playerName,
        String relatedPlayerName,
        String teamName,
        String text
) {
}
