package com.pollybreak.footballcore.api.controller;

import com.pollybreak.footballcore.api.dto.player.AttachUserToPlayerRequest;
import com.pollybreak.footballcore.api.dto.player.PlayerProfileResponse;
import com.pollybreak.footballcore.api.dto.player.RegisterPlayerRequest;
import com.pollybreak.footballcore.api.dto.player.UpdatePlayerRequest;
import com.pollybreak.footballcore.service.PlayerService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;

    @PostMapping
    public PlayerProfileResponse register(@Valid @RequestBody RegisterPlayerRequest request) {
        return playerService.register(request);
    }

    @PutMapping("/{playerId}")
    public PlayerProfileResponse update(
            @PathVariable Long playerId,
            @RequestBody UpdatePlayerRequest request
    ) {
        return playerService.update(playerId, request);
    }

    @PostMapping("/{playerId}/attach-user")
    public PlayerProfileResponse attachUser(
            @PathVariable Long playerId,
            @Valid @RequestBody AttachUserToPlayerRequest request
    ) {
        return playerService.attachUser(playerId, request);
    }

    @GetMapping("/{playerId}")
    public PlayerProfileResponse getById(@PathVariable Long playerId) {
        return playerService.getProfile(playerId);
    }

    @GetMapping
    public List<PlayerProfileResponse> getAll(@RequestParam(defaultValue = "true") boolean activeOnly) {
        return playerService.findAllProfiles(activeOnly);
    }
}
