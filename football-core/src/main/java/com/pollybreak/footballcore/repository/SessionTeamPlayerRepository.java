package com.pollybreak.footballcore.repository;

import com.pollybreak.footballcore.domain.entity.SessionTeamPlayer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionTeamPlayerRepository extends JpaRepository<SessionTeamPlayer, Long> {

    List<SessionTeamPlayer> findAllBySessionTeamIdAndActiveTrue(Long sessionTeamId);

    List<SessionTeamPlayer> findAllBySessionTeamSessionIdAndPlayerIdAndActiveTrue(Long sessionId, Long playerId);

    Optional<SessionTeamPlayer> findBySessionTeamIdAndPlayerIdAndActiveTrue(Long sessionTeamId, Long playerId);
}
