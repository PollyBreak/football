package com.pollybreak.footballcore.api.dto.session;

import com.pollybreak.footballcore.domain.enums.SessionFormatType;
import com.pollybreak.footballcore.domain.enums.SessionStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record CreateGameSessionRequest(
        @NotBlank String title,
        @NotNull LocalDate sessionDate,
        @NotNull LocalTime sessionTime,
        String location,
        String locationUrl,
        @NotNull SessionFormatType formatType,
        SessionStatus status,
        Integer plannedMatchDurationMinutes,
        Integer maxPlayers,
        String notes,
        Long createdByUserId,
        @Valid List<CreateSessionTeamRequest> teams
) {
}
