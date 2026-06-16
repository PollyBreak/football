alter table player
    add column rating integer not null default 100;

update player
set rating = 100
where rating is null;
