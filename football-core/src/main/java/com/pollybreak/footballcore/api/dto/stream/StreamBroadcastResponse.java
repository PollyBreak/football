package com.pollybreak.footballcore.api.dto.stream;

import com.pollybreak.footballcore.domain.entity.StreamBroadcast;
import java.time.OffsetDateTime;

public record StreamBroadcastResponse(
        Long id,
        Long sessionId,
        String title,
        String youtubeVideoId,
        String youtubeBroadcastId,
        OffsetDateTime streamStartedAt,
        OffsetDateTime streamEndedAt,
        Integer timelineShiftSeconds,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static StreamBroadcastResponse fromEntity(StreamBroadcast broadcast) {
        return new StreamBroadcastResponse(
                broadcast.getId(),
                broadcast.getSession().getId(),
                broadcast.getTitle(),
                broadcast.getYoutubeVideoId(),
                broadcast.getYoutubeBroadcastId(),
                broadcast.getStreamStartedAt(),
                broadcast.getStreamEndedAt(),
                broadcast.getTimelineShiftSeconds(),
                broadcast.getCreatedAt(),
                broadcast.getUpdatedAt()
        );
    }
}
