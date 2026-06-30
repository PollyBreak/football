package com.pollybreak.footballcore.service;

import com.pollybreak.footballcore.api.dto.match.MatchEventResponse;
import com.pollybreak.footballcore.api.dto.overlay.OverlayEventResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
public class OverlayEventService {

    public static final String MATCH_STARTED = "MATCH_STARTED";
    public static final String MATCH_FINISHED = "MATCH_FINISHED";
    public static final String MATCH_PAUSED = "MATCH_PAUSED";
    public static final String MATCH_RESUMED = "MATCH_RESUMED";
    public static final String GOAL_RECORDED = "GOAL_RECORDED";
    public static final String SUBSTITUTION_RECORDED = "SUBSTITUTION_RECORDED";
    public static final String GOAL_CANCELLED = "GOAL_CANCELLED";

    private static final long STREAM_TIMEOUT_MILLIS = 6 * 60 * 60 * 1000L;

    private final OverlayStateService overlayStateService;
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emittersBySession = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long sessionId) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
        emittersBySession.computeIfAbsent(sessionId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(sessionId, emitter));
        emitter.onTimeout(() -> removeEmitter(sessionId, emitter));
        emitter.onError(error -> removeEmitter(sessionId, emitter));

        send(sessionId, emitter, "CONNECTED", new OverlayEventResponse(
                "CONNECTED",
                sessionId,
                null,
                overlayStateService.getState(sessionId, null),
                null,
                null,
                OffsetDateTime.now()
        ));

        return emitter;
    }

    public void pushCurrentState(Long sessionId) {
        publish("STATE_REFRESH", sessionId, null, null, null);
    }

    public void publishAfterCommit(String type, Long sessionId, Long matchId) {
        publishAfterCommit(type, sessionId, matchId, null, null);
    }

    public void publishAfterCommit(
            String type,
            Long sessionId,
            Long matchId,
            MatchEventResponse event,
            MatchEventResponse assistEvent
    ) {
        Runnable publishAction = () -> publish(type, sessionId, matchId, event, assistEvent);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publishAction.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishAction.run();
            }
        });
    }

    private void publish(
            String type,
            Long sessionId,
            Long matchId,
            MatchEventResponse event,
            MatchEventResponse assistEvent
    ) {
        OverlayEventResponse response = new OverlayEventResponse(
                type,
                sessionId,
                matchId,
                overlayStateService.getState(sessionId, matchId),
                event,
                assistEvent,
                OffsetDateTime.now()
        );

        List<SseEmitter> emitters = emittersBySession.get(sessionId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            send(sessionId, emitter, type, response);
        }
    }

    private void send(Long sessionId, SseEmitter emitter, String eventName, OverlayEventResponse response) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(response));
        } catch (IOException | IllegalStateException exception) {
            removeEmitter(sessionId, emitter);
        }
    }

    private void removeEmitter(Long sessionId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersBySession.get(sessionId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersBySession.remove(sessionId, emitters);
        }
    }
}
