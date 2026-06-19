package com.pollybreak.footballcore.repository;

import com.pollybreak.footballcore.domain.entity.SessionMatch;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionMatchRepository extends JpaRepository<SessionMatch, Long> {

    List<SessionMatch> findAllBySessionIdOrderByMatchNumberAsc(Long sessionId);

    Optional<SessionMatch> findTopBySessionIdOrderByMatchNumberDesc(Long sessionId);

    @Modifying
    @Query("""
            delete from SessionMatch match
            where match.session.id = :sessionId
            """)
    void deleteAllBySessionId(@Param("sessionId") Long sessionId);
}
