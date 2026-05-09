package com.pollybreak.footballcore.api.dto.session;

import com.pollybreak.footballcore.domain.enums.PlayerPosition;

public record CreateRandomSessionPlayerRequest(
        String nicknamePrefix,
        PlayerPosition position
) {
}
