package com.pollybreak.footballcore.api.controller;

import com.pollybreak.footballcore.api.dto.match.MatchPlayerResponse;
import com.pollybreak.footballcore.service.MatchPlayerService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/session-matches/{matchId}/players")
@RequiredArgsConstructor
public class MatchPlayerController {

    private final MatchPlayerService matchPlayerService;

    @GetMapping
    public List<MatchPlayerResponse> getMatchPlayers(@PathVariable Long matchId) {
        return matchPlayerService.findByMatchId(matchId).stream()
                .map(MatchPlayerResponse::fromEntity)
                .toList();
    }
}
