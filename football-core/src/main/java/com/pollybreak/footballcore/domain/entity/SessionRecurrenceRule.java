package com.pollybreak.footballcore.domain.entity;

import com.pollybreak.footballcore.domain.enums.SessionRecurrenceType;
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
@Table(name = "session_recurrence_rule")
public class SessionRecurrenceRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_type", nullable = false, length = 50)
    private SessionRecurrenceType recurrenceType;

    @Column(name = "interval_days")
    private Integer intervalDays;

    @Column(name = "day_of_month")
    private Integer dayOfMonth;

    @Column(name = "auto_start_registration", nullable = false)
    private boolean autoStartRegistration;

    @Column(name = "auto_start_contribution_collection", nullable = false)
    private boolean autoStartContributionCollection;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_session_id")
    private GameSession currentSession;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
