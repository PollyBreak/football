alter table match_event
    add column linked_event_id bigint references match_event(id) on delete set null;

create index idx_match_event_linked_event_id on match_event(linked_event_id);
