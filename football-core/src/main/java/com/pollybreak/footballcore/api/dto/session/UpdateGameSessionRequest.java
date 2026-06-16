package com.pollybreak.footballcore.api.dto.session;

import com.pollybreak.footballcore.domain.enums.SessionStatus;
import java.time.LocalDate;
import java.time.LocalTime;

public record UpdateGameSessionRequest(
        String title,
        LocalDate sessionDate,
        LocalTime sessionTime,
        String location,
        String locationUrl,
        String broadcastUrl,
        SessionStatus status,
        Integer plannedMatchDurationMinutes,
        String notes,
        Integer maxPlayers
) {
}
