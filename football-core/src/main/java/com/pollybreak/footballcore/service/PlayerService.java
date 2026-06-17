package com.pollybreak.footballcore.service;

import com.pollybreak.footballcore.api.dto.player.PlayerProfileResponse;
import com.pollybreak.footballcore.api.dto.player.PlayerSessionSummaryResponse;
import com.pollybreak.footballcore.api.dto.player.PlayerStatsResponse;
import com.pollybreak.footballcore.api.dto.player.RegisterPlayerRequest;
import com.pollybreak.footballcore.api.dto.player.AttachUserToPlayerRequest;
import com.pollybreak.footballcore.api.dto.player.UpdatePlayerRequest;
import com.pollybreak.footballcore.domain.entity.GameSession;
import com.pollybreak.footballcore.domain.entity.AppUser;
import com.pollybreak.footballcore.domain.entity.Player;
import com.pollybreak.footballcore.domain.enums.MatchEventType;
import com.pollybreak.footballcore.repository.AppUserRepository;
import com.pollybreak.footballcore.repository.MatchEventRepository;
import com.pollybreak.footballcore.repository.PlayerRepository;
import com.pollybreak.footballcore.repository.SessionPlayerRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final AppUserRepository appUserRepository;
    private final MatchEventRepository matchEventRepository;
    private final SessionPlayerRepository sessionPlayerRepository;
    private final TelegramRegistrationService telegramRegistrationService;

    public List<Player> findAll() {
        return playerRepository.findAll();
    }

    public List<PlayerProfileResponse> findAllProfiles(boolean activeOnly) {
        List<Player> players = activeOnly ? playerRepository.findAllByActiveTrueOrderByFirstNameAscLastNameAsc()
                : playerRepository.findAll();
        return players.stream()
                .map(this::buildProfileResponse)
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
        return buildProfileResponse(getById(id));
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
        player.setRating(100);
        player.setActive(true);

        Player savedPlayer = playerRepository.save(player);
        telegramRegistrationService.applyPendingRegistrations(savedPlayer);
        return buildProfileResponse(savedPlayer);
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

        return buildProfileResponse(player);
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

        telegramRegistrationService.applyPendingRegistrations(player);
        return buildProfileResponse(player);
    }

    @Transactional
    public Player save(Player player) {
        return playerRepository.save(player);
    }

    private PlayerProfileResponse buildProfileResponse(Player player) {
        long goals = matchEventRepository.countByPlayerIdAndEventTypeSafe(player.getId(), MatchEventType.GOAL);
        long assists = matchEventRepository.countByPlayerIdAndEventTypeSafe(player.getId(), MatchEventType.ASSIST);

        Map<Long, GameSession> uniqueSessions = new LinkedHashMap<>();
        sessionPlayerRepository.findAllByPlayerIdWithSessionOrderBySessionDateDesc(player.getId())
                .forEach(sessionPlayer -> uniqueSessions.putIfAbsent(sessionPlayer.getSession().getId(), sessionPlayer.getSession()));

        return PlayerProfileResponse.fromEntity(
                player,
                new PlayerStatsResponse(goals, assists),
                uniqueSessions.values().stream()
                        .map(PlayerSessionSummaryResponse::fromEntity)
                        .toList()
        );
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
