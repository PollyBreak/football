package com.pollybreak.footballcore.api.dto.auth;

import com.pollybreak.footballcore.api.dto.player.PlayerProfileResponse;

public record TelegramAuthResponse(
        boolean authenticated,
        boolean onboardingRequired,
        AppUserResponse user,
        PlayerProfileResponse player
) {
}
