package com.pollybreak.footballcore.service;

import com.pollybreak.footballcore.api.dto.stream.StartStreamBroadcastRequest;
import com.pollybreak.footballcore.api.dto.stream.StreamBroadcastResponse;
import com.pollybreak.footballcore.api.dto.stream.StreamTimelineItemResponse;
import com.pollybreak.footballcore.api.dto.stream.StreamTimelineResponse;
import com.pollybreak.footballcore.api.dto.stream.UpdateStreamShiftRequest;
import com.pollybreak.footballcore.domain.entity.GameSession;
import com.pollybreak.footballcore.domain.entity.MatchEvent;
import com.pollybreak.footballcore.domain.entity.SessionMatch;
import com.pollybreak.footballcore.domain.entity.SessionTeam;
import com.pollybreak.footballcore.domain.entity.StreamBroadcast;
import com.pollybreak.footballcore.domain.enums.MatchEventType;
import com.pollybreak.footballcore.domain.enums.SessionFormatType;
import com.pollybreak.footballcore.repository.GameSessionRepository;
import com.pollybreak.footballcore.repository.MatchEventRepository;
import com.pollybreak.footballcore.repository.StreamBroadcastRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StreamBroadcastService {

    private static final List<MatchEventType> TIMELINE_EVENT_TYPES = List.of(
            MatchEventType.MATCH_STARTED,
            MatchEventType.GOAL,
            MatchEventType.OWN_GOAL,
            MatchEventType.GOAL_CANCELLED,
            MatchEventType.PENALTY
    );

    private final StreamBroadcastRepository streamBroadcastRepository;
    private final GameSessionRepository gameSessionRepository;
    private final MatchEventRepository matchEventRepository;

    public List<StreamBroadcastResponse> findBySession(Long sessionId) {
        return streamBroadcastRepository.findAllBySession_IdOrderByStreamStartedAtDescIdDesc(sessionId).stream()
                .map(StreamBroadcastResponse::fromEntity)
                .toList();
    }

    public StreamBroadcastResponse getResponse(Long sessionId, Long streamId) {
        return StreamBroadcastResponse.fromEntity(getByIdAndSession(streamId, sessionId));
    }

    @Transactional
    public StreamBroadcastResponse start(Long sessionId, StartStreamBroadcastRequest request) {
        GameSession session = gameSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Game session not found: " + sessionId));
        streamBroadcastRepository.findFirstBySession_IdAndStreamEndedAtIsNullOrderByStreamStartedAtDescIdDesc(sessionId)
                .ifPresent(active -> {
                    throw new IllegalArgumentException("Session already has an active stream broadcast");
                });

        return StreamBroadcastResponse.fromEntity(createBroadcast(session, request, OffsetDateTime.now()));
    }

    @Transactional
    public StreamBroadcastResponse restart(Long sessionId, StartStreamBroadcastRequest request) {
        GameSession session = gameSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Game session not found: " + sessionId));
        OffsetDateTime now = OffsetDateTime.now();
        streamBroadcastRepository.findFirstBySession_IdAndStreamEndedAtIsNullOrderByStreamStartedAtDescIdDesc(sessionId)
                .ifPresent(active -> {
                    if (active.getStreamEndedAt() == null) {
                        active.setStreamEndedAt(now);
                    }
                    active.setUpdatedAt(now);
                    streamBroadcastRepository.saveAndFlush(active);
                });

        return StreamBroadcastResponse.fromEntity(createBroadcast(session, request, now));
    }

    @Transactional
    public StreamBroadcastResponse finish(Long sessionId, Long streamId) {
        StreamBroadcast broadcast = getByIdAndSession(streamId, sessionId);
        if (broadcast.getStreamEndedAt() == null) {
            broadcast.setStreamEndedAt(OffsetDateTime.now());
        }
        broadcast.setUpdatedAt(OffsetDateTime.now());
        return StreamBroadcastResponse.fromEntity(broadcast);
    }

    @Transactional
    public StreamBroadcastResponse addShift(Long sessionId, Long streamId, UpdateStreamShiftRequest request) {
        StreamBroadcast broadcast = getByIdAndSession(streamId, sessionId);
        int shift = request == null || request.shiftSeconds() == null ? 0 : request.shiftSeconds();
        broadcast.setTimelineShiftSeconds((broadcast.getTimelineShiftSeconds() == null ? 0 : broadcast.getTimelineShiftSeconds()) + shift);
        broadcast.setUpdatedAt(OffsetDateTime.now());
        return StreamBroadcastResponse.fromEntity(broadcast);
    }

    public void attachActiveStreamTimecode(MatchEvent event, Long sessionId) {
        streamBroadcastRepository.findFirstBySession_IdAndStreamEndedAtIsNullOrderByStreamStartedAtDescIdDesc(sessionId)
                .ifPresent(broadcast -> attachStreamTimecode(event, broadcast, OffsetDateTime.now()));
    }

    public StreamTimelineResponse getTimeline(Long sessionId, Long streamId) {
        StreamBroadcast broadcast = getByIdAndSession(streamId, sessionId);
        int shift = broadcast.getTimelineShiftSeconds() == null ? 0 : broadcast.getTimelineShiftSeconds();
        List<MatchEvent> events = matchEventRepository
                .findAllByStreamBroadcast_IdOrderByStreamOffsetSecondsAscIdAsc(streamId)
                .stream()
                .filter(event -> TIMELINE_EVENT_TYPES.contains(event.getEventType()))
                .sorted(Comparator
                        .comparing(MatchEvent::getStreamOffsetSeconds, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(MatchEvent::getId))
                .toList();
        List<StreamTimelineItemResponse> items = events.stream()
                .map(event -> toTimelineItem(event, shift))
                .toList();
        String descriptionBlock = buildDescriptionBlock(broadcast.getSession(), events, shift);

        return new StreamTimelineResponse(
                broadcast.getId(),
                sessionId,
                broadcast.getYoutubeVideoId(),
                shift,
                descriptionBlock,
                items
        );
    }

    private StreamBroadcast getByIdAndSession(Long streamId, Long sessionId) {
        return streamBroadcastRepository.findByIdAndSession_Id(streamId, sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Stream broadcast not found for this session"));
    }

    private StreamBroadcast createBroadcast(GameSession session, StartStreamBroadcastRequest request, OffsetDateTime now) {
        StreamBroadcast broadcast = new StreamBroadcast();
        broadcast.setSession(session);
        broadcast.setTitle(request != null ? request.title() : null);
        broadcast.setYoutubeVideoId(request != null ? request.youtubeVideoId() : null);
        broadcast.setYoutubeBroadcastId(request != null ? request.youtubeBroadcastId() : null);
        broadcast.setStreamStartedAt(request != null && request.streamStartedAt() != null
                ? request.streamStartedAt()
                : now);
        broadcast.setTimelineShiftSeconds(0);
        broadcast.setUpdatedAt(now);
        return streamBroadcastRepository.save(broadcast);
    }

    private void attachStreamTimecode(MatchEvent event, StreamBroadcast broadcast, OffsetDateTime eventTime) {
        int offsetSeconds = (int) Math.max(0, Duration.between(broadcast.getStreamStartedAt(), eventTime).getSeconds());
        event.setStreamBroadcast(broadcast);
        event.setStreamOffsetSeconds(offsetSeconds);
        event.setStreamEventTime(eventTime);
    }

    private StreamTimelineItemResponse toTimelineItem(MatchEvent event, int shift) {
        int rawOffset = event.getStreamOffsetSeconds() == null ? 0 : event.getStreamOffsetSeconds();
        int adjustedOffset = Math.max(0, rawOffset + shift);
        String line = formatTimecode(adjustedOffset) + " " + timelineLabel(event, null);
        return new StreamTimelineItemResponse(
                event.getId(),
                event.getMatch().getId(),
                event.getMatch().getMatchNumber(),
                event.getMatch().getRoundNumber(),
                event.getEventType(),
                rawOffset,
                adjustedOffset,
                formatTimecode(adjustedOffset),
                playerName(event),
                relatedPlayerName(event),
                event.getTeam() != null ? event.getTeam().getName() : null,
                line
        );
    }

    private String buildDescriptionBlock(GameSession session, List<MatchEvent> events, int shift) {
        Map<Long, List<MatchEvent>> eventsByMatch = new LinkedHashMap<>();
        for (MatchEvent event : events) {
            eventsByMatch.computeIfAbsent(event.getMatch().getId(), ignored -> new ArrayList<>()).add(event);
        }

        List<String> lines = new ArrayList<>();
        Integer currentRound = null;
        boolean showRounds = session.getFormatType() == SessionFormatType.ROUND_ROBIN;

        for (List<MatchEvent> matchEvents : eventsByMatch.values()) {
            if (matchEvents.isEmpty()) {
                continue;
            }

            SessionMatch match = matchEvents.get(0).getMatch();
            Integer roundNumber = match.getRoundNumber();
            if (showRounds && (currentRound == null || !currentRound.equals(roundNumber))) {
                if (!lines.isEmpty()) {
                    lines.add("");
                }
                currentRound = roundNumber;
                lines.add((roundNumber == null ? 1 : roundNumber) + " круг");
            } else if (!lines.isEmpty()) {
                lines.add("");
            }

            MatchTimelineState matchState = new MatchTimelineState(match);
            boolean matchHeaderWritten = false;
            for (MatchEvent event : matchEvents) {
                if (event.getEventType() == MatchEventType.MATCH_STARTED) {
                    lines.add(formatTimecode(adjustedOffset(event, shift)) + " " + matchHeader(match));
                    matchHeaderWritten = true;
                    continue;
                }
                if (!matchHeaderWritten) {
                    lines.add(formatTimecode(adjustedOffset(event, shift)) + " " + matchHeader(match));
                    matchHeaderWritten = true;
                }

                matchState.apply(event);
                lines.add(formatTimecode(adjustedOffset(event, shift)) + " " + timelineLabel(event, matchState.scoreLabel()));
            }
        }

        return String.join("\n", lines);
    }

    private String matchHeader(SessionMatch match) {
        return "🏁 Матч " + match.getMatchNumber()
                + " " + teamEmoji(match.getTeamA()) + " " + match.getTeamA().getName()
                + "  " + match.getTeamAScore() + " - " + match.getTeamBScore() + " "
                + teamEmoji(match.getTeamB()) + " " + match.getTeamB().getName();
    }

    private int adjustedOffset(MatchEvent event, int shift) {
        return Math.max(0, (event.getStreamOffsetSeconds() == null ? 0 : event.getStreamOffsetSeconds()) + shift);
    }

    private String timelineLabel(MatchEvent event, String scoreLabel) {
        String matchLabel = "Матч " + event.getMatch().getMatchNumber() + ": ";
        String scoreSuffix = scoreLabel == null ? "" : " (" + scoreLabel + ")";
        return switch (event.getEventType()) {
            case MATCH_STARTED -> matchLabel + "начало матча";
            case MATCH_FINISHED -> matchLabel + "завершение матча";
            case MATCH_PAUSED -> matchLabel + "пауза";
            case MATCH_RESUMED -> matchLabel + "возобновление";
            case ASSIST -> matchLabel + playerNameOrFallback(event) + ", ассист";
            case PENALTY -> "⚠️ ПЕНАЛЬТИ";
            case GOAL_CANCELLED -> "❌ Отмена гола" + scoreSuffix;
            case OWN_GOAL -> "⚽ АВТОГОЛ, " + playerNameOrFallback(event) + scoreSuffix;
            case GOAL -> "⚽ ГОЛ, " + playerNameOrFallback(event) + scoreSuffix;
            default -> matchLabel + event.getEventType().name();
        };
    }

    private String teamEmoji(SessionTeam team) {
        String marker = ((team.getName() == null ? "" : team.getName()) + " " + (team.getColor() == null ? "" : team.getColor())).toLowerCase();
        if (marker.contains("red") || marker.contains("крас")) {
            return "🔴";
        }
        if (marker.contains("green") || marker.contains("зелен") || marker.contains("зелён")) {
            return "🟢";
        }
        if (marker.contains("blue") || marker.contains("син")) {
            return "🔵";
        }
        return "⚪";
    }

    private String playerNameOrFallback(MatchEvent event) {
        String name = playerName(event);
        return name == null || name.isBlank() ? "Игрок" : name;
    }

    private String playerName(MatchEvent event) {
        if (event.getPlayer() == null) {
            return null;
        }
        String firstName = event.getPlayer().getFirstName();
        String lastName = event.getPlayer().getLastName();
        return lastName == null || lastName.isBlank() ? firstName : firstName + " " + lastName;
    }

    private String relatedPlayerName(MatchEvent event) {
        if (event.getRelatedPlayer() == null) {
            return null;
        }
        String firstName = event.getRelatedPlayer().getFirstName();
        String lastName = event.getRelatedPlayer().getLastName();
        return lastName == null || lastName.isBlank() ? firstName : firstName + " " + lastName;
    }

    private String formatTimecode(int offsetSeconds) {
        int hours = offsetSeconds / 3600;
        int minutes = (offsetSeconds % 3600) / 60;
        int seconds = offsetSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private static class MatchTimelineState {
        private final Long teamAId;
        private final Long teamBId;
        private int teamAScore;
        private int teamBScore;

        private MatchTimelineState(SessionMatch match) {
            this.teamAId = match.getTeamA().getId();
            this.teamBId = match.getTeamB().getId();
        }

        private void apply(MatchEvent event) {
            if (event.getTeam() == null) {
                return;
            }
            if (event.getEventType() == MatchEventType.GOAL || event.getEventType() == MatchEventType.OWN_GOAL) {
                addGoal(event.getTeam().getId());
            }
            if (event.getEventType() == MatchEventType.GOAL_CANCELLED) {
                removeGoal(event.getTeam().getId());
            }
        }

        private void addGoal(Long teamId) {
            if (teamAId.equals(teamId)) {
                teamAScore++;
            } else if (teamBId.equals(teamId)) {
                teamBScore++;
            }
        }

        private void removeGoal(Long teamId) {
            if (teamAId.equals(teamId)) {
                teamAScore = Math.max(0, teamAScore - 1);
            } else if (teamBId.equals(teamId)) {
                teamBScore = Math.max(0, teamBScore - 1);
            }
        }

        private String scoreLabel() {
            return teamAScore + " - " + teamBScore;
        }
    }
}
