package com.pollybreak.footballcore.api.controller;

import com.pollybreak.footballcore.api.dto.session.CreateSessionTeamRequest;
import com.pollybreak.footballcore.api.dto.session.SessionTeamResponse;
import com.pollybreak.footballcore.service.SessionTeamService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions/{sessionId}/teams")
@RequiredArgsConstructor
public class SessionTeamController {
    private final SessionTeamService sessionTeamService;

    @PostMapping
    public SessionTeamResponse createTeam(
            @PathVariable Long sessionId,
            @Valid @RequestBody CreateSessionTeamRequest request
    ) {
        return sessionTeamService.create(sessionId, request);
    }

    @GetMapping
    public List<SessionTeamResponse> getTeams(@PathVariable Long sessionId) {
        return sessionTeamService.getResponsesBySessionId(sessionId);
    }
}
