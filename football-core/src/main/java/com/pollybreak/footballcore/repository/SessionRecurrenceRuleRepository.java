package com.pollybreak.footballcore.repository;

import com.pollybreak.footballcore.domain.entity.SessionRecurrenceRule;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRecurrenceRuleRepository extends JpaRepository<SessionRecurrenceRule, Long> {

    @EntityGraph(attributePaths = "currentSession")
    List<SessionRecurrenceRule> findAllByActiveTrue();
}
