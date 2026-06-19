package com.pollybreak.footballcore.api.dto.stream;

import java.util.List;

public record StreamTimelineResponse(
        Long streamId,
        Long sessionId,
        String youtubeVideoId,
        Integer timelineShiftSeconds,
        String descriptionBlock,
        List<StreamTimelineItemResponse> items
) {
}
