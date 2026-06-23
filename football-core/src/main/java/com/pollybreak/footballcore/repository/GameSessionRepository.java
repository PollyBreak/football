package com.pollybreak.footballcore.repository;

import com.pollybreak.footballcore.domain.entity.GameSession;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GameSessionRepository extends JpaRepository<GameSession, Long> {

    List<GameSession> findAllByOrderBySessionDateDescSessionTimeDescCreatedAtDesc();

    List<GameSession> findAllBySessionDateOrderBySessionTimeDescCreatedAtDesc(LocalDate sessionDate);

    Optional<GameSession> findFirstByRecurrenceRuleIdOrderBySessionDateDescSessionTimeDescCreatedAtDesc(Long recurrenceRuleId);

    @EntityGraph(attributePaths = "createdBy")
    @Query("""
            select session
            from GameSession session
            where session.autoStartRegistration = true
              and session.telegramRegistrationMessageId is null
              and session.telegramChatId is not null
              and session.createdBy is not null
            """)
    List<GameSession> findRegistrationAutoStartCandidates();

    @Query("""
            select session
            from GameSession session
            where session.mvpVotingEnabled = true
              and session.mvpVotingStartedAt is not null
              and session.mvpVotingEndsAt is not null
              and session.mvpVotingEndsAt <= current_timestamp
              and session.telegramMvpResultSentAt is null
            """)
    List<GameSession> findMvpVotingResultAnnouncementCandidates();
}
