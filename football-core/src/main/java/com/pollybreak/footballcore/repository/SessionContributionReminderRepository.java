package com.pollybreak.footballcore.repository;

import com.pollybreak.footballcore.domain.entity.SessionContributionReminder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionContributionReminderRepository extends JpaRepository<SessionContributionReminder, Long> {

    @Query("""
            select reminder
            from SessionContributionReminder reminder
            where reminder.session.id = :sessionId
            order by reminder.hoursBefore desc, reminder.id asc
            """)
    List<SessionContributionReminder> findAllForSession(@Param("sessionId") Long sessionId);

    @Query("""
            select reminder
            from SessionContributionReminder reminder
            where reminder.session.id = :sessionId and reminder.hoursBefore = :hoursBefore
            """)
    Optional<SessionContributionReminder> findForSessionAndHoursBefore(
            @Param("sessionId") Long sessionId,
            @Param("hoursBefore") Integer hoursBefore
    );

    @EntityGraph(attributePaths = "session")
    @Query("""
            select reminder
            from SessionContributionReminder reminder
            join reminder.session gameSession
            where reminder.sentAt is null
            order by gameSession.sessionDate asc, gameSession.sessionTime asc, reminder.hoursBefore desc, reminder.id asc
            """)
    List<SessionContributionReminder> findPendingWithSession();
}
