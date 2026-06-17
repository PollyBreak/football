package com.pollybreak.footballcore.repository;

import com.pollybreak.footballcore.domain.entity.MatchEvent;
import com.pollybreak.footballcore.domain.enums.MatchEventType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchEventRepository extends JpaRepository<MatchEvent, Long> {

    List<MatchEvent> findAllByMatchIdOrderByEventTimeAscIdAsc(Long matchId);

    List<MatchEvent> findAllByMatchSessionIdOrderByEventTimeAscIdAsc(Long sessionId);

    List<MatchEvent> findAllByLinkedEventId(Long linkedEventId);

    Optional<MatchEvent> findByIdAndMatchId(Long id, Long matchId);

    long countByPlayerIdAndEventType(Long playerId, MatchEventType eventType);

    @Query("""
            select count(me)
            from MatchEvent me
            where me.player.id = :playerId
              and me.eventType = :eventType
            """)
    long countByPlayerIdAndEventTypeSafe(@Param("playerId") Long playerId, @Param("eventType") MatchEventType eventType);
}
