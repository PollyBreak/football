package com.pollybreak.footballcore.api.dto.session;

import com.pollybreak.footballcore.domain.enums.MvpVotingParticipantScope;
import com.pollybreak.footballcore.domain.enums.SessionStatus;
import java.time.LocalDate;
import java.time.LocalTime;

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
        Integer feeAmount,
        String feeRecipient,
        Boolean mvpVotingEnabled,
        Integer mvpVotingDurationHours,
        MvpVotingParticipantScope mvpVotingParticipantScope,
        Boolean mvpVotingTelegramEnabled,
        SessionStatus status,
        Integer plannedMatchDurationMinutes,
        Integer sessionDurationMinutes,
        String notes,
        Integer maxPlayers,
        String playerFormat,
        Boolean recurrenceActive
) {
}
