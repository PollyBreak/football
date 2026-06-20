create table if not exists telegram_known_chat (
    chat_id bigint primary key,
    title varchar(200),
    username varchar(100),
    chat_type varchar(50) not null,
    active boolean not null default true,
    created_at timestamptz not null default now()
);
