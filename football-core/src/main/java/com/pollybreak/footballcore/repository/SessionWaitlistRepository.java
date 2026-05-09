package com.pollybreak.footballcore.repository;

import com.pollybreak.footballcore.domain.entity.SessionWaitlistEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionWaitlistRepository extends JpaRepository<SessionWaitlistEntry, Long> {

    List<SessionWaitlistEntry> findAllBySessionIdAndActiveTrueOrderByQueuedAtAscIdAsc(Long sessionId);

    Optional<SessionWaitlistEntry> findFirstBySessionIdAndActiveTrueOrderByQueuedAtAscIdAsc(Long sessionId);

    Optional<SessionWaitlistEntry> findBySessionIdAndPlayerId(Long sessionId, Long playerId);
}
