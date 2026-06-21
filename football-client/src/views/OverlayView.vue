<template>
  <main class="overlay-page">
    <section v-if="currentMatch" class="overlay-scoreboard" :class="{ 'is-offline': connectionStatus !== 'live' }">
      <div class="overlay-scoreboard__meta">
        <span class="overlay-live-row">
          <span class="overlay-status-dot"></span>
          {{ statusLabel }}
        </span>
        <span v-if="currentMatch">Круг {{ currentMatch.roundNumber ?? 1 }} · матч {{ currentMatch.matchNumber }}</span>
      </div>

      <div v-if="currentMatch" class="overlay-scoreboard__match">
        <div class="overlay-team-name">
          <img class="overlay-team-logo" :src="teamLogo(leftTeam)" alt="" />
          <span :style="teamNameStyle(leftTeam)">{{ currentMatch.teamAName }}</span>
        </div>

        <div class="overlay-score-center">
          <div class="overlay-score">{{ currentMatch.teamAScore }}<span>:</span>{{ currentMatch.teamBScore }}</div>
          <strong class="overlay-time-badge">{{ minuteLabel }}</strong>
        </div>

        <div class="overlay-team-name overlay-team-name--right">
          <span :style="teamNameStyle(rightTeam)">{{ currentMatch.teamBName }}</span>
          <img class="overlay-team-logo" :src="teamLogo(rightTeam)" alt="" />
        </div>
      </div>

      <div v-else class="overlay-scoreboard__empty">Матч не выбран</div>

      <div v-if="goalRows.length" class="overlay-goal-list">
        <div class="overlay-goal-list__column">
          <p v-for="goal in leftGoalRows" :key="goal.id">
            <span>{{ goal.minute }}</span>
            <strong>{{ goal.name }}</strong>
          </p>
        </div>
        <div class="overlay-goal-list__column overlay-goal-list__column--right">
          <p v-for="goal in rightGoalRows" :key="goal.id">
            <span>{{ goal.minute }}</span>
            <strong>{{ goal.name }}</strong>
          </p>
        </div>
      </div>
    </section>

    <section v-if="currentMatch" class="overlay-lineups-shell">
      <div class="overlay-lineups">
        <div class="overlay-team-panel">
          <div class="overlay-lineups__players">
            <article
              v-for="player in visiblePlayers(leftTeam)"
              :key="player.id"
              class="overlay-player-card"
              :style="playerCardStyle(leftTeam)"
            >
              <div class="overlay-card-top">
                <div class="overlay-card-position">
                  <span>{{ positionLabel(player.position) }}</span>
                  <img :src="teamLogo(leftTeam)" alt="" />
                </div>
              </div>
              <div class="overlay-player-card__photo">
                <img v-if="player.photoUrl" :src="player.photoUrl" :alt="displayPlayerName(player)" />
                <span v-else>{{ initials(displayPlayerName(player)) }}</span>
              </div>
              <strong class="overlay-player-card__name">{{ displayPlayerName(player) }}</strong>
              <div v-if="playerStatsLabel(player.playerId)" class="overlay-player-card__stats">
                {{ playerStatsLabel(player.playerId) }}
              </div>
            </article>
          </div>
        </div>

        <div class="overlay-lineups__divider">
          <img src="/vs.png" alt="VS" />
        </div>

        <div class="overlay-team-panel">
          <div class="overlay-lineups__players overlay-lineups__players--right">
            <article
              v-for="player in visiblePlayers(rightTeam)"
              :key="player.id"
              class="overlay-player-card"
              :style="playerCardStyle(rightTeam)"
            >
              <div class="overlay-card-top">
                <div class="overlay-card-position">
                  <span>{{ positionLabel(player.position) }}</span>
                  <img :src="teamLogo(rightTeam)" alt="" />
                </div>
              </div>
              <div class="overlay-player-card__photo">
                <img v-if="player.photoUrl" :src="player.photoUrl" :alt="displayPlayerName(player)" />
                <span v-else>{{ initials(displayPlayerName(player)) }}</span>
              </div>
              <strong class="overlay-player-card__name">{{ displayPlayerName(player) }}</strong>
              <div v-if="playerStatsLabel(player.playerId)" class="overlay-player-card__stats">
                {{ playerStatsLabel(player.playerId) }}
              </div>
            </article>
          </div>
        </div>
      </div>
    </section>

    <section v-if="intermissionVisible" class="overlay-intermission">
      <div class="overlay-standings">
        <h2>Турнирная таблица</h2>
        <div class="overlay-standings__head">
          <span>#</span>
          <span>Команда</span>
          <span>И</span>
          <span>В</span>
          <span>Н</span>
          <span>П</span>
          <span>М</span>
          <span>О</span>
        </div>
        <div v-if="standingsRows.length" class="overlay-standings__body">
          <div v-for="row in standingsRows" :key="row.team.id" class="overlay-standings__row">
            <span class="overlay-standings__place">{{ row.place }}</span>
            <span class="overlay-standings__team">
              <img :src="teamLogo(row.team)" alt="" />
              <strong :style="standingTeamNameStyle(row.team)">{{ row.team.name }}</strong>
            </span>
            <span>{{ row.played }}</span>
            <span>{{ row.wins }}</span>
            <span>{{ row.draws }}</span>
            <span>{{ row.losses }}</span>
            <span>{{ row.goalsFor }}:{{ row.goalsAgainst }}</span>
            <strong>{{ row.points }}</strong>
          </div>
        </div>
        <p v-else class="overlay-empty-note">Команды пока не добавлены</p>
      </div>

      <div class="overlay-intermission__side">
        <div class="overlay-match-card">
          <h2>Результат последнего матча</h2>
          <div v-if="lastFinishedMatch" class="overlay-result-match">
            <div class="overlay-result-team">
              <img :src="teamLogo(findTeam(lastFinishedMatch.teamAId))" alt="" />
              <strong :style="teamNameStyle(findTeam(lastFinishedMatch.teamAId))">{{ lastFinishedMatch.teamAName }}</strong>
            </div>
            <div class="overlay-result-score">{{ lastFinishedMatch.teamAScore }}<span>:</span>{{ lastFinishedMatch.teamBScore }}</div>
            <div class="overlay-result-team overlay-result-team--right">
              <img :src="teamLogo(findTeam(lastFinishedMatch.teamBId))" alt="" />
              <strong :style="teamNameStyle(findTeam(lastFinishedMatch.teamBId))">{{ lastFinishedMatch.teamBName }}</strong>
            </div>
          </div>
          <div v-if="lastFinishedMatch && lastFinishedMatchGoals.length" class="overlay-result-goals">
            <div class="overlay-result-goals__column">
              <p v-for="goal in lastFinishedLeftGoals" :key="goal.id">
                <span>{{ goal.minute }}</span>
                <strong>{{ goal.name }}</strong>
              </p>
            </div>
            <div class="overlay-result-goals__column overlay-result-goals__column--right">
              <p v-for="goal in lastFinishedRightGoals" :key="goal.id">
                <span>{{ goal.minute }}</span>
                <strong>{{ goal.name }}</strong>
              </p>
            </div>
          </div>
          <p v-if="!lastFinishedMatch" class="overlay-empty-note">Сыгранных матчей пока нет</p>
        </div>

        <div class="overlay-match-card">
          <h2>Следующий матч</h2>
          <div v-if="nextMatch" class="overlay-next-match">
            <div class="overlay-next-team">
              <img :src="teamLogo(findTeam(nextMatch.teamAId))" alt="" />
              <strong :style="teamNameStyle(findTeam(nextMatch.teamAId))">{{ nextMatch.teamAName }}</strong>
            </div>
            <img class="overlay-next-vs" src="/vs.png" alt="VS" />
            <div class="overlay-next-team">
              <img :src="teamLogo(findTeam(nextMatch.teamBId))" alt="" />
              <strong :style="teamNameStyle(findTeam(nextMatch.teamBId))">{{ nextMatch.teamBName }}</strong>
            </div>
          </div>
          <p v-else class="overlay-empty-note">Следующий матч не выбран</p>
        </div>
      </div>
    </section>

    <Transition name="goal-toast">
      <section
        v-if="goalToast"
        class="overlay-goal-toast"
        :class="{ 'is-cancelled': goalToast.cancelled, 'has-assist': goalToast.assist && !goalToast.cancelled }"
      >
        <template v-if="goalToast.assist && !goalToast.cancelled">
          <div class="overlay-goal-person overlay-goal-person--assist">
            <span>АССИСТ</span>
            <div class="overlay-goal-photo">
              <img v-if="goalToast.assistPhotoUrl" :src="goalToast.assistPhotoUrl" :alt="goalToast.assist" />
              <strong v-else>{{ initials(goalToast.assist) }}</strong>
            </div>
            <strong>{{ goalToast.assist }}</strong>
          </div>

          <div class="overlay-goal-arrow">→</div>

          <div class="overlay-goal-person">
            <span>{{ goalToast.ownGoal ? 'АВТОГОЛ' : 'ГОЛ' }}</span>
            <div class="overlay-goal-photo">
              <img v-if="goalToast.scorerPhotoUrl" :src="goalToast.scorerPhotoUrl" :alt="goalToast.scorer" />
              <strong v-else>{{ initials(goalToast.scorer) }}</strong>
            </div>
            <strong>{{ goalToast.scorer }}</strong>
          </div>
        </template>

        <template v-else>
          <p>{{ goalToast.cancelled ? 'Гол отменен' : goalToast.ownGoal ? 'АВТОГОЛ' : 'ГОЛ' }}</p>
          <div class="overlay-goal-photo overlay-goal-photo--solo">
            <img v-if="goalToast.scorerPhotoUrl" :src="goalToast.scorerPhotoUrl" :alt="goalToast.scorer" />
            <strong v-else>{{ initials(goalToast.scorer) }}</strong>
          </div>
          <strong>{{ goalToast.scorer }}</strong>
        </template>
      </section>
    </Transition>

    <section v-if="error" class="overlay-error">{{ error }}</section>
  </main>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { api } from '../lib/api';
