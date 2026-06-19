package com.pollybreak.footballcore.api.dto.session;

import com.pollybreak.footballcore.domain.entity.SessionVenue;
import java.time.OffsetDateTime;

public record SessionVenueResponse(
        Long id,
        String name,
        String address,
        String gisUrl,
        String photoUrl,
        OffsetDateTime createdAt
) {
    public static SessionVenueResponse fromEntity(SessionVenue venue) {
        return new SessionVenueResponse(
                venue.getId(),
                venue.getName(),
                venue.getAddress(),
                venue.getGisUrl(),
                venue.getPhotoUrl(),
                venue.getCreatedAt()
        );
    }
}
