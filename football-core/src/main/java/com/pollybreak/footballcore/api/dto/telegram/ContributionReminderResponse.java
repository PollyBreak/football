package com.pollybreak.footballcore.api.dto.telegram;

import java.time.OffsetDateTime;

public record ContributionReminderResponse(
        Long id,
        Long sessionId,
        Integer hoursBefore,
        OffsetDateTime sentAt
) {
}
