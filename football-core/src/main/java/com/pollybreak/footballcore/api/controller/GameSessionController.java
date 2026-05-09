package com.pollybreak.footballcore.api.controller;

import com.pollybreak.footballcore.api.dto.session.CreateGameSessionRequest;
import com.pollybreak.footballcore.api.dto.session.GameSessionResponse;
import com.pollybreak.footballcore.api.dto.session.SessionStandingsResponse;
import com.pollybreak.footballcore.api.dto.session.UpdateGameSessionRequest;

import com.pollybreak.footballcore.service.GameSessionService;
import com.pollybreak.footballcore.service.SessionMatchService;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class GameSessionController {

    private final GameSessionService gameSessionService;
    private final SessionMatchService sessionMatchService;

    @PostMapping
    public GameSessionResponse createSession(@Valid @RequestBody CreateGameSessionRequest request) {
        return gameSessionService.create(request);
    }

    @GetMapping("/{sessionId}")
    public GameSessionResponse getSession(@PathVariable Long sessionId) {
        return gameSessionService.getResponseById(sessionId);
    }

    @PatchMapping("/{sessionId}")
    public GameSessionResponse updateSession(
            @PathVariable Long sessionId,
            @RequestBody UpdateGameSessionRequest request
    ) {
        return gameSessionService.update(sessionId, request);
    }

    @GetMapping
    public List<GameSessionResponse> getSessions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sessionDate
    ) {
        return (sessionDate == null ? gameSessionService.findAll() : gameSessionService.findBySessionDate(sessionDate)).stream()
                .map(session -> gameSessionService.getResponseById(session.getId()))
                .toList();
    }

    @GetMapping("/{sessionId}/standings")
    public SessionStandingsResponse getStandings(@PathVariable Long sessionId) {
        return sessionMatchService.getStandings(sessionId);
    }

}
