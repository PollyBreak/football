package com.pollybreak.footballcore.api.dto.session;

import com.pollybreak.footballcore.domain.enums.MvpVotingParticipantScope;
import com.pollybreak.footballcore.domain.enums.SessionFormatType;
import com.pollybreak.footballcore.domain.enums.SessionStatus;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record UpdateGameSessionRequest(
        String title,
        LocalDate sessionDate,
        LocalTime sessionTime,
        String location,
        String locationAddress,
        String locationUrl,
        String broadcastUrl,
        Long telegramChatId,
        String telegramChatTitle,
        Boolean autoStartRegistration,
        Integer registrationOpenHoursBefore,
        Boolean autoStartContributionCollection,
        Integer feeAmount,
        String feeRecipient,
        Boolean mvpVotingEnabled,
        Integer mvpVotingDurationHours,
        MvpVotingParticipantScope mvpVotingParticipantScope,
        Boolean mvpVotingTelegramEnabled,
        SessionFormatType formatType,
        SessionStatus status,
        Integer plannedMatchDurationMinutes,
        Integer sessionDurationMinutes,
        String notes,
        Integer maxPlayers,
        Integer teamCount,
        String playerFormat,
        Boolean recurrenceActive,
        @Valid List<CreateSessionTeamRequest> teams
) {
}
