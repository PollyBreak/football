package com.pollybreak.footballcore.api.dto.mvp;

import com.pollybreak.footballcore.domain.enums.MvpVotingParticipantScope;
import java.time.OffsetDateTime;
import java.util.List;

public record SessionMvpVotingResponse(
        Long sessionId,
        boolean enabled,
        boolean started,
        boolean finished,
        OffsetDateTime startedAt,
        OffsetDateTime endsAt,
        MvpVotingParticipantScope participantScope,
        boolean canVote,
        String cannotVoteReason,
        Long selectedPlayerId,
        List<SessionMvpCandidateResponse> candidates,
        List<SessionMvpCandidateResponse> winners
) {
}
