alter table game_session
    add column telegram_contribution_poll_message_id bigint,
    add column telegram_contribution_poll_id varchar(200);

create unique index ux_game_session_contribution_poll_id
    on game_session(telegram_contribution_poll_id)
    where telegram_contribution_poll_id is not null;
