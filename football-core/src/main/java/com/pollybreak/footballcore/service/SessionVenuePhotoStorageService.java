package com.pollybreak.footballcore.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SessionVenuePhotoStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final Path storageDirectory;

    public SessionVenuePhotoStorageService(
            @Value("${app.uploads.venue-photos-dir:uploads/session-venues}") String storageDirectory
    ) {
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Photo file must not be empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Only JPEG, PNG and WebP images are supported");
        }

        try {
            Files.createDirectories(storageDirectory);
            String filename = UUID.randomUUID() + extensionFor(contentType);
            Path target = storageDirectory.resolve(filename).normalize();
            if (!target.startsWith(storageDirectory)) {
                throw new IllegalArgumentException("Invalid photo file path");
            }
            file.transferTo(target);
            return "/uploads/session-venues/" + filename;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save venue photo", exception);
        }
    }

    public Path storageDirectory() {
        return storageDirectory;
    }

    private String extensionFor(String contentType) {
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