import type { MatchEvent, OverlayEvent, OverlayState, OverlayTeam, PlayerPosition, SessionMatch, SessionTeamPlayer } from '../types';

interface OverlayStandingRow {
  place: number;
  team: OverlayTeam;
  played: number;
  wins: number;
  draws: number;
  losses: number;
  goalsFor: number;
  goalsAgainst: number;
  goalDifference: number;
  points: number;
}

const props = defineProps<{
  sessionId: string;
}>();

const route = useRoute();
const state = ref<OverlayState | null>(null);
const now = ref(new Date());
const connectionStatus = ref<'connecting' | 'live' | 'offline'>('connecting');
const error = ref('');
const goalToast = ref<{
  scorer: string;
  scorerPhotoUrl: string | null;
  assist: string | null;
  assistPhotoUrl: string | null;
  ownGoal: boolean;
  cancelled: boolean;
} | null>(null);

let events: EventSource | null = null;
let clockTimer: number | null = null;
let toastTimer: number | null = null;

const sessionIdNumber = computed(() => Number(props.sessionId));
const preferredMatchId = computed(() => {
  const raw = route.query.matchId;
  const value = Array.isArray(raw) ? raw[0] : raw;
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
});

const selectedMatch = computed(() => state.value?.currentMatch ?? null);
const currentMatch = computed(() => {
  const match = selectedMatch.value;
  return match?.status === 'IN_PROGRESS' ? match : null;
});
const leftTeam = computed(() => findTeam(currentMatch.value?.teamAId ?? null));
const rightTeam = computed(() => findTeam(currentMatch.value?.teamBId ?? null));

