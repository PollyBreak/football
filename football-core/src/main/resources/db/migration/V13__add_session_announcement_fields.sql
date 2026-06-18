alter table game_session
    add column location_address varchar(300),
    add column session_duration_minutes integer,
    add column player_format varchar(50);
