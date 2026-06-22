package com.pollybreak.footballcore.api.controller;

import com.pollybreak.footballcore.api.dto.session.FileUploadResponse;
import com.pollybreak.footballcore.api.dto.session.CreateSessionVenueRequest;
import com.pollybreak.footballcore.api.dto.session.SessionVenueResponse;
import com.pollybreak.footballcore.service.SessionVenuePhotoStorageService;
import com.pollybreak.footballcore.service.SessionVenueService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/session-venues")
@RequiredArgsConstructor
public class SessionVenueController {

    private final SessionVenueService sessionVenueService;
    private final SessionVenuePhotoStorageService sessionVenuePhotoStorageService;

    @GetMapping
    public List<SessionVenueResponse> getVenues() {
        return sessionVenueService.findAll();
    }

    @PostMapping
    public SessionVenueResponse createVenue(@Valid @RequestBody CreateSessionVenueRequest request) {
        return sessionVenueService.create(request);
    }

    @PutMapping("/{venueId}")
    public SessionVenueResponse updateVenue(
            @PathVariable Long venueId,
            @Valid @RequestBody CreateSessionVenueRequest request
    ) {
        return sessionVenueService.update(venueId, request);
    }

    @PostMapping("/photos")
    public FileUploadResponse uploadPhoto(@RequestParam("file") MultipartFile file) {
        String publicPath = sessionVenuePhotoStorageService.save(file);
        return new FileUploadResponse(publicPath);
    }
}
