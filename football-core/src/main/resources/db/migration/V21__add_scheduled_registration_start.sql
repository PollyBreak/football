alter table game_session
    add column auto_start_registration boolean not null default false,
    add column registration_open_hours_before integer;

alter table session_recurrence_rule
    add column registration_open_hours_before integer;

update game_session
set registration_open_hours_before = 120
where auto_start_registration = true
  and registration_open_hours_before is null;

update session_recurrence_rule
set registration_open_hours_before = 120
where auto_start_registration = true
  and registration_open_hours_before is null;

update game_session session
set auto_start_registration = rule.auto_start_registration,
    registration_open_hours_before = rule.registration_open_hours_before
from session_recurrence_rule rule
where session.recurrence_rule_id = rule.id
  and rule.auto_start_registration = true
  and session.telegram_registration_message_id is null;

alter table game_session
    add constraint chk_game_session_registration_open_hours_before
        check (registration_open_hours_before is null or registration_open_hours_before >= 0);

alter table session_recurrence_rule
    add constraint chk_session_recurrence_registration_open_hours_before
        check (registration_open_hours_before is null or registration_open_hours_before >= 0);

create index idx_game_session_auto_registration_due
    on game_session(auto_start_registration, telegram_registration_message_id, session_date, session_time)
    where auto_start_registration = true and telegram_registration_message_id is null;
