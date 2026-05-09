package com.pollybreak.footballcore.api.dto.session;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AddPlayersToSessionTeamRequest(
        @NotEmpty List<@NotNull Long> playerIds
) {
}
