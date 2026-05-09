package com.pollybreak.footballcore.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pollybreak.footballcore.api.dto.auth.AppUserResponse;
import com.pollybreak.footballcore.api.dto.auth.TelegramAuthRequest;
import com.pollybreak.footballcore.api.dto.auth.TelegramAuthResponse;
import com.pollybreak.footballcore.api.dto.player.PlayerProfileResponse;
import com.pollybreak.footballcore.config.TelegramBotProperties;
import com.pollybreak.footballcore.domain.entity.AppUser;
import com.pollybreak.footballcore.domain.entity.Player;
import com.pollybreak.footballcore.repository.AppUserRepository;
import com.pollybreak.footballcore.repository.PlayerRepository;
import jakarta.annotation.Nullable;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TelegramAuthService {

    private static final long MAX_AUTH_AGE_SECONDS = 86_400;

    private final TelegramBotProperties telegramBotProperties;
    private final AppUserRepository appUserRepository;
    private final PlayerRepository playerRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public TelegramAuthResponse authenticate(TelegramAuthRequest request) {
        if (telegramBotProperties.getToken() == null || telegramBotProperties.getToken().isBlank()) {
            throw new IllegalArgumentException("Telegram bot token is not configured on the backend");
        }

        Map<String, String> data = parseInitData(request.initData());
        validateHash(data, request.initData());
        validateAuthDate(data.get("auth_date"));

        TelegramMiniAppUser telegramUser = parseTelegramUser(data.get("user"));
        AppUser user = appUserRepository.findByTelegramId(telegramUser.id())
                .map(existing -> updateUser(existing, telegramUser))
                .orElseGet(() -> createUser(telegramUser));

        Optional<Player> player = playerRepository.findByUserId(user.getId());

        return new TelegramAuthResponse(
                true,
                player.isEmpty(),
                AppUserResponse.fromEntity(user),
                player.map(PlayerProfileResponse::fromEntity).orElse(null)
        );
    }

    private Map<String, String> parseInitData(String initData) {
        return Arrays.stream(initData.split("&"))
                .map(pair -> pair.split("=", 2))
                .filter(parts -> parts.length == 2)
                .collect(Collectors.toMap(
                        parts -> decode(parts[0]),
                        parts -> decode(parts[1]),
                        (left, right) -> right,
                        LinkedHashMap::new
                ));
    }

    private void validateHash(Map<String, String> data, String rawInitData) {
        String incomingHash = data.get("hash");
        if (incomingHash == null || incomingHash.isBlank()) {
            throw new IllegalArgumentException("Telegram initData hash is missing");
        }

        String dataCheckString = data.entrySet().stream()
                .filter(entry -> !"hash".equals(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("\n"));

        byte[] secretKey = hmacSha256("WebAppData".getBytes(StandardCharsets.UTF_8),
                telegramBotProperties.getToken().getBytes(StandardCharsets.UTF_8));
        String expectedHash = toHex(hmacSha256(secretKey, dataCheckString.getBytes(StandardCharsets.UTF_8)));

        if (!expectedHash.equalsIgnoreCase(incomingHash)) {
            throw new IllegalArgumentException("Telegram initData validation failed");
        }
    }

    private void validateAuthDate(@Nullable String authDateRaw) {
        if (authDateRaw == null || authDateRaw.isBlank()) {
            throw new IllegalArgumentException("Telegram auth_date is missing");
        }

        long authDate = Long.parseLong(authDateRaw);
        long now = Instant.now().getEpochSecond();
        if (now - authDate > MAX_AUTH_AGE_SECONDS) {
            throw new IllegalArgumentException("Telegram initData is too old");
        }
    }

    private TelegramMiniAppUser parseTelegramUser(@Nullable String rawUser) {
        if (rawUser == null || rawUser.isBlank()) {
            throw new IllegalArgumentException("Telegram user payload is missing");
        }
        try {
            return objectMapper.readValue(rawUser, TelegramMiniAppUser.class);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Failed to parse Telegram user payload", exception);
        }
    }

    private AppUser createUser(TelegramMiniAppUser telegramUser) {
        AppUser user = new AppUser();
        user.setTelegramId(telegramUser.id());
        user.setUsername(telegramUser.username());
        user.setDisplayName(buildDisplayName(telegramUser));
        user.setPhotoUrl(telegramUser.photoUrl());
        return appUserRepository.save(user);
    }

    private AppUser updateUser(AppUser user, TelegramMiniAppUser telegramUser) {
        user.setUsername(telegramUser.username());
        user.setDisplayName(buildDisplayName(telegramUser));
        user.setPhotoUrl(telegramUser.photoUrl());
        return user;
    }

    private String buildDisplayName(TelegramMiniAppUser telegramUser) {
        String name = Arrays.asList(telegramUser.firstName(), telegramUser.lastName()).stream()
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" "));
        return name.isBlank()
                ? Optional.ofNullable(telegramUser.username()).filter(value -> !value.isBlank()).orElse("Telegram User")
                : name;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private byte[] hmacSha256(byte[] key, byte[] value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to calculate HMAC-SHA256", exception);
        }
    }

    private String toHex(byte[] value) {
        StringBuilder builder = new StringBuilder(value.length * 2);
        for (byte item : value) {
            builder.append(String.format("%02x", item));
        }
        return builder.toString();
    }

    private record TelegramMiniAppUser(
            Long id,
            @JsonProperty("first_name") String firstName,
            @JsonProperty("last_name") String lastName,
            String username,
            @JsonProperty("photo_url") String photoUrl
    ) {
    }
}
