package com.pollybreak.footballcore.service;

import com.pollybreak.footballcore.api.dto.session.AddPlayerToSessionRequest;
import com.pollybreak.footballcore.api.dto.session.CreateRandomSessionPlayerRequest;
import com.pollybreak.footballcore.api.dto.session.SessionJoinResponse;
import com.pollybreak.footballcore.api.dto.session.SessionPlayerResponse;
import com.pollybreak.footballcore.api.dto.session.SessionWaitlistResponse;
import com.pollybreak.footballcore.domain.entity.GameSession;
import com.pollybreak.footballcore.domain.entity.Player;
import com.pollybreak.footballcore.domain.entity.SessionPlayer;
import com.pollybreak.footballcore.domain.entity.SessionWaitlistEntry;
import com.pollybreak.footballcore.domain.enums.PlayerPosition;
import com.pollybreak.footballcore.repository.GameSessionRepository;
import com.pollybreak.footballcore.repository.PlayerRepository;
import com.pollybreak.footballcore.repository.SessionPlayerRepository;
import com.pollybreak.footballcore.repository.SessionWaitlistRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionPlayerService {

    private static final String[] RANDOM_FIRST_NAMES = {
            "Alex", "Maks", "Timur", "Arman", "Egor", "Nikita", "Denis", "Roma", "Ilya", "Artur"
    };
    private static final String[] RANDOM_NICK_PARTS = {
            "Guest", "Rocket", "Shadow", "Falcon", "Flash", "Storm", "Turbo", "Fox", "Blade", "Comet"
    };

    private final SessionPlayerRepository sessionPlayerRepository;
    private final GameSessionRepository gameSessionRepository;
    private final PlayerRepository playerRepository;
    private final SessionTeamPlayerService sessionTeamPlayerService;
    private final SessionWaitlistRepository sessionWaitlistRepository;

    public List<SessionPlayerResponse> getActivePlayers(Long sessionId) {
        return sessionPlayerRepository.findAllBySessionIdAndActiveTrueOrderByJoinedAtAscIdAsc(sessionId).stream()
                .map(SessionPlayerResponse::fromEntity)
                .toList();
    }

    public List<SessionWaitlistResponse> getActiveWaitlist(Long sessionId) {
        return sessionWaitlistRepository.findAllBySessionIdAndActiveTrueOrderByQueuedAtAscIdAsc(sessionId).stream()
                .map(SessionWaitlistResponse::fromEntity)
                .toList();
    }

    public SessionPlayer getActiveSessionPlayer(Long sessionId, Long playerId) {
        return sessionPlayerRepository.findBySessionIdAndPlayerIdAndActiveTrue(sessionId, playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player is not registered for this session"));
    }

    @Transactional
    public SessionPlayerResponse addExistingPlayer(Long sessionId, AddPlayerToSessionRequest request) {
        GameSession session = getSession(sessionId);
        Player player = playerRepository.findById(request.playerId())
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + request.playerId()));

        SessionPlayer sessionPlayer = sessionPlayerRepository.findBySessionIdAndPlayerId(sessionId, request.playerId())
                .map(existing -> reactivateExistingPlayer(existing, request, player))
                .orElseGet(() -> createSessionPlayer(session, player, request));

        return SessionPlayerResponse.fromEntity(sessionPlayerRepository.save(sessionPlayer));
    }

    @Transactional
    public SessionJoinResponse join(Long sessionId, AddPlayerToSessionRequest request) {
        GameSession session = getSession(sessionId);
        Player player = playerRepository.findById(request.playerId())
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + request.playerId()));

        sessionPlayerRepository.findBySessionIdAndPlayerIdAndActiveTrue(sessionId, request.playerId())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Player is already registered for this session");
                });

        if (hasAvailableSlot(session)) {
            deactivateWaitlistEntryIfExists(sessionId, request.playerId());
            SessionPlayer sessionPlayer = sessionPlayerRepository.findBySessionIdAndPlayerId(sessionId, request.playerId())
                    .map(existing -> reactivateExistingPlayer(existing, request, player))
                    .orElseGet(() -> createSessionPlayer(session, player, request));
            return new SessionJoinResponse(
                    "ACTIVE",
                    SessionPlayerResponse.fromEntity(sessionPlayerRepository.save(sessionPlayer)),
                    null
            );
        }

        SessionWaitlistEntry waitlistEntry = sessionWaitlistRepository.findBySessionIdAndPlayerId(sessionId, request.playerId())
                .map(existing -> reactivateWaitlistEntry(existing, request, player))
                .orElseGet(() -> createWaitlistEntry(session, player, request));

        return new SessionJoinResponse(
                "QUEUED",
                null,
                SessionWaitlistResponse.fromEntity(sessionWaitlistRepository.save(waitlistEntry))
        );
    }

    private SessionPlayer createSessionPlayer(GameSession session, Player player, AddPlayerToSessionRequest request) {
        SessionPlayer sessionPlayer = new SessionPlayer();
        sessionPlayer.setSession(session);
        sessionPlayer.setPlayer(player);
        sessionPlayer.setPosition(request.position() != null ? request.position() : player.getDefaultPosition());
        sessionPlayer.setActive(true);
        return sessionPlayer;
    }

    private SessionPlayer reactivateExistingPlayer(
            SessionPlayer sessionPlayer,
            AddPlayerToSessionRequest request,
            Player player
    ) {
        if (sessionPlayer.isActive()) {
            throw new IllegalArgumentException("Player is already registered for this session");
        }

        sessionPlayer.setPosition(request.position() != null ? request.position() : player.getDefaultPosition());
        sessionPlayer.setActive(true);
        sessionPlayer.setLeftAt(null);
        return sessionPlayer;
    }

    @Transactional
    public SessionPlayerResponse createRandomPlayer(Long sessionId, CreateRandomSessionPlayerRequest request) {
        GameSession session = getSession(sessionId);

        Player player = new Player();
        player.setFirstName(randomFirstName());
        player.setLastName(null);
        player.setNickname(buildNickname(request.nicknamePrefix()));
        player.setDefaultPosition(request.position() != null ? request.position() : randomPosition());
        player.setActive(true);
        Player savedPlayer = playerRepository.save(player);

        SessionPlayer sessionPlayer = new SessionPlayer();
        sessionPlayer.setSession(session);
        sessionPlayer.setPlayer(savedPlayer);
        sessionPlayer.setPosition(savedPlayer.getDefaultPosition());
        sessionPlayer.setActive(true);

        return SessionPlayerResponse.fromEntity(sessionPlayerRepository.save(sessionPlayer));
    }

    @Transactional
    public void removePlayer(Long sessionId, Long playerId) {
        SessionPlayer sessionPlayer = sessionPlayerRepository.findBySessionIdAndPlayerIdAndActiveTrue(sessionId, playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player is not registered for this session"));

        sessionPlayer.setActive(false);
        sessionPlayer.setLeftAt(OffsetDateTime.now());
        sessionTeamPlayerService.deactivateAllForSession(sessionId, playerId);
        promoteFirstQueuedPlayer(sessionPlayer.getSession());
    }

    @Transactional
    public void leaveWaitlist(Long sessionId, Long playerId) {
        SessionWaitlistEntry waitlistEntry = sessionWaitlistRepository.findBySessionIdAndPlayerId(sessionId, playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player is not in waitlist for this session"));
        if (!waitlistEntry.isActive()) {
            throw new IllegalArgumentException("Player is not in waitlist for this session");
        }
        waitlistEntry.setActive(false);
        waitlistEntry.setLeftAt(OffsetDateTime.now());
    }

    @Transactional
    public void fillAvailableSlots(Long sessionId) {
        GameSession session = getSession(sessionId);
        while (hasAvailableSlot(session) && sessionWaitlistRepository
                .findFirstBySessionIdAndActiveTrueOrderByQueuedAtAscIdAsc(sessionId)
                .isPresent()) {
            promoteFirstQueuedPlayer(session);
        }
    }

    private boolean hasAvailableSlot(GameSession session) {
        Integer maxPlayers = session.getMaxPlayers();
        return maxPlayers == null || sessionPlayerRepository.countBySessionIdAndActiveTrue(session.getId()) < maxPlayers;
    }

    private SessionWaitlistEntry createWaitlistEntry(
            GameSession session,
            Player player,
            AddPlayerToSessionRequest request
    ) {
        SessionWaitlistEntry entry = new SessionWaitlistEntry();
        entry.setSession(session);
        entry.setPlayer(player);
        entry.setPosition(request.position() != null ? request.position() : player.getDefaultPosition());
        entry.setActive(true);
        return entry;
    }

    private SessionWaitlistEntry reactivateWaitlistEntry(
            SessionWaitlistEntry entry,
            AddPlayerToSessionRequest request,
            Player player
    ) {
        if (entry.isActive()) {
            throw new IllegalArgumentException("Player is already in waitlist for this session");
        }
        entry.setPosition(request.position() != null ? request.position() : player.getDefaultPosition());
        entry.setActive(true);
        entry.setLeftAt(null);
        return entry;
    }

    private void deactivateWaitlistEntryIfExists(Long sessionId, Long playerId) {
        sessionWaitlistRepository.findBySessionIdAndPlayerId(sessionId, playerId)
                .filter(SessionWaitlistEntry::isActive)
                .ifPresent(entry -> {
                    entry.setActive(false);
                    entry.setLeftAt(OffsetDateTime.now());
                });
    }

    private void promoteFirstQueuedPlayer(GameSession session) {
        if (!hasAvailableSlot(session)) {
            return;
        }

        sessionWaitlistRepository.findFirstBySessionIdAndActiveTrueOrderByQueuedAtAscIdAsc(session.getId())
                .ifPresent(entry -> {
                    AddPlayerToSessionRequest request = new AddPlayerToSessionRequest(
                            entry.getPlayer().getId(),
                            entry.getPosition()
                    );
                    SessionPlayer sessionPlayer = sessionPlayerRepository.findBySessionIdAndPlayerId(session.getId(), entry.getPlayer().getId())
                            .map(existing -> reactivateExistingPlayer(existing, request, entry.getPlayer()))
                            .orElseGet(() -> createSessionPlayer(session, entry.getPlayer(), request));
                    sessionPlayerRepository.save(sessionPlayer);
                    entry.setActive(false);
                    entry.setLeftAt(OffsetDateTime.now());
                });
    }

    private GameSession getSession(Long sessionId) {
        return gameSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Game session not found: " + sessionId));
    }

    private String randomFirstName() {
        return RANDOM_FIRST_NAMES[ThreadLocalRandom.current().nextInt(RANDOM_FIRST_NAMES.length)];
    }

    private String buildNickname(String nicknamePrefix) {
        String prefix = nicknamePrefix == null || nicknamePrefix.isBlank()
                ? RANDOM_NICK_PARTS[ThreadLocalRandom.current().nextInt(RANDOM_NICK_PARTS.length)]
                : nicknamePrefix.trim();
        int suffix = ThreadLocalRandom.current().nextInt(10, 100);
        return prefix + suffix;
    }

    private PlayerPosition randomPosition() {
        PlayerPosition[] positions = PlayerPosition.values();
        return positions[ThreadLocalRandom.current().nextInt(positions.length)];
    }
}
