package com.pollybreak.footballcore.api.dto.player;

import com.pollybreak.footballcore.domain.enums.PlayerPosition;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record RegisterPlayerRequest(
        Long telegramId,
        String username,
        @NotBlank String displayName,
        @NotBlank String firstName,
        String lastName,
        String nickname,
        String homeCity,
        LocalDate birthDate,
        PlayerPosition defaultPosition
) {
}
