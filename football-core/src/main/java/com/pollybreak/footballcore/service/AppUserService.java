package com.pollybreak.footballcore.service;

import com.pollybreak.footballcore.domain.entity.AppUser;
import com.pollybreak.footballcore.repository.AppUserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppUserService {

    private final AppUserRepository appUserRepository;

    public List<AppUser> findAll() {
        return appUserRepository.findAll();
    }

    public AppUser getById(Long id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    public AppUser findByTelegramId(Long telegramId) {
        return appUserRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new IllegalArgumentException("User not found by telegramId: " + telegramId));
    }

    @Transactional
    public AppUser save(AppUser appUser) {
        return appUserRepository.save(appUser);
    }
}
