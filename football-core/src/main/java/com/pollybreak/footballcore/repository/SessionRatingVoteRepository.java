package com.pollybreak.footballcore.repository;

import com.pollybreak.footballcore.domain.entity.SessionRatingVote;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRatingVoteRepository extends JpaRepository<SessionRatingVote, Long> {

    List<SessionRatingVote> findAllBySessionId(Long sessionId);

    Optional<SessionRatingVote> findBySessionIdAndTelegramUserId(Long sessionId, Long telegramUserId);
}
