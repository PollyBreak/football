package com.pollybreak.footballcore.repository;

import com.pollybreak.footballcore.domain.entity.SessionMvpVote;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionMvpVoteRepository extends JpaRepository<SessionMvpVote, Long> {

    List<SessionMvpVote> findAllBySessionId(Long sessionId);

    Optional<SessionMvpVote> findBySessionIdAndVoterId(Long sessionId, Long voterId);

    void deleteAllBySessionId(Long sessionId);
}
