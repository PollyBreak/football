package com.pollybreak.footballcore.api.controller;

import com.pollybreak.footballcore.api.dto.session.AddPlayerToSessionRequest;
import com.pollybreak.footballcore.api.dto.session.CreateRandomSessionPlayerRequest;
import com.pollybreak.footballcore.api.dto.session.SessionJoinResponse;
import com.pollybreak.footballcore.api.dto.session.SessionPlayerResponse;
import com.pollybreak.footballcore.api.dto.session.SessionWaitlistResponse;
import com.pollybreak.footballcore.service.SessionPlayerService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/api/sessions/{sessionId}/players")
@RequiredArgsConstructor
public class SessionPlayerController {

    private final SessionPlayerService sessionPlayerService;

    @GetMapping
    public List<SessionPlayerResponse> getPlayers(@PathVariable Long sessionId) {
        return sessionPlayerService.getActivePlayers(sessionId);
    }

    @GetMapping("/waitlist")
    public List<SessionWaitlistResponse> getWaitlist(@PathVariable Long sessionId) {
        return sessionPlayerService.getActiveWaitlist(sessionId);
    }

    @PostMapping
    public SessionPlayerResponse addExistingPlayer(
            @PathVariable Long sessionId,
            @Valid @RequestBody AddPlayerToSessionRequest request
    ) {
        return sessionPlayerService.addExistingPlayer(sessionId, request);
    }

    @PostMapping("/join")
    public SessionJoinResponse join(
            @PathVariable Long sessionId,
            @Valid @RequestBody AddPlayerToSessionRequest request
    ) {
        return sessionPlayerService.join(sessionId, request);
    }

    @DeleteMapping("/{playerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removePlayer(
            @PathVariable Long sessionId,
            @PathVariable Long playerId
    ) {
        sessionPlayerService.removePlayer(sessionId, playerId);
    }

    @DeleteMapping("/waitlist/{playerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveWaitlist(
            @PathVariable Long sessionId,
            @PathVariable Long playerId
    ) {
        sessionPlayerService.leaveWaitlist(sessionId, playerId);
    }

    @PostMapping("/random")
    public SessionPlayerResponse createRandomPlayer(
            @PathVariable Long sessionId,
            @RequestBody(required = false) CreateRandomSessionPlayerRequest request
    ) {
        CreateRandomSessionPlayerRequest safeRequest = request == null
                ? new CreateRandomSessionPlayerRequest(null, null)
                : request;
        return sessionPlayerService.createRandomPlayer(sessionId, safeRequest);
    }
}
