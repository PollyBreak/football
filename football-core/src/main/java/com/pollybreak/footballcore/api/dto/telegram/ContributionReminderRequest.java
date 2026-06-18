package com.pollybreak.footballcore.api.dto.telegram;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ContributionReminderRequest(
        @NotNull @Min(1) Integer hoursBefore
) {
}
