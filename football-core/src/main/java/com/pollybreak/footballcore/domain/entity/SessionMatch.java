package com.pollybreak.footballcore.domain.entity;

import com.pollybreak.footballcore.domain.enums.MatchStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "session_match")
public class SessionMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private GameSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_a_id", nullable = false)
    private SessionTeam teamA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_b_id", nullable = false)
    private SessionTeam teamB;

    @Column(name = "match_number", nullable = false)
    private Integer matchNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private MatchStatus status = MatchStatus.PLANNED;

    @Column(name = "planned_duration_minutes")
    private Integer plannedDurationMinutes;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    @Column(name = "team_a_score", nullable = false)
    private Integer teamAScore = 0;

    @Column(name = "team_b_score", nullable = false)
    private Integer teamBScore = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winning_team_id")
    private SessionTeam winningTeam;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
