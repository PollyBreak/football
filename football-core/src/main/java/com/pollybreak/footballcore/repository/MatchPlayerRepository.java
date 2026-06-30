package com.pollybreak.footballcore.repository;

import com.pollybreak.footballcore.domain.entity.MatchPlayer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchPlayerRepository extends JpaRepository<MatchPlayer, Long> {

    boolean existsByMatchId(Long matchId);

    @EntityGraph(attributePaths = {"match", "team", "player", "player.user"})
    List<MatchPlayer> findAllByMatchIdOrderByStartedAtAscIdAsc(Long matchId);

    @Query("""
            select matchPlayer
            from MatchPlayer matchPlayer
                join fetch matchPlayer.match match
                join fetch matchPlayer.team team
                join fetch matchPlayer.player player
                left join fetch player.user
            where match.id = :matchId
              and matchPlayer.endedAt is null
            order by team.displayOrder asc, matchPlayer.startedAt asc, matchPlayer.id asc
            """)
    List<MatchPlayer> findActiveByMatchIdForOverlay(@Param("matchId") Long matchId);

    Optional<MatchPlayer> findByMatchIdAndTeamIdAndPlayerIdAndEndedAtIsNull(Long matchId, Long teamId, Long playerId);
}
