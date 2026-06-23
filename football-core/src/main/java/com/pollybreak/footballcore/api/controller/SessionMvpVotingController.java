package com.pollybreak.footballcore.api.controller;

import com.pollybreak.footballcore.api.dto.mvp.SessionMvpVoteRequest;
import com.pollybreak.footballcore.api.dto.mvp.SessionMvpVotingResponse;
import com.pollybreak.footballcore.service.SessionMvpVotingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions/{sessionId}/mvp")
@RequiredArgsConstructor
public class SessionMvpVotingController {

    private final SessionMvpVotingService sessionMvpVotingService;

    @GetMapping
    public SessionMvpVotingResponse getVoting(
            @PathVariable Long sessionId,
            @RequestParam(required = false) Long userId
    ) {
        return sessionMvpVotingService.getVoting(sessionId, userId);
    }

    @PostMapping("/votes")
    public SessionMvpVotingResponse vote(
            @PathVariable Long sessionId,
            @Valid @RequestBody SessionMvpVoteRequest request
    ) {
        return sessionMvpVotingService.vote(sessionId, request.userId(), request.playerId());
    }
}