const statusLabel = computed(() => {
  if (connectionStatus.value === 'live') {
    return currentMatch.value?.status === 'IN_PROGRESS' ? 'LIVE' : 'OVERLAY';
  }
  if (connectionStatus.value === 'connecting') {
    return 'CONNECTING';
  }
  return 'OFFLINE';
});

const minuteLabel = computed(() => {
  const match = currentMatch.value;
  if (!match) {
    return '--:--';
  }
  if (match.status === 'FINISHED') {
    return 'FT';
  }
  if (!match.startedAt) {
    return '00:00';
  }

  const elapsedSeconds = Math.max(0, Math.floor((now.value.getTime() - new Date(match.startedAt).getTime()) / 1000));
  const minutes = Math.floor(elapsedSeconds / 60);
  const seconds = elapsedSeconds % 60;
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
});

const goalRows = computed(() => {
  return (state.value?.currentMatchEvents ?? [])
    .filter((event) => event.eventType === 'GOAL' || event.eventType === 'OWN_GOAL')
    .map((event) => ({
      id: event.id,
      teamId: event.teamId,
      minute: formatEventMinute(event),
      name: eventDisplayName(event)
    }))
    .slice(-5);
});
const leftGoalRows = computed(() => goalRows.value.filter((goal) => goal.teamId === currentMatch.value?.teamAId));
const rightGoalRows = computed(() => goalRows.value.filter((goal) => goal.teamId === currentMatch.value?.teamBId));
const intermissionVisible = computed(() => Boolean(state.value) && !currentMatch.value);

const finishedMatches = computed(() => {
  const matches = [...(state.value?.matches ?? [])];
  const selected = selectedMatch.value;
  if (selected?.status === 'FINISHED' && !matches.some((match) => match.id === selected.id)) {
    matches.push(selected);
  }

  return matches
    .filter((match) => match.status === 'FINISHED')
    .sort((first, second) => matchSortValue(first) - matchSortValue(second));
});

const lastFinishedMatch = computed(() => state.value?.lastFinishedMatch ?? finishedMatches.value[finishedMatches.value.length - 1] ?? null);
const lastFinishedMatchGoals = computed(() => {
  const match = lastFinishedMatch.value;
  if (!match) {
    return [];
  }

  const events = state.value?.lastFinishedMatchEvents ?? state.value?.sessionEvents ?? [];
  return events
    .filter((event) => event.matchId === match.id && (event.eventType === 'GOAL' || event.eventType === 'OWN_GOAL'))
    .sort((first, second) => {
      return (first.minuteInMatch ?? 0) - (second.minuteInMatch ?? 0)
        || (first.secondInMatch ?? 0) - (second.secondInMatch ?? 0)
        || first.id - second.id;
    })
    .map((event) => ({
      id: event.id,
      teamId: event.teamId,
      minute: formatEventMinute(event),
      name: eventDisplayName(event)
    }));
});
const lastFinishedLeftGoals = computed(() => {
  const match = lastFinishedMatch.value;
  return lastFinishedMatchGoals.value.filter((goal) => goal.teamId === match?.teamAId);
});
const lastFinishedRightGoals = computed(() => {
  const match = lastFinishedMatch.value;
  return lastFinishedMatchGoals.value.filter((goal) => goal.teamId === match?.teamBId);
});

const nextMatch = computed(() => {
  if (state.value?.nextMatch) {
    return state.value.nextMatch;
  }

  return [...(state.value?.matches ?? [])]
    .filter((match) => match.status === 'PLANNED')
    .sort((first, second) => matchSequenceValue(first) - matchSequenceValue(second))[0] ?? null;
});

