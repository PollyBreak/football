package com.pollybreak.footballcore.api.dto.overlay;

import com.pollybreak.footballcore.api.dto.session.SessionTeamPlayerResponse;
import java.util.List;

public record OverlayTeamResponse(
        Long id,
        String name,
        String color,
        Integer displayOrder,
        List<SessionTeamPlayerResponse> players
) {
}
