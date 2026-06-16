alter table game_session
    add column session_time time;

alter table game_session
    add column location_url varchar(500);

update game_session
set session_time = '00:00'
where session_time is null;

alter table game_session
    alter column session_time set not null;

