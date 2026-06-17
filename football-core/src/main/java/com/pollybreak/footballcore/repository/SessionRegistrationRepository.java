package com.pollybreak.footballcore.repository;

import com.pollybreak.footballcore.domain.entity.SessionRegistration;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRegistrationRepository extends JpaRepository<SessionRegistration, Long> {

    @EntityGraph(attributePaths = {"player", "player.user"})
    List<SessionRegistration> findAllBySessionIdOrderByUpdatedAtAscIdAsc(Long sessionId);

    Optional<SessionRegistration> findBySessionIdAndPlayerId(Long sessionId, Long playerId);
}
