package com.pollybreak.footballcore.api.dto.session;

import com.pollybreak.footballcore.domain.entity.GameSession;
import com.pollybreak.footballcore.domain.entity.SessionTeam;
import com.pollybreak.footballcore.domain.enums.SessionFormatType;
import com.pollybreak.footballcore.domain.enums.SessionStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

public record GameSessionResponse(
        Long id,
        String title,
        LocalDate sessionDate,
        LocalTime sessionTime,
        String location,
        String locationAddress,
        String locationUrl,
        String broadcastUrl,
        Long telegramChatId,
        String telegramChatTitle,
        Long telegramRegistrationMessageId,
        Long telegramContributionMessageId,
        Integer feeAmount,
        String feeRecipient,
        SessionFormatType formatType,
        SessionStatus status,
        Integer plannedMatchDurationMinutes,
        Integer sessionDurationMinutes,
        Integer maxPlayers,
        String playerFormat,
        String notes,
        Long createdByUserId,
        OffsetDateTime createdAt,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        List<SessionTeamResponse> teams
) {
    public static GameSessionResponse fromEntity(GameSession session, List<SessionTeam> teams) {
        return new GameSessionResponse(
                session.getId(),
                session.getTitle(),
                session.getSessionDate(),
                session.getSessionTime(),
                session.getLocation(),
                session.getLocationAddress(),
                session.getLocationUrl(),
                session.getBroadcastUrl(),
                session.getTelegramChatId(),
                session.getTelegramChatTitle(),
                session.getTelegramRegistrationMessageId(),
                session.getTelegramContributionMessageId(),
                session.getFeeAmount(),
                session.getFeeRecipient(),
                session.getFormatType(),
                session.getStatus(),
                session.getPlannedMatchDurationMinutes(),
                session.getSessionDurationMinutes(),
                session.getMaxPlayers(),
                session.getPlayerFormat(),
                session.getNotes(),
                session.getCreatedBy() != null ? session.getCreatedBy().getId() : null,
                session.getCreatedAt(),
                session.getStartedAt(),
                session.getEndedAt(),
                teams.stream().map(SessionTeamResponse::fromEntity).toList()
        );
    }
}
