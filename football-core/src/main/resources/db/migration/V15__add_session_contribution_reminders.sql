create table session_contribution_reminder (
    id bigserial primary key,
    session_id bigint not null references game_session(id) on delete cascade,
    hours_before integer not null,
    sent_at timestamptz,
    created_at timestamptz not null default now(),
    unique(session_id, hours_before),
    constraint chk_session_contribution_reminder_hours check (hours_before > 0)
);

create index idx_session_contribution_reminder_session_id on session_contribution_reminder(session_id);
create index idx_session_contribution_reminder_pending on session_contribution_reminder(sent_at) where sent_at is null;
