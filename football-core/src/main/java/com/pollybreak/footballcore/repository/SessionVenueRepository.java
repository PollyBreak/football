package com.pollybreak.footballcore.repository;

import com.pollybreak.footballcore.domain.entity.SessionVenue;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionVenueRepository extends JpaRepository<SessionVenue, Long> {

    List<SessionVenue> findAllByOrderByNameAsc();

    Optional<SessionVenue> findByNameIgnoreCase(String name);
}