const standingsRows = computed<OverlayStandingRow[]>(() => {
  if (state.value?.standings?.length) {
    return state.value.standings.map((row, index) => ({
      place: index + 1,
      team: findTeam(row.teamId) ?? {
        id: row.teamId,
        name: row.teamName,
        color: row.teamColor,
        displayOrder: null,
        players: []
      },
      played: row.played,
      wins: row.wins,
      draws: row.draws,
      losses: row.losses,
      goalsFor: row.goalsFor,
      goalsAgainst: row.goalsAgainst,
      goalDifference: row.goalDifference,
      points: row.points
    }));
  }

  const rows = new Map<number, Omit<OverlayStandingRow, 'place'>>();

  for (const team of state.value?.teams ?? []) {
    rows.set(team.id, {
      team,
      played: 0,
      wins: 0,
      draws: 0,
      losses: 0,
      goalsFor: 0,
      goalsAgainst: 0,
      goalDifference: 0,
      points: 0
    });
  }

  for (const match of finishedMatches.value) {
    const teamA = rows.get(match.teamAId);
    const teamB = rows.get(match.teamBId);
    if (!teamA || !teamB) {
      continue;
    }

    applyMatchResult(teamA, match.teamAScore, match.teamBScore);
    applyMatchResult(teamB, match.teamBScore, match.teamAScore);
  }

  return [...rows.values()]
    .map((row) => ({
      ...row,
      goalDifference: row.goalsFor - row.goalsAgainst
    }))
    .sort((first, second) => {
      return second.points - first.points
        || second.goalDifference - first.goalDifference
        || second.goalsFor - first.goalsFor
        || first.team.name.localeCompare(second.team.name, 'ru');
    })
    .map((row, index) => ({
      ...row,
      place: index + 1
    }));
});

onMounted(async () => {
  document.documentElement.classList.add('overlay-root');
  await loadState();
  connectStream();
  clockTimer = window.setInterval(() => {
    now.value = new Date();
  }, 1000);
});

onBeforeUnmount(() => {
  document.documentElement.classList.remove('overlay-root');
  events?.close();
  if (clockTimer) {
    window.clearInterval(clockTimer);
  }
  if (toastTimer) {
    window.clearTimeout(toastTimer);
  }
});

async function loadState() {
  try {
    error.value = '';
    state.value = await api.getOverlayState(sessionIdNumber.value, preferredMatchId.value);
  } catch (loadError) {
    error.value = loadError instanceof Error ? loadError.message : 'Не удалось загрузить overlay';
  }
}

function connectStream() {
  events?.close();
  connectionStatus.value = 'connecting';
  events = new EventSource(api.getOverlayStreamUrl(sessionIdNumber.value));

  events.addEventListener('CONNECTED', handleOverlayEvent);
  events.addEventListener('MATCH_STARTED', handleOverlayEvent);
  events.addEventListener('MATCH_FINISHED', handleOverlayEvent);
  events.addEventListener('MATCH_PAUSED', handleOverlayEvent);
  events.addEventListener('MATCH_RESUMED', handleOverlayEvent);
  events.addEventListener('GOAL_RECORDED', handleOverlayEvent);
  events.addEventListener('GOAL_CANCELLED', handleOverlayEvent);

  events.onopen = () => {
    connectionStatus.value = 'live';
  };
  events.onerror = () => {
    connectionStatus.value = 'offline';
  };
}

function handleOverlayEvent(message: MessageEvent<string>) {
  const payload = JSON.parse(message.data) as OverlayEvent;
  connectionStatus.value = 'live';
  error.value = '';
  if (!preferredMatchId.value || payload.matchId === preferredMatchId.value || payload.type === 'CONNECTED') {
    state.value = payload.type === 'CONNECTED' && preferredMatchId.value
      ? state.value
      : payload.state;
  }

  if (preferredMatchId.value && payload.matchId !== preferredMatchId.value) {
    return;
  }

  if (payload.type === 'GOAL_RECORDED' && payload.event) {
    showGoalToast(payload.event, payload.assistEvent, false);
  }
  if (payload.type === 'GOAL_CANCELLED' && payload.event) {
    showGoalToast(payload.event, null, true);
  }
}

function showGoalToast(event: MatchEvent, assist: MatchEvent | null, cancelled: boolean) {
  goalToast.value = {
    scorer: eventDisplayName(event),
    scorerPhotoUrl: event.playerPhotoUrl,
    assist: assist ? eventDisplayName(assist) : null,
    assistPhotoUrl: assist?.playerPhotoUrl ?? null,
    ownGoal: event.eventType === 'OWN_GOAL',
    cancelled
  };
  if (toastTimer) {
    window.clearTimeout(toastTimer);
  }
  toastTimer = window.setTimeout(() => {
    goalToast.value = null;
  }, cancelled ? 3200 : 5200);
}

function matchSortValue(match: SessionMatch): number {
  const date = match.endedAt ?? match.startedAt ?? match.createdAt;
  const timestamp = date ? new Date(date).getTime() : NaN;
  if (Number.isFinite(timestamp)) {
    return timestamp;
  }
  return matchSequenceValue(match);
}

function matchSequenceValue(match: SessionMatch): number {
  return (match.roundNumber ?? 1) * 10000 + match.matchNumber;
}

function applyMatchResult(
  row: Omit<OverlayStandingRow, 'place'>,
  goalsFor: number,
  goalsAgainst: number
) {
  row.played += 1;
  row.goalsFor += goalsFor;
  row.goalsAgainst += goalsAgainst;

  if (goalsFor > goalsAgainst) {
    row.wins += 1;
    row.points += 3;
  } else if (goalsFor === goalsAgainst) {
    row.draws += 1;
    row.points += 1;
  } else {
    row.losses += 1;
  }
}

