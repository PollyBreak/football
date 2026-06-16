package com.pollybreak.footballcore.api.dto.session;

import java.time.LocalDate;
import java.time.LocalTime;

public record UpdateGameSessionRequest(
        String title,
        LocalDate sessionDate,
        LocalTime sessionTime,
        String location,
        String locationUrl,
        Integer plannedMatchDurationMinutes,
        String notes,
        Integer maxPlayers
) {
}
