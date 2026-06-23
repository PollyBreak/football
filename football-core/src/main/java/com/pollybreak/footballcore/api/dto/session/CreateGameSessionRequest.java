package com.pollybreak.footballcore.api.dto.session;

import com.pollybreak.footballcore.domain.enums.SessionFormatType;
import com.pollybreak.footballcore.domain.enums.MvpVotingParticipantScope;
import com.pollybreak.footballcore.domain.enums.SessionRecurrenceType;
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
        String locationAddress,
        String locationUrl,
        Long venueId,
        Boolean saveVenue,
        String venuePhotoUrl,
        String broadcastUrl,
        Long telegramChatId,
        String telegramChatTitle,
        Boolean autoStartRegistration,
        Integer registrationOpenHoursBefore,
        Boolean autoStartContributionCollection,
        Boolean mvpVotingEnabled,
        Integer mvpVotingDurationHours,
        MvpVotingParticipantScope mvpVotingParticipantScope,
        Boolean mvpVotingTelegramEnabled,
        SessionRecurrenceType recurrenceType,
        Integer recurrenceIntervalDays,
        Integer recurrenceDayOfMonth,
        Integer feeAmount,
        String feeRecipient,
        @NotNull SessionFormatType formatType,
        SessionStatus status,
        Integer plannedMatchDurationMinutes,
        Integer sessionDurationMinutes,
        Integer maxPlayers,
        String playerFormat,
        String notes,
        Long createdByUserId,
        @Valid List<CreateSessionTeamRequest> teams
) {
}
