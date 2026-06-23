alter table game_session
    add column mvp_voting_enabled boolean not null default false,
    add column mvp_voting_duration_hours integer,
    add column mvp_voting_participant_scope varchar(50) not null default 'ALL',
    add column mvp_voting_telegram_enabled boolean not null default false,
    add column mvp_voting_started_at timestamptz,
    add column mvp_voting_ends_at timestamptz,
    add column telegram_mvp_voting_message_id bigint,
    add column telegram_mvp_result_sent_at timestamptz;

create table session_mvp_vote (
    id bigserial primary key,
    session_id bigint not null references game_session(id) on delete cascade,
    voter_user_id bigint not null references app_user(id),
    candidate_player_id bigint not null references player(id),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uk_session_mvp_vote_voter unique (session_id, voter_user_id)
);

create index idx_session_mvp_vote_session on session_mvp_vote(session_id);
create index idx_session_mvp_vote_candidate on session_mvp_vote(candidate_player_id);
