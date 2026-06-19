create table session_venue (
    id bigserial primary key,
    name varchar(200) not null,
    address varchar(300),
    gis_url varchar(500),
    photo_url text,
    created_at timestamptz not null default now()
);

create unique index ux_session_venue_name_lower on session_venue (lower(name));

alter table game_session
    add column venue_id bigint references session_venue(id) on delete set null;

create index idx_game_session_venue_id on game_session(venue_id);
