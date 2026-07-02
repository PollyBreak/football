alter table game_session
    add column session_rating_poll_enabled boolean not null default false,
    add column session_rating_average numeric(3, 2),
    add column session_rating_vote_count integer not null default 0,
    add column telegram_session_rating_summary_message_id bigint,
    add column telegram_session_rating_poll_message_id bigint,
    add column telegram_session_rating_poll_id varchar(200);

create table session_rating_vote (
    id bigserial primary key,
    session_id bigint not null references game_session(id) on delete cascade,
    telegram_user_id bigint not null,
    rating integer not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_session_rating_vote_rating check (rating between 1 and 5),
    constraint uk_session_rating_vote_user unique (session_id, telegram_user_id)
);

create index idx_session_rating_vote_session on session_rating_vote(session_id);
create unique index ux_game_session_rating_poll_id
    on game_session(telegram_session_rating_poll_id)
    where telegram_session_rating_poll_id is not null;
