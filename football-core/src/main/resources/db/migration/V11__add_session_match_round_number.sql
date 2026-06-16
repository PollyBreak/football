alter table session_match
    add column round_number integer;

with team_counts as (
    select
        st.session_id,
        greatest(1, (count(*) * (count(*) - 1)) / 2) as matches_per_round
    from session_team st
    group by st.session_id
)
update session_match sm
set round_number = ((sm.match_number - 1) / tc.matches_per_round) + 1
from team_counts tc
where sm.session_id = tc.session_id
  and sm.round_number is null;
