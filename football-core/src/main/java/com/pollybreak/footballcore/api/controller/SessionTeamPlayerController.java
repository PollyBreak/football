package com.pollybreak.footballcore.api.controller;

import com.pollybreak.footballcore.api.dto.session.AddPlayerToSessionTeamRequest;
import com.pollybreak.footballcore.api.dto.session.AddPlayersToSessionTeamRequest;
import com.pollybreak.footballcore.api.dto.session.SessionTeamPlayerResponse;
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
@RequestMapping("/api/session-teams/{teamId}/players")
@RequiredArgsConstructor
public class SessionTeamPlayerController {
    private final SessionTeamService sessionTeamService;

    @PostMapping
    public SessionTeamPlayerResponse addPlayerToTeam(
            @PathVariable Long teamId,
            @Valid @RequestBody AddPlayerToSessionTeamRequest request
    ) {
        return sessionTeamService.addPlayer(teamId, request);
    }

    @PostMapping("/bulk")
    public List<SessionTeamPlayerResponse> addPlayersToTeam(
            @PathVariable Long teamId,
            @Valid @RequestBody AddPlayersToSessionTeamRequest request
    ) {
        return sessionTeamService.addPlayers(teamId, request);
    }

    @GetMapping
    public List<SessionTeamPlayerResponse> getActivePlayers(@PathVariable Long teamId) {
        return sessionTeamService.getActivePlayers(teamId);
    }
}
