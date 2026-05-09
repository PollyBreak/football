package com.pollybreak.footballcore.api.controller;

import com.pollybreak.footballcore.api.dto.match.GoalRecordedResponse;
import com.pollybreak.footballcore.api.dto.match.MatchEventResponse;
import com.pollybreak.footballcore.api.dto.match.RecordAssistRequest;
import com.pollybreak.footballcore.api.dto.match.RecordGoalRequest;
import com.pollybreak.footballcore.service.MatchEventService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/session-matches/{matchId}/events")
@RequiredArgsConstructor
public class MatchEventController {

    private final MatchEventService matchEventService;

    @GetMapping
    public List<MatchEventResponse> getEvents(@PathVariable Long matchId) {
        return matchEventService.findResponsesByMatchId(matchId);
    }

    @PostMapping("/goals")
    public GoalRecordedResponse recordGoal(
            @PathVariable Long matchId,
            @Valid @RequestBody RecordGoalRequest request
    ) {
        return matchEventService.recordGoal(matchId, request);
    }

    @PostMapping("/assists")
    public MatchEventResponse recordAssist(
            @PathVariable Long matchId,
            @Valid @RequestBody RecordAssistRequest request
    ) {
        return matchEventService.recordAssist(matchId, request);
    }

    @DeleteMapping("/{eventId}")
    public void deleteEvent(
            @PathVariable Long matchId,
            @PathVariable Long eventId
    ) {
        matchEventService.deleteEvent(matchId, eventId);
    }
}
