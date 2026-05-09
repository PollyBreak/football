create table session_player (
    id bigserial primary key,
    session_id bigint not null references game_session(id) on delete cascade,
    player_id bigint not null references player(id) on delete restrict,
    position varchar(50),
    joined_at timestamptz not null default now(),
    left_at timestamptz,
    is_active boolean not null default true,
    unique(session_id, player_id)
);

create index idx_session_player_session_id on session_player(session_id);
create index idx_session_player_player_id on session_player(player_id);
