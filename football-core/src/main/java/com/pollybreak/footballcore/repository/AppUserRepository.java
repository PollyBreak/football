package com.pollybreak.footballcore.repository;

import com.pollybreak.footballcore.domain.entity.AppUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByTelegramId(Long telegramId);
}