function findTeam(teamId: number | null): OverlayTeam | null {
  if (!teamId) {
    return null;
  }
  return state.value?.teams.find((team) => team.id === teamId) ?? null;
}

function visiblePlayers(team: OverlayTeam | null): SessionTeamPlayer[] {
  if (!team) {
    return [];
  }
  return team.players.filter((player) => player.active);
}

function displayPlayerName(player: SessionTeamPlayer): string {
  return formatUsername(player.playerUsername) ?? shortName(player.playerName);
}

function eventDisplayName(event: MatchEvent): string {
  return formatUsername(event.playerUsername) ?? (event.playerName ? shortName(event.playerName) : 'Игрок');
}

function formatUsername(username: string | null | undefined): string | null {
  const value = username?.trim();
  if (!value) {
    return null;
  }
  return value.startsWith('@') ? value : `@${value}`;
}

function teamNameStyle(team: OverlayTeam | null): Record<string, string> {
  return standingTeamNameStyle(team);
}

function standingTeamNameStyle(team: OverlayTeam | null): Record<string, string> {
  return {
    color: standingTeamColor(team),
    '-webkit-text-stroke': '0.8px rgba(0, 0, 0, 0.9)',
    textShadow: '0 2px 0 rgba(0, 0, 0, 0.36), 0 0 10px rgba(255, 255, 255, 0.16)'
  };
}

function standingTeamColor(team: OverlayTeam | null): string {
  const marker = `${team?.name ?? ''} ${team?.color ?? ''}`.toLowerCase();
  if (marker.includes('blue') || marker.includes('син')) {
    return '#6ee7ff';
  }
  if (marker.includes('red') || marker.includes('крас')) {
    return '#ff6f7d';
  }
  if (marker.includes('green') || marker.includes('зелен') || marker.includes('зелён')) {
    return '#6dff9c';
  }
  return lightenTeamColor(team?.color || '#2ef27a');
}

function lightenTeamColor(color: string): string {
  const match = color.trim().match(/^#?([0-9a-f]{6})$/i);
  if (!match) {
    return color;
  }
  const hex = match[1];
  const channels = [0, 2, 4].map((index) => Number.parseInt(hex.slice(index, index + 2), 16));
  const lightened = channels.map((channel) => Math.round(channel + (255 - channel) * 0.46));
  return `#${lightened.map((channel) => channel.toString(16).padStart(2, '0')).join('')}`;
}

function playerCardStyle(team: OverlayTeam | null): Record<string, string> {
  const color = team?.color || '#2ef27a';
  return {
    borderColor: color,
    boxShadow: `inset 0 0 0 1px rgba(255, 255, 255, 0.38), 0 0 0 2px ${color}55, 0 13px 26px rgba(0, 0, 0, 0.32)`
  };
}

function teamLogo(team: OverlayTeam | null): string {
  const marker = `${team?.name ?? ''} ${team?.color ?? ''}`.toLowerCase();
  if (marker.includes('red') || marker.includes('крас')) {
    return '/pozitiv-red.png';
  }
  if (marker.includes('green') || marker.includes('зелен') || marker.includes('зелён')) {
    return '/pozitiv-green.png';
  }
  return '/pozitiv.png';
}

function playerStats(playerId: number): { goals: number; assists: number } {
  const events = state.value?.sessionEvents ?? [];
  return {
    goals: events.filter((event) => (event.eventType === 'GOAL' || event.eventType === 'OWN_GOAL') && event.playerId === playerId).length,
    assists: events.filter((event) => event.eventType === 'ASSIST' && event.playerId === playerId).length
  };
}

function playerStatsLabel(playerId: number): string {
  const stats = playerStats(playerId);
  return [
    stats.goals > 0 ? `${stats.goals} ⚽` : '',
    stats.assists > 0 ? `${stats.assists} 👟` : ''
  ].filter(Boolean).join(' ');
}

function formatEventMinute(event: MatchEvent): string {
  if (event.minuteInMatch == null) {
    return '--';
  }
  if (event.secondInMatch == null) {
    return `${String(event.minuteInMatch).padStart(2, '0')}:00`;
  }
  return `${String(event.minuteInMatch).padStart(2, '0')}:${String(event.secondInMatch).padStart(2, '0')}`;
}

function shortName(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length <= 1) {
    return parts[0] || 'Игрок';
  }
  return `${parts[0]} ${parts[1][0]}.`;
}

function initials(name: string): string {
  return name
    .trim()
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('') || 'P';
}

function positionLabel(position: PlayerPosition | null): string {
  const labels: Record<PlayerPosition, string> = {
    GOALKEEPER: 'ВР',
    DEFENDER: 'ЗЩ',
    MIDFIELDER: 'ПЗ',
    FORWARD: 'НП',
    UNIVERSAL: 'УН'
  };
  return position ? labels[position] : 'ИГР';
}
</script>

<style scoped>
.overlay-page {
  position: relative;
  width: 100vw;
  min-height: 100vh;
  overflow: hidden;
  color: #f7fff9;
  background: transparent;
  font-family: "Segoe UI", "SF Pro Display", system-ui, sans-serif;
  letter-spacing: 0;
}

