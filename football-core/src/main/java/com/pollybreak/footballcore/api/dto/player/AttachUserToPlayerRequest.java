package com.pollybreak.footballcore.api.dto.player;

import com.pollybreak.footballcore.domain.enums.PlayerPosition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AttachUserToPlayerRequest(
        @NotNull Long telegramId,
        String username,
        @NotBlank String displayName,
        String firstName,
        String lastName,
        String nickname,
        String homeCity,
        LocalDate birthDate,
        PlayerPosition defaultPosition
) {
}
