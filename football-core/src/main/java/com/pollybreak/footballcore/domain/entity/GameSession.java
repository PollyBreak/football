package com.pollybreak.footballcore.domain.entity;

import com.pollybreak.footballcore.domain.enums.SessionFormatType;
import com.pollybreak.footballcore.domain.enums.SessionStatus;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "game_session")
public class GameSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "session_time", nullable = false)
    private LocalTime sessionTime;

    @Column(length = 200)
    private String location;

    @Column(name = "location_address", length = 300)
    private String locationAddress;

    @Column(name = "location_url", length = 500)
    private String locationUrl;

    @Column(name = "broadcast_url", length = 500)
    private String broadcastUrl;

    @Column(name = "telegram_chat_id")
    private Long telegramChatId;

    @Column(name = "telegram_chat_title", length = 200)
    private String telegramChatTitle;

    @Column(name = "telegram_registration_message_id")
    private Long telegramRegistrationMessageId;

    @Column(name = "telegram_contribution_message_id")
    private Long telegramContributionMessageId;

    @Column(name = "fee_amount")
    private Integer feeAmount;

    @Column(name = "fee_recipient", length = 200)
    private String feeRecipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "format_type", nullable = false, length = 100)
    private SessionFormatType formatType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SessionStatus status = SessionStatus.PLANNED;

    @Column(name = "planned_match_duration_minutes")
    private Integer plannedMatchDurationMinutes;

    @Column(name = "session_duration_minutes")
    private Integer sessionDurationMinutes;

    @Column(name = "max_players")
    private Integer maxPlayers;

    @Column(name = "player_format", length = 50)
    private String playerFormat;

    @Column(columnDefinition = "text")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;
}
