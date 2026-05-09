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
        Long relatedPlayerId,
        String relatedPlayerName,
        Long linkedEventId,
        Integer minuteInMatch,
        Integer secondInMatch,
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
                event.getRelatedPlayer() != null ? event.getRelatedPlayer().getId() : null,
                fullName(event.getRelatedPlayer() != null ? event.getRelatedPlayer().getFirstName() : null,
                        event.getRelatedPlayer() != null ? event.getRelatedPlayer().getLastName() : null),
                event.getLinkedEvent() != null ? event.getLinkedEvent().getId() : null,
                event.getMinuteInMatch(),
                event.getSecondInMatch(),
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