.overlay-scoreboard,
.overlay-team-panel,
.overlay-intermission,
.overlay-match-card,
.overlay-goal-toast,
.overlay-error {
  background: linear-gradient(135deg, rgba(6, 15, 18, 0.9), rgba(16, 30, 34, 0.78));
  border: 1px solid rgba(255, 255, 255, 0.14);
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(16px);
}

.overlay-scoreboard {
  position: absolute;
  top: 22px;
  left: 22px;
  width: min(450px, calc(100vw - 44px));
  padding: 8px 14px 13px;
  border-radius: 8px;
}

.overlay-scoreboard__match {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  margin-top: 2px;
  padding-bottom: 28px;
}

.overlay-team-name {
  min-width: 0;
  display: inline-flex;
  align-items: center;
  gap: 9px;
  overflow: hidden;
  font-size: clamp(14px, 1.7vw, 21px);
  font-weight: 950;
  text-overflow: ellipsis;
  text-transform: uppercase;
  white-space: nowrap;
}

.overlay-team-name--right {
  justify-content: flex-end;
  text-align: right;
}

.overlay-team-logo {
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  object-fit: contain;
}

.overlay-score-center {
  position: relative;
  display: grid;
  justify-items: center;
}

.overlay-score {
  min-width: 88px;
  display: inline-flex;
  align-items: baseline;
  justify-content: center;
  gap: 8px;
  padding: 4px 10px 6px;
  border: 2px solid rgba(255, 255, 255, 0.72);
  border-radius: 8px;
  color: #ffffff;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.12), rgba(255, 255, 255, 0.03));
  box-shadow: inset 0 0 18px rgba(255, 255, 255, 0.12), 0 0 22px rgba(255, 210, 64, 0.42);
  font-size: clamp(24px, 3vw, 36px);
  font-weight: 950;
  line-height: 0.9;
}

.overlay-score span {
  color: rgba(255, 255, 255, 0.58);
  font-size: 0.66em;
}

.overlay-scoreboard__empty {
  padding: 18px 0 12px;
  color: rgba(247, 255, 249, 0.74);
  font-size: 22px;
  font-weight: 800;
}

.overlay-scoreboard__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: rgba(247, 255, 249, 0.76);
  font-size: 11px;
  font-weight: 850;
  text-transform: uppercase;
}

.overlay-live-row {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.overlay-time-badge {
  position: absolute;
  top: calc(100% + 6px);
  left: 50%;
  transform: translateX(-50%);
  min-width: 72px;
  padding: 2px 9px 3px;
  border: 1.5px solid rgba(255, 255, 255, 0.78);
  border-radius: 6px;
  color: #ffffff;
  background: rgba(255, 255, 255, 0.08);
  box-shadow: inset 0 0 12px rgba(255, 255, 255, 0.1);
  font-size: 15px;
  line-height: 1;
  text-align: center;
}

.overlay-status-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #ff364e;
  box-shadow: 0 0 14px rgba(255, 54, 78, 0.95);
}

.overlay-scoreboard.is-offline .overlay-status-dot {
  background: #ffbf4d;
  box-shadow: 0 0 14px rgba(255, 191, 77, 0.8);
}

.overlay-goal-list {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 88px minmax(0, 1fr);
  gap: 5px 8px;
  margin-top: -18px;
  color: rgba(247, 255, 249, 0.8);
  font-size: 13px;
}

