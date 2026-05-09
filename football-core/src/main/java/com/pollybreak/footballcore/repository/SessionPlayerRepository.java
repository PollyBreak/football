package com.pollybreak.footballcore.repository;

import com.pollybreak.footballcore.domain.entity.SessionPlayer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionPlayerRepository extends JpaRepository<SessionPlayer, Long> {

    List<SessionPlayer> findAllBySessionIdAndActiveTrueOrderByJoinedAtAscIdAsc(Long sessionId);

    long countBySessionIdAndActiveTrue(Long sessionId);

    Optional<SessionPlayer> findBySessionIdAndPlayerIdAndActiveTrue(Long sessionId, Long playerId);

    Optional<SessionPlayer> findBySessionIdAndPlayerId(Long sessionId, Long playerId);
}
