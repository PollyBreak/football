package com.pollybreak.footballcore.api.dto.player;

import com.pollybreak.footballcore.domain.enums.PlayerPosition;
import java.time.LocalDate;

public record UpdatePlayerRequest(
        String username,
        String displayName,
        String firstName,
        String lastName,
        String nickname,
        String homeCity,
        LocalDate birthDate,
        PlayerPosition defaultPosition,
        Boolean active
) {
}
