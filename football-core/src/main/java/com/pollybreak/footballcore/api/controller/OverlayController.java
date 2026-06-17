package com.pollybreak.footballcore.api.controller;

import com.pollybreak.footballcore.api.dto.overlay.OverlayStateResponse;
import com.pollybreak.footballcore.service.OverlayEventService;
import com.pollybreak.footballcore.service.OverlayStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/overlay/sessions/{sessionId}")
@RequiredArgsConstructor
public class OverlayController {

    private final OverlayStateService overlayStateService;
    private final OverlayEventService overlayEventService;

    @GetMapping("/state")
    public OverlayStateResponse getState(
            @PathVariable Long sessionId,
            @RequestParam(required = false) Long matchId
    ) {
        return overlayStateService.getState(sessionId, matchId);
    }

    @GetMapping("/stream")
    public SseEmitter stream(@PathVariable Long sessionId) {
        return overlayEventService.subscribe(sessionId);
    }
}
