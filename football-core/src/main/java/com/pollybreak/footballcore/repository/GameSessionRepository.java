package com.pollybreak.footballcore.repository;

import com.pollybreak.footballcore.domain.entity.GameSession;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameSessionRepository extends JpaRepository<GameSession, Long> {

    List<GameSession> findAllByOrderBySessionDateDescSessionTimeDescCreatedAtDesc();

    List<GameSession> findAllBySessionDateOrderBySessionTimeDescCreatedAtDesc(LocalDate sessionDate);

    Optional<GameSession> findFirstByRecurrenceRuleIdOrderBySessionDateDescSessionTimeDescCreatedAtDesc(Long recurrenceRuleId);
}
