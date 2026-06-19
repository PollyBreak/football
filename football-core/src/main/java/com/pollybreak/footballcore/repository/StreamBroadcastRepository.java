package com.pollybreak.footballcore.repository;

import com.pollybreak.footballcore.domain.entity.StreamBroadcast;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StreamBroadcastRepository extends JpaRepository<StreamBroadcast, Long> {

    List<StreamBroadcast> findAllBySession_IdOrderByStreamStartedAtDescIdDesc(Long sessionId);

    Optional<StreamBroadcast> findFirstBySession_IdAndStreamEndedAtIsNullOrderByStreamStartedAtDescIdDesc(Long sessionId);

    Optional<StreamBroadcast> findByIdAndSession_Id(Long id, Long sessionId);
}
