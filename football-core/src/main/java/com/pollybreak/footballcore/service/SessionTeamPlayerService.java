package com.pollybreak.footballcore.service;

import com.pollybreak.footballcore.domain.entity.SessionTeamPlayer;
import com.pollybreak.footballcore.repository.SessionTeamPlayerRepository;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionTeamPlayerService {

    private final SessionTeamPlayerRepository sessionTeamPlayerRepository;

    public List<SessionTeamPlayer> findActiveBySessionTeamId(Long sessionTeamId) {
        return sessionTeamPlayerRepository.findAllBySessionTeamIdAndActiveTrue(sessionTeamId);
    }

    public SessionTeamPlayer getById(Long id) {
        return sessionTeamPlayerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Session team player not found: " + id));
    }

    @Transactional
    public SessionTeamPlayer save(SessionTeamPlayer sessionTeamPlayer) {
        return sessionTeamPlayerRepository.save(sessionTeamPlayer);
    }

    @Transactional
    public void deactivate(Long sessionTeamId, Long playerId) {
        SessionTeamPlayer membership = sessionTeamPlayerRepository
                .findBySessionTeamIdAndPlayerIdAndActiveTrue(sessionTeamId, playerId)
                .orElseThrow(() -> new IllegalArgumentException("Active team membership not found"));
        membership.setActive(false);
        membership.setLeftAt(OffsetDateTime.now());
    }

    @Transactional
    public void deactivateAllForSession(Long sessionId, Long playerId) {
        sessionTeamPlayerRepository.findAllBySessionTeamSessionIdAndPlayerIdAndActiveTrue(sessionId, playerId)
                .forEach(membership -> {
                    membership.setActive(false);
                    membership.setLeftAt(OffsetDateTime.now());
                });
    }
}
