package com.pollybreak.footballcore.repository;

import com.pollybreak.footballcore.domain.entity.SessionTeam;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionTeamRepository extends JpaRepository<SessionTeam, Long> {

    List<SessionTeam> findAllBySessionIdOrderByDisplayOrderAsc(Long sessionId);
}
