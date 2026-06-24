alter table game_session
    add column team_count integer;

update game_session gs
set team_count = coalesce(team_counts.count, case when gs.format_type = 'DUEL' then 2 else 3 end)
from (
    select session_id, count(*)::integer as count
    from session_team
    group by session_id
) team_counts
where team_counts.session_id = gs.id;

update game_session
set team_count = case when format_type = 'DUEL' then 2 else 3 end
where team_count is null;
