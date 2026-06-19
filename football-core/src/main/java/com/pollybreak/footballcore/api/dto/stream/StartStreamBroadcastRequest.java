package com.pollybreak.footballcore.api.dto.stream;

import java.time.OffsetDateTime;

public record StartStreamBroadcastRequest(
        String title,
        String youtubeVideoId,
        String youtubeBroadcastId,
        OffsetDateTime streamStartedAt
) {
}
