create table app_user (
                          id bigserial primary key,
                          telegram_id bigint unique,
                          username varchar(100),
                          display_name varchar(150) not null,
                          created_at timestamptz not null default now()
);

create table player (
                        id bigserial primary key,
                        user_id bigint unique references app_user(id) on delete set null,
                        first_name varchar(100) not null,
                        last_name varchar(100),
                        nickname varchar(100),
                        default_position varchar(50),
                        is_active boolean not null default true,
                        created_at timestamptz not null default now()
);

create table game_session (
                              id bigserial primary key,
                              title varchar(200) not null,
                              session_date date not null,
                              location varchar(200),
                              format_type varchar(100) not null,
                              status varchar(50) not null default 'PLANNED',
                              planned_match_duration_minutes integer,
                              notes text,
                              created_by bigint references app_user(id) on delete set null,
                              created_at timestamptz not null default now(),
                              started_at timestamptz,
                              ended_at timestamptz
);

create table session_team (
                              id bigserial primary key,
                              session_id bigint not null references game_session(id) on delete cascade,
                              name varchar(100) not null,
                              color varchar(50),
                              display_order integer not null,
                              created_at timestamptz not null default now()
);

create table session_team_player (
                                     id bigserial primary key,
                                     session_team_id bigint not null references session_team(id) on delete cascade,
                                     player_id bigint not null references player(id) on delete restrict,
                                     position varchar(50),
                                     joined_at timestamptz not null default now(),
                                     left_at timestamptz,
                                     is_active boolean not null default true,
                                     unique(session_team_id, player_id)
);

create table session_match (
                       id bigserial primary key,
                       session_id bigint not null references game_session(id) on delete cascade,
                       team_a_id bigint not null references session_team(id) on delete restrict,
                       team_b_id bigint not null references session_team(id) on delete restrict,
                       match_number integer not null,
                       status varchar(50) not null default 'PLANNED',
                       planned_duration_minutes integer,
                       started_at timestamptz,
                       ended_at timestamptz,
                       team_a_score integer not null default 0,
                       team_b_score integer not null default 0,
                       winning_team_id bigint references session_team(id) on delete set null,
                       created_at timestamptz not null default now(),
                       constraint chk_match_teams_different check (team_a_id <> team_b_id),
                       unique(session_id, match_number)
);

create table match_event (
                             id bigserial primary key,
                             match_id bigint not null references session_match(id) on delete cascade,
                             event_type varchar(50) not null,
                             team_id bigint references session_team(id) on delete set null,
                             player_id bigint references player(id) on delete set null,
                             related_player_id bigint references player(id) on delete set null,
                             minute_in_match integer,
                             second_in_match integer,
                             event_time timestamptz not null default now(),
                             created_by bigint references app_user(id) on delete set null,
                             payload jsonb,
                             created_at timestamptz not null default now()
);

create index idx_player_user_id on player(user_id);
create index idx_session_date on game_session(session_date);
create index idx_session_team_session_id on session_team(session_id);
create index idx_session_team_player_team_id on session_team_player(session_team_id);
create index idx_session_team_player_player_id on session_team_player(player_id);
create index idx_match_session_id on session_match(session_id);
create index idx_match_team_a_id on session_match(team_a_id);
create index idx_match_team_b_id on session_match(team_b_id);
create index idx_match_event_match_id on match_event(match_id);
create index idx_match_event_player_id on match_event(player_id);
create index idx_match_event_team_id on match_event(team_id);
create index idx_match_event_event_type on match_event(event_type);
create index idx_match_event_event_time on match_event(event_time);