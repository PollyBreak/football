package com.pollybreak.footballcore.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "telegram_known_chat")
public class TelegramKnownChat {

    @Id
    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(length = 200)
    private String title;

    @Column(length = 100)
    private String username;

    @Column(name = "chat_type", length = 50, nullable = false)
    private String chatType;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
