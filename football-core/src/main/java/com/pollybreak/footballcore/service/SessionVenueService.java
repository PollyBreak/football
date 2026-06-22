package com.pollybreak.footballcore.service;

import com.pollybreak.footballcore.api.dto.session.CreateSessionVenueRequest;
import com.pollybreak.footballcore.api.dto.session.SessionVenueResponse;
import com.pollybreak.footballcore.domain.entity.SessionVenue;
import com.pollybreak.footballcore.repository.SessionVenueRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionVenueService {

    private final SessionVenueRepository sessionVenueRepository;

    public List<SessionVenueResponse> findAll() {
        return sessionVenueRepository.findAllByOrderByNameAsc().stream()
                .map(SessionVenueResponse::fromEntity)
                .toList();
    }

    @Transactional
    public SessionVenueResponse create(CreateSessionVenueRequest request) {
        return SessionVenueResponse.fromEntity(createVenue(request));
    }

    @Transactional
    public SessionVenue createVenue(CreateSessionVenueRequest request) {
        String name = cleanRequired(request.name(), "Venue name must not be blank");
        sessionVenueRepository.findByNameIgnoreCase(name).ifPresent(existing -> {
            throw new IllegalArgumentException("Venue with this name already exists");
        });

        SessionVenue venue = new SessionVenue();
        venue.setName(name);
        venue.setAddress(cleanOptional(request.address()));
        venue.setGisUrl(cleanOptional(request.gisUrl()));
        venue.setPhotoUrl(cleanOptional(request.photoUrl()));
        return sessionVenueRepository.save(venue);
    }

    @Transactional
    public SessionVenueResponse update(Long id, CreateSessionVenueRequest request) {
        SessionVenue venue = getById(id);
        String name = cleanRequired(request.name(), "Venue name must not be blank");
        sessionVenueRepository.findByNameIgnoreCase(name)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Venue with this name already exists");
                });

        venue.setName(name);
        venue.setAddress(cleanOptional(request.address()));
        venue.setGisUrl(cleanOptional(request.gisUrl()));
        venue.setPhotoUrl(cleanOptional(request.photoUrl()));
        return SessionVenueResponse.fromEntity(sessionVenueRepository.save(venue));
    }

    public SessionVenue getById(Long id) {
        return sessionVenueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Session venue not found: " + id));
    }

    private String cleanRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String cleanOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
