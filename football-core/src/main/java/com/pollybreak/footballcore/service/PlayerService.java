package com.pollybreak.footballcore.service;

import com.pollybreak.footballcore.api.dto.player.PlayerProfileResponse;
import com.pollybreak.footballcore.api.dto.player.RegisterPlayerRequest;
import com.pollybreak.footballcore.api.dto.player.AttachUserToPlayerRequest;
import com.pollybreak.footballcore.api.dto.player.UpdatePlayerRequest;
import com.pollybreak.footballcore.domain.entity.AppUser;
import com.pollybreak.footballcore.domain.entity.Player;
import com.pollybreak.footballcore.repository.AppUserRepository;
import com.pollybreak.footballcore.repository.PlayerRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final AppUserRepository appUserRepository;

    public List<Player> findAll() {
        return playerRepository.findAll();
    }

    public List<PlayerProfileResponse> findAllProfiles(boolean activeOnly) {
        List<Player> players = activeOnly ? playerRepository.findAllByActiveTrueOrderByFirstNameAscLastNameAsc()
                : playerRepository.findAll();
        return players.stream()
                .map(PlayerProfileResponse::fromEntity)
                .toList();
    }

    public List<Player> findActivePlayers() {
        return playerRepository.findAllByActiveTrueOrderByFirstNameAscLastNameAsc();
    }

    public Player getById(Long id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + id));
    }

    public PlayerProfileResponse getProfile(Long id) {
        return PlayerProfileResponse.fromEntity(getById(id));
    }

    @Transactional
    public PlayerProfileResponse register(RegisterPlayerRequest request) {
        AppUser user = resolveOrCreateUser(request);
        playerRepository.findByUserId(user.getId()).ifPresent(existing -> {
            throw new IllegalArgumentException("Player profile already exists for user: " + user.getId());
        });

        Player player = new Player();
        player.setUser(user);
        player.setFirstName(request.firstName());
        player.setLastName(request.lastName());
        player.setNickname(request.nickname());
        player.setHomeCity(request.homeCity());
        player.setBirthDate(request.birthDate());
        player.setDefaultPosition(request.defaultPosition());
        player.setActive(true);

        return PlayerProfileResponse.fromEntity(playerRepository.save(player));
    }

    @Transactional
    public PlayerProfileResponse update(Long playerId, UpdatePlayerRequest request) {
        Player player = getById(playerId);

        AppUser user = player.getUser();
        if (user != null) {
            if (request.displayName() != null && !request.displayName().isBlank()) {
                user.setDisplayName(request.displayName());
            }
            user.setUsername(request.username());
        }

        if (request.firstName() != null && !request.firstName().isBlank()) {
            player.setFirstName(request.firstName());
        }
        player.setLastName(request.lastName());
        player.setNickname(request.nickname());
        player.setHomeCity(request.homeCity());
        player.setBirthDate(request.birthDate());
        player.setDefaultPosition(request.defaultPosition());
        if (request.active() != null) {
            player.setActive(request.active());
        }

        return PlayerProfileResponse.fromEntity(player);
    }

    @Transactional
    public PlayerProfileResponse attachUser(Long playerId, AttachUserToPlayerRequest request) {
        Player player = getById(playerId);

        AppUser user = appUserRepository.findByTelegramId(request.telegramId())
                .map(existing -> attachExistingUser(player, existing, request))
                .orElseGet(() -> createUserForExistingPlayer(request));

        player.setUser(user);

        if (request.firstName() != null && !request.firstName().isBlank()) {
            player.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            player.setLastName(request.lastName());
        }
        if (request.nickname() != null) {
            player.setNickname(request.nickname());
        }
        if (request.homeCity() != null) {
            player.setHomeCity(request.homeCity());
        }
        if (request.birthDate() != null) {
            player.setBirthDate(request.birthDate());
        }
        if (request.defaultPosition() != null) {
            player.setDefaultPosition(request.defaultPosition());
        }

        return PlayerProfileResponse.fromEntity(player);
    }

    @Transactional
    public Player save(Player player) {
        return playerRepository.save(player);
    }

    private AppUser resolveOrCreateUser(RegisterPlayerRequest request) {
        if (request.telegramId() != null) {
            return appUserRepository.findByTelegramId(request.telegramId())
                    .map(existing -> updateUser(existing, request))
                    .orElseGet(() -> createUser(request));
        }
        return createUser(request);
    }

    private AppUser createUser(RegisterPlayerRequest request) {
        AppUser user = new AppUser();
        user.setTelegramId(request.telegramId());
        user.setUsername(request.username());
        user.setDisplayName(request.displayName());
        return appUserRepository.save(user);
    }

    private AppUser updateUser(AppUser user, RegisterPlayerRequest request) {
        user.setUsername(request.username());
        user.setDisplayName(request.displayName());
        return user;
    }

    private AppUser attachExistingUser(Player player, AppUser user, AttachUserToPlayerRequest request) {
        playerRepository.findByUserId(user.getId()).ifPresent(existingPlayer -> {
            if (!existingPlayer.getId().equals(player.getId())) {
                throw new IllegalArgumentException("This Telegram account is already attached to another player");
            }
        });

        user.setUsername(request.username());
        user.setDisplayName(request.displayName());
        return user;
    }

    private AppUser createUserForExistingPlayer(AttachUserToPlayerRequest request) {
        AppUser user = new AppUser();
        user.setTelegramId(request.telegramId());
        user.setUsername(request.username());
        user.setDisplayName(request.displayName());
        return appUserRepository.save(user);
    }
}
