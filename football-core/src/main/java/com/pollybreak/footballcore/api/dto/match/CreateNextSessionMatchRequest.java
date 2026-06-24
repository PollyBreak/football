package com.pollybreak.footballcore.api.dto.match;

public record CreateNextSessionMatchRequest(
        String firstPairKey,
        String secondPairKey
) {
}
