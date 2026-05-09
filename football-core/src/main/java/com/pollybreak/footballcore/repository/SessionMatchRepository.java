package com.pollybreak.footballcore.repository;

import com.pollybreak.footballcore.domain.entity.SessionMatch;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionMatchRepository extends JpaRepository<SessionMatch, Long> {

    List<SessionMatch> findAllBySessionIdOrderByMatchNumberAsc(Long sessionId);

    Optional<SessionMatch> findTopBySessionIdOrderByMatchNumberDesc(Long sessionId);
}
