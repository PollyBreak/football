create table session_recurrence_rule (
    id bigserial primary key,
    recurrence_type varchar(50) not null,
    interval_days integer,
    day_of_month integer,
    auto_start_registration boolean not null default false,
    auto_start_contribution_collection boolean not null default false,
    active boolean not null default true,
    current_session_id bigint references game_session(id) on delete set null,
    created_at timestamptz not null default now(),
    constraint chk_session_recurrence_days check (
        (recurrence_type = 'DAYS' and interval_days is not null and interval_days > 0 and day_of_month is null)
        or
        (recurrence_type = 'MONTHLY' and day_of_month is not null and day_of_month between 1 and 31 and interval_days is null)
    )
);

alter table game_session
    add column recurrence_rule_id bigint references session_recurrence_rule(id) on delete set null;

create index idx_game_session_recurrence_rule_id on game_session(recurrence_rule_id);
create unique index ux_session_recurrence_rule_current_session_id on session_recurrence_rule(current_session_id) where current_session_id is not null;
