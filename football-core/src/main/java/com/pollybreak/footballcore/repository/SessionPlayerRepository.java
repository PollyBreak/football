package com.pollybreak.footballcore.repository;

import com.pollybreak.footballcore.domain.entity.SessionPlayer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionPlayerRepository extends JpaRepository<SessionPlayer, Long> {

    List<SessionPlayer> findAllBySessionIdAndActiveTrueOrderByJoinedAtAscIdAsc(Long sessionId);

    long countBySessionIdAndActiveTrue(Long sessionId);

    Optional<SessionPlayer> findBySessionIdAndPlayerIdAndActiveTrue(Long sessionId, Long playerId);

    Optional<SessionPlayer> findBySessionIdAndPlayerId(Long sessionId, Long playerId);

    @Query("""
            select sp
            from SessionPlayer sp
            join fetch sp.session s
            where sp.player.id = :playerId
            order by s.sessionDate desc, s.sessionTime desc, sp.joinedAt desc
            """)
    List<SessionPlayer> findAllByPlayerIdWithSessionOrderBySessionDateDesc(@Param("playerId") Long playerId);
}
