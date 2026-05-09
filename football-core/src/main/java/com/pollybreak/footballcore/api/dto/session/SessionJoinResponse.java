package com.pollybreak.footballcore.api.dto.session;

public record SessionJoinResponse(
        String status,
        SessionPlayerResponse player,
        SessionWaitlistResponse waitlistEntry
) {
}
