alter table game_session
    add column telegram_contribution_message_id bigint;

create table session_contribution (
    id bigserial primary key,
    session_id bigint not null references game_session(id) on delete cascade,
    player_id bigint not null references player(id) on delete cascade,
    paid boolean not null default false,
    updated_at timestamptz not null default now(),
    unique(session_id, player_id)
);

create index idx_session_contribution_session_id on session_contribution(session_id);
create index idx_session_contribution_player_id on session_contribution(player_id);
