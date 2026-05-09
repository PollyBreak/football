package com.pollybreak.footballcore.api.dto.session;

public record SessionStandingsRowResponse(
        Long teamId,
        String teamName,
        String teamColor,
        int played,
        int wins,
        int draws,
        int losses,
        int goalsFor,
        int goalsAgainst,
        int goalDifference,
        int points
) {
}
