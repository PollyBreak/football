package com.pollybreak.footballcore.repository;

import com.pollybreak.footballcore.domain.entity.SessionContribution;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionContributionRepository extends JpaRepository<SessionContribution, Long> {

    @EntityGraph(attributePaths = {"player", "player.user"})
    List<SessionContribution> findAllBySessionIdAndPaidTrueOrderByUpdatedAtAscIdAsc(Long sessionId);

    Optional<SessionContribution> findBySessionIdAndPlayerId(Long sessionId, Long playerId);
}
