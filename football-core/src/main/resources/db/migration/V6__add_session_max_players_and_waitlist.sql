alter table game_session
    add column max_players integer;

create table session_waitlist (
    id bigserial primary key,
    session_id bigint not null references game_session(id) on delete cascade,
    player_id bigint not null references player(id) on delete restrict,
    position varchar(50),
    queued_at timestamptz not null default now(),
    left_at timestamptz,
    is_active boolean not null default true,
    unique(session_id, player_id)
);

create index idx_session_waitlist_session_id on session_waitlist(session_id);
create index idx_session_waitlist_player_id on session_waitlist(player_id);
create index idx_session_waitlist_active_queue on session_waitlist(session_id, is_active, queued_at, id);
