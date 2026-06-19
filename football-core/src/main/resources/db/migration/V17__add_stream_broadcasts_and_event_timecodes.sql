create table stream_broadcast (
    id bigserial primary key,
    session_id bigint not null references game_session(id) on delete cascade,
    title varchar(200),
    youtube_video_id varchar(100),
    youtube_broadcast_id varchar(100),
    stream_started_at timestamptz not null,
    stream_ended_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_stream_broadcast_time_order check (
        stream_ended_at is null or stream_ended_at >= stream_started_at
    )
);

create index idx_stream_broadcast_session_id on stream_broadcast(session_id);
create index idx_stream_broadcast_youtube_video_id on stream_broadcast(youtube_video_id);
create index idx_stream_broadcast_youtube_broadcast_id on stream_broadcast(youtube_broadcast_id);
create unique index ux_stream_broadcast_active_session
    on stream_broadcast(session_id)
    where stream_ended_at is null;

alter table match_event
    add column stream_broadcast_id bigint references stream_broadcast(id) on delete set null,
    add column stream_offset_seconds integer,
    add column stream_event_time timestamptz,
    add constraint chk_match_event_stream_offset_non_negative check (
        stream_offset_seconds is null or stream_offset_seconds >= 0
    );

create index idx_match_event_stream_broadcast_id on match_event(stream_broadcast_id);
create index idx_match_event_stream_offset_seconds on match_event(stream_offset_seconds);
