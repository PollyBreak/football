create table match_player (
    id bigserial primary key,
    match_id bigint not null references session_match(id) on delete cascade,
    team_id bigint not null references session_team(id) on delete restrict,
    player_id bigint not null references player(id) on delete restrict,
    started_at timestamptz not null,
    ended_at timestamptz,
    source varchar(50) not null,
    created_at timestamptz not null default now(),
    constraint chk_match_player_interval check (ended_at is null or ended_at >= started_at)
);

create index idx_match_player_match_id on match_player(match_id);
create index idx_match_player_team_id on match_player(team_id);
create index idx_match_player_player_id on match_player(player_id);
create index idx_match_player_active on match_player(match_id, team_id, player_id) where ended_at is null;
