alter table stream_broadcast
    add column timeline_shift_seconds integer not null default 0;
