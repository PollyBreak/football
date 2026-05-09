package com.pollybreak.footballcore.repository;

import com.pollybreak.footballcore.domain.entity.MatchEvent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchEventRepository extends JpaRepository<MatchEvent, Long> {

    List<MatchEvent> findAllByMatchIdOrderByEventTimeAscIdAsc(Long matchId);

    List<MatchEvent> findAllByLinkedEventId(Long linkedEventId);

    Optional<MatchEvent> findByIdAndMatchId(Long id, Long matchId);
}
