package com.pollybreak.footballcore.api.dto.player;

import com.pollybreak.footballcore.domain.entity.GameSession;
import com.pollybreak.footballcore.domain.enums.SessionStatus;
import java.time.LocalDate;
import java.time.LocalTime;

public record PlayerSessionSummaryResponse(
        Long sessionId,
        String title,
        LocalDate sessionDate,
        LocalTime sessionTime,
        SessionStatus status
) {
    public static PlayerSessionSummaryResponse fromEntity(GameSession session) {
        return new PlayerSessionSummaryResponse(
                session.getId(),
                session.getTitle(),
                session.getSessionDate(),
                session.getSessionTime(),
                session.getStatus()
        );
    }
}
