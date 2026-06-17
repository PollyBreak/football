alter table game_session
    add column telegram_chat_id bigint,
    add column telegram_chat_title varchar(200),
    add column telegram_registration_message_id bigint,
    add column fee_amount integer,
    add column fee_recipient varchar(200);

create table session_registration (
    id bigserial primary key,
    session_id bigint not null references game_session(id) on delete cascade,
    player_id bigint not null references player(id) on delete cascade,
    status varchar(50) not null,
    updated_at timestamptz not null default now(),
    unique(session_id, player_id)
);

create index idx_session_registration_session_id on session_registration(session_id);
create index idx_session_registration_player_id on session_registration(player_id);

create table telegram_pending_registration (
    id bigserial primary key,
    telegram_id bigint not null,
    session_id bigint not null references game_session(id) on delete cascade,
    status varchar(50) not null,
    created_at timestamptz not null default now(),
    unique(telegram_id, session_id)
);

create index idx_telegram_pending_registration_telegram_id on telegram_pending_registration(telegram_id);
