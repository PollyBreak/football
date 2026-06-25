package com.pollybreak.footballcore.api.dto.mvp;

public record SessionMvpCandidateResponse(
        Long playerId,
        String firstName,
        String lastName,
        String displayName,
        String photoUrl,
        String telegramPhotoUrl,
        String position,
        Long teamId,
        String teamName,
        String teamColor,
        int goals,
        int assists,
        int votes
) {
}
