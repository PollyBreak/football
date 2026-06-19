package com.pollybreak.footballcore.api.dto.match;

import com.pollybreak.footballcore.domain.entity.MatchEvent;
import com.pollybreak.footballcore.domain.enums.MatchEventType;
import java.time.OffsetDateTime;
import java.util.Map;

public record MatchEventResponse(
        Long id,
        Long matchId,
        MatchEventType eventType,
        Long teamId,
        String teamName,
        Long playerId,
        String playerName,
        String playerPhotoUrl,
        Long relatedPlayerId,
        String relatedPlayerName,
        String relatedPlayerPhotoUrl,
        Long linkedEventId,
        Long streamBroadcastId,
        Integer minuteInMatch,
        Integer secondInMatch,
        Integer streamOffsetSeconds,
        OffsetDateTime streamEventTime,
        OffsetDateTime eventTime,
        Long createdByUserId,
        Map<String, Object> payload,
        OffsetDateTime createdAt
) {
    public static MatchEventResponse fromEntity(MatchEvent event) {
        return new MatchEventResponse(
                event.getId(),
                event.getMatch().getId(),
                event.getEventType(),
                event.getTeam() != null ? event.getTeam().getId() : null,
                event.getTeam() != null ? event.getTeam().getName() : null,
                event.getPlayer() != null ? event.getPlayer().getId() : null,
                fullName(event.getPlayer() != null ? event.getPlayer().getFirstName() : null,
                        event.getPlayer() != null ? event.getPlayer().getLastName() : null),
                event.getPlayer() != null && event.getPlayer().getUser() != null ? event.getPlayer().getUser().getPhotoUrl() : null,
                event.getRelatedPlayer() != null ? event.getRelatedPlayer().getId() : null,
                fullName(event.getRelatedPlayer() != null ? event.getRelatedPlayer().getFirstName() : null,
                        event.getRelatedPlayer() != null ? event.getRelatedPlayer().getLastName() : null),
                event.getRelatedPlayer() != null && event.getRelatedPlayer().getUser() != null ? event.getRelatedPlayer().getUser().getPhotoUrl() : null,
                event.getLinkedEvent() != null ? event.getLinkedEvent().getId() : null,
                event.getStreamBroadcast() != null ? event.getStreamBroadcast().getId() : null,
                event.getMinuteInMatch(),
                event.getSecondInMatch(),
                event.getStreamOffsetSeconds(),
                event.getStreamEventTime(),
                event.getEventTime(),
                event.getCreatedBy() != null ? event.getCreatedBy().getId() : null,
                event.getPayload(),
                event.getCreatedAt()
        );
    }

    private static String fullName(String firstName, String lastName) {
        if (firstName == null) {
            return null;
        }
        return lastName == null || lastName.isBlank() ? firstName : firstName + " " + lastName;
    }
}