.overlay-goal-list__column {
  grid-column: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.overlay-goal-list__column--right {
  grid-column: 3;
  align-items: flex-end;
  text-align: right;
}

.overlay-goal-list__column p {
  min-width: 0;
  margin: 0;
  display: flex;
  gap: 6px;
}

.overlay-goal-list__column--right p {
  justify-content: flex-end;
}

.overlay-goal-list strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.overlay-lineups-shell {
  position: absolute;
  left: 50%;
  bottom: 24px;
  width: min(1800px, calc(100vw - 48px));
  transform: translateX(-50%);
}

.overlay-lineups {
  display: grid;
  grid-template-columns: minmax(0, 1fr) clamp(110px, 8vw, 150px) minmax(0, 1fr);
  align-items: end;
  justify-content: center;
  column-gap: clamp(18px, 1.6vw, 30px);
}

.overlay-team-panel {
  width: 100%;
  min-width: 0;
  min-height: 112px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  border-radius: 8px;
  padding: 0 12px 8px;
  background: linear-gradient(135deg, rgba(3, 10, 12, 0.94), rgba(12, 23, 26, 0.9));
}

.overlay-lineups__players {
  width: 100%;
  display: flex;
  align-items: flex-end;
  justify-content: flex-end;
  gap: clamp(5px, 0.7vw, 10px);
  min-width: 0;
  margin-top: -88px;
  overflow: visible;
}

.overlay-lineups__players--right {
  justify-content: flex-start;
}

.overlay-lineups__divider {
  width: 129px;
  height: 129px;
  display: grid;
  place-items: center;
  align-self: center;
  filter: drop-shadow(0 0 16px rgba(46, 242, 122, 0.64)) drop-shadow(0 8px 22px rgba(0, 0, 0, 0.42));
  transform: translateY(-20%);
}

.overlay-lineups__divider img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.overlay-player-card {
  container-type: inline-size;
  width: clamp(72px, 8vw, 124px);
  min-width: 0;
  max-width: 124px;
  height: clamp(140px, 14.5vw, 194px);
  flex: 1 1 92px;
  display: grid;
  grid-template-rows: minmax(20px, 26px) minmax(42px, 64px) minmax(20px, auto) minmax(20px, 24px);
  justify-items: center;
  gap: clamp(4px, 0.55vw, 7px);
  padding: clamp(6px, 0.7vw, 9px);
  border: 1px solid rgba(255, 255, 255, 0.82);
  border-radius: 9px;
  color: #101820;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.95), rgba(172, 183, 190, 0.96) 42%, rgba(244, 247, 248, 0.94)),
    radial-gradient(circle at 20% 15%, rgba(255, 255, 255, 0.95), transparent 34%);
  overflow: hidden;
}

.overlay-card-top {
  width: 100%;
  display: flex;
  justify-content: flex-start;
}

.overlay-card-position {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 3px;
  font-size: 11px;
  font-size: clamp(9px, 11cqw, 12px);
  font-weight: 950;
}

.overlay-card-position img {
  width: clamp(14px, 1.6vw, 20px);
  height: clamp(14px, 1.6vw, 20px);
  object-fit: contain;
}

.overlay-player-card__photo {
  width: clamp(42px, 5vw, 64px);
  height: clamp(42px, 5vw, 64px);
  display: grid;
  place-items: center;
  overflow: hidden;
  border: 2px solid rgba(255, 255, 255, 0.78);
  border-radius: 50%;
  color: #061012;
  background: linear-gradient(135deg, #f5fff7, #aeb8be);
  font-size: clamp(13px, 1.4vw, 18px);
  font-weight: 950;
}

.overlay-player-card__photo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.overlay-player-card__name {
  width: 100%;
  min-width: 0;
  overflow: hidden;
  font-size: 13px;
  font-size: clamp(10px, 13cqw, 15px);
  line-height: 1.12;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.overlay-player-card__stats {
  width: 100%;
  min-width: 0;
  min-height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: 1px solid rgba(16, 24, 32, 0.22);
  padding-top: 3px;
  overflow: hidden;
  font-size: 13px;
  font-size: clamp(10px, 12cqw, 15px);
  font-weight: 950;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.overlay-intermission {
  position: absolute;
  left: 50%;
  top: 50%;
  width: min(1060px, calc(100vw - 56px));
  display: grid;
  grid-template-columns: minmax(0, 1.22fr) minmax(0, 0.94fr);
  gap: 22px;
  padding: 24px;
  border-radius: 10px;
  transform: translate(-50%, -50%);
}

.overlay-standings,
.overlay-intermission__side {
  min-width: 0;
}

.overlay-standings h2,
.overlay-match-card h2 {
  margin: 0;
  color: rgba(255, 255, 255, 0.94);
  font-size: 20px;
  font-weight: 950;
  line-height: 1;
  text-transform: uppercase;
}

.overlay-standings__head,
.overlay-standings__row {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr) 38px 38px 38px 38px 66px 44px;
  align-items: center;
  gap: 10px;
}

.overlay-standings__head {
  margin-top: 18px;
  padding: 0 12px 10px;
  color: rgba(255, 255, 255, 0.62);
  font-size: 12px;
  font-weight: 900;
  text-transform: uppercase;
}

.overlay-standings__body {
  display: grid;
  gap: 7px;
}

.overlay-standings__row {
  min-height: 54px;
  padding: 8px 12px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.07);
  color: rgba(255, 255, 255, 0.9);
  font-size: 16px;
  font-weight: 900;
}

.overlay-standings__place {
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  color: #ffffff;
}

.overlay-standings__team {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.overlay-standings__team img {
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  object-fit: contain;
}

.overlay-standings__team strong {
  min-width: 0;
  overflow: hidden;
  font-size: 17px;
  font-weight: 950;
  text-overflow: ellipsis;
  text-transform: uppercase;
  white-space: nowrap;
}

.overlay-intermission__side {
  display: grid;
  gap: 18px;
}

.overlay-match-card {
  min-width: 0;
  border-radius: 10px;
  padding: 20px;
}

.overlay-result-match {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 16px;
  margin-top: 22px;
}

.overlay-result-team,
.overlay-next-team {
  min-width: 0;
  display: grid;
  justify-items: center;
  gap: 8px;
  text-align: center;
}

.overlay-result-team img,
.overlay-next-team img {
  width: 54px;
  height: 54px;
  object-fit: contain;
}

.overlay-result-team strong,
.overlay-next-team strong {
  max-width: 100%;
  overflow: hidden;
  font-size: 16px;
  font-weight: 950;
  text-overflow: ellipsis;
  text-transform: uppercase;
  white-space: nowrap;
}

.overlay-result-score {
  min-width: 104px;
  padding: 8px 16px 10px;
  border: 2px solid rgba(255, 255, 255, 0.74);
  border-radius: 8px;
  color: #ffffff;
  background: rgba(255, 255, 255, 0.08);
  box-shadow: inset 0 0 16px rgba(255, 255, 255, 0.1), 0 0 20px rgba(255, 210, 64, 0.34);
  font-size: 38px;
  font-weight: 950;
  line-height: 0.9;
  text-align: center;
}

.overlay-result-score span {
  padding: 0 6px;
  color: rgba(255, 255, 255, 0.58);
  font-size: 0.66em;
}

.overlay-result-goals {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 104px minmax(0, 1fr);
  gap: 8px 16px;
  margin-top: 12px;
  color: rgba(255, 255, 255, 0.84);
  font-size: 12px;
  font-weight: 850;
}

.overlay-result-goals__column {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.overlay-result-goals__column--right {
  grid-column: 3;
  align-items: flex-end;
  text-align: right;
}

.overlay-result-goals__column p {
  max-width: 100%;
  margin: 0;
  display: flex;
  gap: 5px;
}

.overlay-result-goals__column--right p {
  justify-content: flex-end;
}

.overlay-result-goals strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.overlay-next-match {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 90px minmax(0, 1fr);
  align-items: center;
  gap: 16px;
  margin-top: 20px;
}

.overlay-next-vs {
  width: 90px;
  height: 90px;
  object-fit: contain;
  filter: drop-shadow(0 0 16px rgba(255, 210, 64, 0.52));
}

.overlay-empty-note {
  margin: 18px 0 0;
  color: rgba(255, 255, 255, 0.68);
  font-size: 13px;
  font-weight: 800;
}

.overlay-goal-toast {
  position: absolute;
  left: 50%;
  top: 50%;
  width: min(760px, calc(100vw - 56px));
  min-height: 260px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 92px minmax(0, 1fr);
  align-items: center;
  gap: 24px;
  padding: 28px 34px;
  border-radius: 8px;
  text-align: center;
  transform: translate(-50%, -50%);
}

.overlay-goal-toast:not(.has-assist) {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 12px;
}

.overlay-goal-toast p,
.overlay-goal-toast strong,
.overlay-goal-toast span {
  display: block;
}

.overlay-goal-toast p {
  margin: 0;
  color: #ffffff;
  font-size: clamp(36px, 6vw, 76px);
  font-weight: 950;
  line-height: 1;
  text-transform: uppercase;
  text-shadow: 0 0 22px rgba(255, 210, 64, 0.42), 0 4px 20px rgba(0, 0, 0, 0.58);
}

.overlay-goal-toast.is-cancelled p {
  color: #ff3f55;
  text-shadow: 0 0 20px rgba(255, 63, 85, 0.62), 0 4px 20px rgba(0, 0, 0, 0.58);
}

.overlay-goal-toast strong {
  min-width: 0;
  overflow: hidden;
  font-size: clamp(20px, 2.6vw, 34px);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.overlay-goal-toast span {
  color: #ffffff;
  font-size: 18px;
  font-weight: 950;
  letter-spacing: 0;
  text-transform: uppercase;
}

.overlay-goal-person {
  min-width: 0;
  display: grid;
  justify-items: center;
  gap: 10px;
}

.overlay-goal-person--assist span {
  color: rgba(255, 255, 255, 0.78);
}

.overlay-goal-photo {
  width: 128px;
  height: 128px;
  display: grid;
  place-items: center;
  overflow: hidden;
  border: 3px solid rgba(255, 255, 255, 0.82);
  border-radius: 50%;
  color: #071014;
  background: linear-gradient(145deg, #ffffff, #b7c0c6);
  box-shadow: 0 0 0 3px rgba(255, 210, 64, 0.42), 0 16px 28px rgba(0, 0, 0, 0.36);
  font-size: 34px;
  font-weight: 950;
}

.overlay-goal-toast.is-cancelled .overlay-goal-photo {
  box-shadow: 0 0 0 3px rgba(255, 63, 85, 0.55), 0 16px 28px rgba(0, 0, 0, 0.36);
}

.overlay-goal-photo--solo {
  width: 148px;
  height: 148px;
}

.overlay-goal-photo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.overlay-goal-arrow {
  color: #ffffff;
  font-size: 68px;
  font-weight: 950;
  text-shadow: 0 0 22px rgba(255, 210, 64, 0.72), 0 4px 18px rgba(0, 0, 0, 0.54);
}

.overlay-error {
  position: absolute;
  right: 28px;
  top: 28px;
  max-width: 420px;
  padding: 12px 14px;
  border-color: rgba(255, 93, 93, 0.44);
  color: #ffd5d5;
  border-radius: 8px;
}

.goal-toast-enter-active,
.goal-toast-leave-active {
  transition: opacity 0.28s ease, transform 0.28s ease;
}

.goal-toast-enter-from,
.goal-toast-leave-to {
  opacity: 0;
  transform: translate(-50%, -44%) scale(0.96);
}

@media (max-width: 900px) {
  .overlay-scoreboard,
  .overlay-lineups-shell,
  .overlay-intermission,
  .overlay-error {
    position: static;
    width: auto;
    margin: 12px;
  }

  .overlay-page {
    min-height: 100vh;
    padding: 1px 0 12px;
    overflow-y: auto;
  }

  .overlay-lineups {
    grid-template-columns: minmax(0, 1fr);
  }

  .overlay-intermission {
    grid-template-columns: minmax(0, 1fr);
    transform: none;
  }

  .overlay-lineups-shell {
    transform: none;
  }

  .overlay-lineups__players,
  .overlay-lineups__players--right {
    justify-content: flex-start;
    overflow-x: auto;
  }

  .overlay-lineups__divider {
    display: none;
  }

  .overlay-team-caption--right {
    text-align: left;
  }
}
</style>
