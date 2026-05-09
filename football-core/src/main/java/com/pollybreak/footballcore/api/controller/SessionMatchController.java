package com.pollybreak.footballcore.api.controller;

import com.pollybreak.footballcore.api.dto.match.CreateSessionMatchRequest;
import com.pollybreak.footballcore.api.dto.match.FinishSessionMatchRequest;
import com.pollybreak.footballcore.api.dto.match.SessionMatchResponse;
import com.pollybreak.footballcore.api.dto.match.StartSessionMatchRequest;
import com.pollybreak.footballcore.api.dto.match.UpdateSessionMatchScoreRequest;
import com.pollybreak.footballcore.service.SessionMatchService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions/{sessionId}/matches")
@RequiredArgsConstructor
public class SessionMatchController {
    private final SessionMatchService sessionMatchService;

    @PostMapping
    public SessionMatchResponse createMatch(
            @PathVariable Long sessionId,
            @Valid @RequestBody CreateSessionMatchRequest request
    ) {
        return sessionMatchService.create(sessionId, request);
    }

    @GetMapping
    public List<SessionMatchResponse> getMatches(@PathVariable Long sessionId) {
        return sessionMatchService.getResponsesBySessionId(sessionId);
    }

    @GetMapping("/{matchId}")
    public SessionMatchResponse getMatchById(
            @PathVariable Long sessionId,
            @PathVariable Long matchId
    ) {
        validateMatchBelongsToSession(sessionId, matchId);
        return sessionMatchService.getResponseById(matchId);
    }

    @PostMapping("/{matchId}/start")
    public SessionMatchResponse startMatch(
            @PathVariable Long sessionId,
            @PathVariable Long matchId,
            @RequestBody(required = false) StartSessionMatchRequest request
    ) {
        validateMatchBelongsToSession(sessionId, matchId);
        StartSessionMatchRequest safeRequest = request == null ? new StartSessionMatchRequest(null) : request;
        return sessionMatchService.start(matchId, safeRequest);
    }

    @PatchMapping("/{matchId}/score")
    public SessionMatchResponse updateScore(
            @PathVariable Long sessionId,
            @PathVariable Long matchId,
            @Valid @RequestBody UpdateSessionMatchScoreRequest request
    ) {
        validateMatchBelongsToSession(sessionId, matchId);
        return sessionMatchService.updateScore(matchId, request);
    }

    @PostMapping("/{matchId}/finish")
    public SessionMatchResponse finishMatch(
            @PathVariable Long sessionId,
            @PathVariable Long matchId,
            @RequestBody(required = false) FinishSessionMatchRequest request
    ) {
        validateMatchBelongsToSession(sessionId, matchId);
        return sessionMatchService.finish(matchId, request);
    }

    private void validateMatchBelongsToSession(Long sessionId, Long matchId) {
        sessionMatchService.getResponsesBySessionId(sessionId).stream()
                .filter(item -> item.id().equals(matchId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Match does not belong to this session"));
    }
}
