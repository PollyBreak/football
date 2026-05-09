package com.pollybreak.footballcore.api.dto.auth;

import com.pollybreak.footballcore.domain.entity.AppUser;
import java.time.OffsetDateTime;

public record AppUserResponse(
        Long id,
        Long telegramId,
        String username,
        String displayName,
        String photoUrl,
        OffsetDateTime createdAt
) {
    public static AppUserResponse fromEntity(AppUser user) {
        return new AppUserResponse(
                user.getId(),
                user.getTelegramId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getPhotoUrl(),
                user.getCreatedAt()
        );
    }
}
