package com.pollybreak.footballcore.api.controller;

import com.pollybreak.footballcore.api.dto.stream.StartStreamBroadcastRequest;
import com.pollybreak.footballcore.api.dto.stream.StreamBroadcastResponse;
import com.pollybreak.footballcore.api.dto.stream.StreamTimelineResponse;
import com.pollybreak.footballcore.api.dto.stream.UpdateStreamShiftRequest;
import com.pollybreak.footballcore.service.StreamBroadcastService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions/{sessionId}/streams")
@RequiredArgsConstructor
public class StreamBroadcastController {

    private final StreamBroadcastService streamBroadcastService;

    @GetMapping
    public List<StreamBroadcastResponse> getStreams(@PathVariable Long sessionId) {
        return streamBroadcastService.findBySession(sessionId);
    }

    @GetMapping("/{streamId}")
    public StreamBroadcastResponse getStream(
            @PathVariable Long sessionId,
            @PathVariable Long streamId
    ) {
        return streamBroadcastService.getResponse(sessionId, streamId);
    }

    @PostMapping("/start")
    public StreamBroadcastResponse startStream(
            @PathVariable Long sessionId,
            @RequestBody(required = false) StartStreamBroadcastRequest request
    ) {
        return streamBroadcastService.start(sessionId, request);
    }

    @PostMapping("/{streamId}/finish")
    public StreamBroadcastResponse finishStream(
            @PathVariable Long sessionId,
            @PathVariable Long streamId
    ) {
        return streamBroadcastService.finish(sessionId, streamId);
    }

    @PostMapping("/{streamId}/shifts")
    public StreamBroadcastResponse addShift(
            @PathVariable Long sessionId,
            @PathVariable Long streamId,
            @RequestBody(required = false) UpdateStreamShiftRequest request
    ) {
        return streamBroadcastService.addShift(sessionId, streamId, request);
    }

    @GetMapping("/{streamId}/timeline")
    public StreamTimelineResponse getTimeline(
            @PathVariable Long sessionId,
            @PathVariable Long streamId
    ) {
        return streamBroadcastService.getTimeline(sessionId, streamId);
    }
}
