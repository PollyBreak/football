package com.pollybreak.footballcore.repository;

import com.pollybreak.footballcore.domain.entity.TelegramPendingRegistration;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TelegramPendingRegistrationRepository extends JpaRepository<TelegramPendingRegistration, Long> {

    List<TelegramPendingRegistration> findAllByTelegramIdOrderByCreatedAtAscIdAsc(Long telegramId);

    Optional<TelegramPendingRegistration> findByTelegramIdAndSessionId(Long telegramId, Long sessionId);

    void deleteAllByTelegramId(Long telegramId);
}
