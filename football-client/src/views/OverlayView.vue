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
        <div class="overlay-goal-list__column" :class="{ 'is-two-columns': leftGoalRows.length > 3 }">
          <p v-for="goal in leftGoalRows" :key="goal.id">
            <span>{{ goal.minute }}</span>
            <strong>{{ goal.name }}</strong>
          </p>
        </div>
        <div class="overlay-goal-list__column overlay-goal-list__column--right" :class="{ 'is-two-columns': rightGoalRows.length > 3 }">
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
              <strong class="overlay-player-card__name">
                <span v-for="nameLine in playerNameLines(player)" :key="nameLine" class="overlay-player-card__name-line">{{ nameLine }}</span>
              </strong>
              <div class="overlay-player-card__photo">
                <PlayerAvatar :sources="[player.photoUrl, player.telegramPhotoUrl]" :initials="initials(displayPlayerName(player))" :alt="displayPlayerName(player)" />
              </div>
              <div v-if="playerStats(player.playerId).goals > 0" class="overlay-player-card__stat-badge overlay-player-card__stat-badge--goals">
                <span>{{ playerStats(player.playerId).goals }}</span>
                <span>⚽</span>
              </div>
              <div v-if="playerStats(player.playerId).assists > 0" class="overlay-player-card__stat-badge overlay-player-card__stat-badge--assists">
                <span>{{ playerStats(player.playerId).assists }}</span>
                <span>👟</span>
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
              <strong class="overlay-player-card__name">
                <span v-for="nameLine in playerNameLines(player)" :key="nameLine" class="overlay-player-card__name-line">{{ nameLine }}</span>
              </strong>
              <div class="overlay-player-card__photo">
                <PlayerAvatar :sources="[player.photoUrl, player.telegramPhotoUrl]" :initials="initials(displayPlayerName(player))" :alt="displayPlayerName(player)" />
              </div>
              <div v-if="playerStats(player.playerId).goals > 0" class="overlay-player-card__stat-badge overlay-player-card__stat-badge--goals">
                <span>{{ playerStats(player.playerId).goals }}</span>
                <span>⚽</span>
              </div>
              <div v-if="playerStats(player.playerId).assists > 0" class="overlay-player-card__stat-badge overlay-player-card__stat-badge--assists">
                <span>{{ playerStats(player.playerId).assists }}</span>
                <span>👟</span>
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
        <div v-if="standingsRows.length && (topScorers.length || topAssistants.length)" class="overlay-leaders">
          <section class="overlay-leader-board">
            <h3>Топ бомбардиров</h3>
            <div class="overlay-leader-list">
              <article v-for="leader in topScorers" :key="`goal-${leader.playerId}`" class="overlay-leader">
                <strong class="overlay-leader__name">{{ leader.displayName }}</strong>
                <div class="overlay-leader__row">
                  <div class="overlay-leader__photo" :style="leaderPhotoStyle(leader.team)">
                    <PlayerAvatar :sources="[leader.photoUrl, leader.telegramPhotoUrl]" :initials="initials(leader.displayName)" :alt="leader.displayName" />
                  </div>
                  <span class="overlay-leader__count">{{ leader.count }} ⚽</span>
                </div>
              </article>
            </div>
          </section>

          <section class="overlay-leader-board">
            <h3>Топ ассистов</h3>
            <div class="overlay-leader-list">
              <article v-for="leader in topAssistants" :key="`assist-${leader.playerId}`" class="overlay-leader">
                <strong class="overlay-leader__name">{{ leader.displayName }}</strong>
                <div class="overlay-leader__row">
                  <div class="overlay-leader__photo" :style="leaderPhotoStyle(leader.team)">
                    <PlayerAvatar :sources="[leader.photoUrl, leader.telegramPhotoUrl]" :initials="initials(leader.displayName)" :alt="leader.displayName" />
                  </div>
                  <span class="overlay-leader__count">{{ leader.count }} 👟</span>
                </div>
              </article>
            </div>
          </section>
        </div>
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
              <PlayerAvatar :sources="[goalToast.assistPhotoUrl, goalToast.assistTelegramPhotoUrl]" :initials="initials(goalToast.assist)" :alt="goalToast.assist" />
            </div>
            <strong>{{ goalToast.assist }}</strong>
          </div>

          <div class="overlay-goal-arrow">→</div>

          <div class="overlay-goal-person">
            <span>{{ goalToast.ownGoal ? 'АВТОГОЛ' : 'ГОЛ' }}</span>
            <div class="overlay-goal-photo">
              <PlayerAvatar :sources="[goalToast.scorerPhotoUrl, goalToast.scorerTelegramPhotoUrl]" :initials="initials(goalToast.scorer)" :alt="goalToast.scorer" />
            </div>
            <strong>{{ goalToast.scorer }}</strong>
          </div>
        </template>

        <template v-else>
          <p>{{ goalToast.cancelled ? 'Гол отменен' : goalToast.ownGoal ? 'АВТОГОЛ' : 'ГОЛ' }}</p>
          <div class="overlay-goal-photo overlay-goal-photo--solo">
            <PlayerAvatar :sources="[goalToast.scorerPhotoUrl, goalToast.scorerTelegramPhotoUrl]" :initials="initials(goalToast.scorer)" :alt="goalToast.scorer" />
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
import type { MatchEvent, OverlayEvent, OverlayState, OverlayTeam, SessionMatch, SessionTeamPlayer } from '../types';
import PlayerAvatar from '../components/PlayerAvatar.vue';

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

interface OverlayLeaderRow {
  playerId: number;
  displayName: string;
  photoUrl: string | null;
  telegramPhotoUrl: string | null;
  team: OverlayTeam | null;
  count: number;
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
  scorerTelegramPhotoUrl: string | null;
  assist: string | null;
  assistPhotoUrl: string | null;
  assistTelegramPhotoUrl: string | null;
  ownGoal: boolean;
  cancelled: boolean;
} | null>(null);

let events: EventSource | null = null;
let clockTimer: number | null = null;
let statePollTimer: number | null = null;
let toastTimer: number | null = null;
let stateLoading = false;
let lastSseEventAt = 0;
const statePollIntervalMs = 20_000;

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

const topScorers = computed(() => buildTopLeaders('GOAL'));
const topAssistants = computed(() => buildTopLeaders('ASSIST'));

onMounted(async () => {
  document.documentElement.classList.add('overlay-root');
  await loadState();
  connectStream();
  clockTimer = window.setInterval(() => {
    now.value = new Date();
  }, 1000);
  statePollTimer = window.setInterval(() => {
    void pollStateIfSseIsStale();
  }, statePollIntervalMs);
});

onBeforeUnmount(() => {
  document.documentElement.classList.remove('overlay-root');
  events?.close();
  if (clockTimer) {
    window.clearInterval(clockTimer);
  }
  if (statePollTimer) {
    window.clearInterval(statePollTimer);
  }
  if (toastTimer) {
    window.clearTimeout(toastTimer);
  }
});

async function loadState() {
  if (stateLoading) {
    return;
  }

  stateLoading = true;
  try {
    error.value = '';
    state.value = await api.getOverlayState(sessionIdNumber.value, preferredMatchId.value);
  } catch (loadError) {
    error.value = loadError instanceof Error ? loadError.message : 'Не удалось загрузить overlay';
  } finally {
    stateLoading = false;
  }
}

async function pollStateIfSseIsStale() {
  if (Date.now() - lastSseEventAt < statePollIntervalMs) {
    return;
  }

  await loadState();
}

function connectStream() {
  events?.close();
  connectionStatus.value = 'connecting';
  events = new EventSource(api.getOverlayStreamUrl(sessionIdNumber.value));

  events.addEventListener('CONNECTED', handleOverlayEvent);
  events.addEventListener('STATE_REFRESH', handleOverlayEvent);
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
  lastSseEventAt = Date.now();
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
    scorerTelegramPhotoUrl: event.playerTelegramPhotoUrl,
    assist: assist ? eventDisplayName(assist) : null,
    assistPhotoUrl: assist?.playerPhotoUrl ?? null,
    assistTelegramPhotoUrl: assist?.playerTelegramPhotoUrl ?? null,
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
  return player.playerDisplayName?.trim() || formatUsername(player.playerUsername) || shortName(player.playerName);
}

function playerNameLines(player: SessionTeamPlayer): string[] {
  const name = displayPlayerName(player);
  const words = name.split(/\s+/).filter(Boolean);
  return words.length === 2 ? words : [name];
}

function buildTopLeaders(eventType: 'GOAL' | 'ASSIST'): OverlayLeaderRow[] {
  const leaders = new Map<number, OverlayLeaderRow>();

  for (const event of state.value?.sessionEvents ?? []) {
    if (event.eventType !== eventType || !event.playerId) {
      continue;
    }

    const teamPlayer = findTeamPlayer(event.playerId);
    const player = teamPlayer?.player ?? null;
    const team = teamPlayer?.team ?? findTeam(event.teamId);
    const displayName = player ? displayPlayerName(player) : eventDisplayName(event);
    const existing = leaders.get(event.playerId);

    if (existing) {
      existing.count += 1;
      continue;
    }

    leaders.set(event.playerId, {
      playerId: event.playerId,
      displayName,
      photoUrl: player?.photoUrl ?? event.playerPhotoUrl,
      telegramPhotoUrl: player?.telegramPhotoUrl ?? event.playerTelegramPhotoUrl,
      team,
      count: 1
    });
  }

  return [...leaders.values()]
    .sort((first, second) => {
      return second.count - first.count
        || first.displayName.localeCompare(second.displayName, 'ru');
    })
    .slice(0, 2);
}

function findTeamPlayer(playerId: number): { team: OverlayTeam; player: SessionTeamPlayer } | null {
  for (const team of state.value?.teams ?? []) {
    const player = team.players.find((candidate) => candidate.playerId === playerId);
    if (player) {
      return { team, player };
    }
  }
  return null;
}

function eventDisplayName(event: MatchEvent): string {
  return event.playerDisplayName?.trim() || formatUsername(event.playerUsername) || (event.playerName ? shortName(event.playerName) : 'Игрок');
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
    fontWeight: '950'
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

function playerBorderColor(team: OverlayTeam | null): string {
  const marker = `${team?.name ?? ''} ${team?.color ?? ''}`.toLowerCase();
  if (marker.includes('blue') || marker.includes('син')) {
    return '#1557ff';
  }
  if (marker.includes('red') || marker.includes('крас')) {
    return '#e31b35';
  }
  if (marker.includes('green') || marker.includes('зелен') || marker.includes('зелён')) {
    return '#087a2f';
  }
  return team?.color || '#087a2f';
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
  const color = playerBorderColor(team);
  return {
    '--team-color': color,
    '--team-color-soft': `${color}55`
  };
}

function leaderPhotoStyle(team: OverlayTeam | null): Record<string, string> {
  return playerCardStyle(team);
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
  left: 50%;
  width: min(450px, calc(100vw - 44px));
  padding: 8px 14px 13px;
  border-radius: 8px;
  opacity: 0.9;
  transform: translateX(-50%) scale(1.25);
  transform-origin: top center;
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
  min-width: 84px;
  display: inline-flex;
  align-items: baseline;
  justify-content: center;
  gap: 7px;
  padding: 4px 10px 5px;
  border: 2px solid rgba(255, 255, 255, 0.72);
  border-radius: 8px;
  color: #ffffff;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.12), rgba(255, 255, 255, 0.03));
  box-shadow: inset 0 0 18px rgba(255, 255, 255, 0.12), 0 0 22px rgba(255, 210, 64, 0.42);
  font-size: clamp(24px, 2.64vw, 30px);
  font-weight: 950;
  line-height: 0.92;
  transform: translateY(-8px) scale(1.29);
  transform-origin: bottom center;
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
  min-width: 101px;
  padding: 3px 13px 4px;
  border: 1.5px solid rgba(255, 255, 255, 0.78);
  border-radius: 6px;
  color: #ffffff;
  background: rgba(255, 255, 255, 0.08);
  box-shadow: inset 0 0 12px rgba(255, 255, 255, 0.1);
  font-size: 21px;
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
  gap: 3px 8px;
  margin-top: -20px;
  color: rgba(247, 255, 249, 0.8);
  font-size: 12px;
}

.overlay-goal-list__column {
  grid-column: 1;
  min-width: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 2px 8px;
}

.overlay-goal-list__column.is-two-columns {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.overlay-goal-list__column--right {
  grid-column: 3;
  align-items: start;
  justify-items: end;
  text-align: right;
}

.overlay-goal-list__column p {
  min-width: 0;
  margin: 0;
  display: flex;
  gap: 4px;
  line-height: 1.08;
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
  width: min(1920px, calc(100vw - 16px));
  transform: translateX(-50%);
}

.overlay-lineups {
  display: grid;
  grid-template-columns: minmax(0, 1fr) clamp(82px, 6vw, 118px) minmax(0, 1fr);
  align-items: end;
  justify-content: center;
  column-gap: clamp(8px, 0.8vw, 18px);
}

.overlay-team-panel {
  container-type: inline-size;
  width: 100%;
  min-width: 0;
  min-height: 112px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  border: 0;
  border-radius: 8px;
  padding: 0 6px 8px;
  background: transparent;
  box-shadow: none;
  backdrop-filter: none;
}

.overlay-lineups__players {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(clamp(78px, 10cqw, 124px), 1fr));
  align-items: flex-end;
  justify-items: center;
  gap: clamp(2px, 0.45cqw, 7px);
  min-width: 0;
  margin-top: -88px;
  overflow: visible;
}

.overlay-lineups__players--right {
  justify-content: stretch;
}

.overlay-lineups__divider {
  width: clamp(82px, 6vw, 118px);
  height: clamp(82px, 6vw, 118px);
  display: grid;
  place-items: center;
  align-self: center;
  filter: drop-shadow(0 0 16px rgba(46, 242, 122, 0.64)) drop-shadow(0 8px 22px rgba(0, 0, 0, 0.42));
  transform: translateY(-12%);
}

.overlay-lineups__divider img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.overlay-player-card {
  container-type: inline-size;
  width: 100%;
  min-width: 0;
  max-width: clamp(142px, 26cqw, 270px);
  aspect-ratio: 1 / 1.08;
  position: relative;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  overflow: visible;
}

.overlay-player-card__photo {
  width: clamp(86px, 98cqw, 218px);
  height: clamp(86px, 98cqw, 218px);
  position: relative;
  display: grid;
  place-items: center;
  overflow: hidden;
  border: clamp(3px, 5.5cqw, 8px) solid var(--team-color, #2ef27a);
  border-radius: 50%;
  opacity: 0.95;
  color: #102025;
  background: radial-gradient(circle at 30% 20%, #ffffff, #b7c4c9);
  box-shadow:
    0 0 20px var(--team-color-soft, rgba(46, 242, 122, 0.34)),
    0 14px 28px rgba(0, 0, 0, 0.38);
  font-size: clamp(20px, 24cqw, 36px);
  font-weight: 950;
}

.overlay-player-card__photo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.overlay-player-card__name {
  width: min(118%, 240px);
  position: absolute;
  left: 50%;
  bottom: clamp(83px, 104cqw, 232px);
  z-index: 2;
  display: -webkit-box;
  overflow: hidden;
  color: #ffffff;
  font-size: clamp(15px, 17cqw, 28px);
  font-weight: 950;
  line-height: 0.98;
  text-align: center;
  text-wrap: balance;
  overflow-wrap: anywhere;
  letter-spacing: 0;
  text-shadow:
    -1px -1px 0 #000,
    1px -1px 0 #000,
    -1px 1px 0 #000,
    1px 1px 0 #000,
    0 3px 8px rgba(0, 0, 0, 0.55);
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  -webkit-text-stroke: 0.25px rgba(0, 0, 0, 0.82);
  transform: translateX(-50%);
}

.overlay-player-card__name-line {
  display: block;
}

.overlay-player-card__stat-badge {
  min-width: clamp(40px, 42cqw, 76px);
  min-height: clamp(28px, 33cqw, 54px);
  position: absolute;
  bottom: clamp(0px, 3cqw, 5px);
  z-index: 4;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: clamp(2px, 4cqw, 7px);
  border-radius: 999px;
  padding: clamp(2px, 3cqw, 5px) clamp(5px, 7cqw, 11px);
  color: #ffffff;
  background: rgba(5, 12, 16, 0.82);
  box-shadow:
    0 0 0 1px rgba(255, 255, 255, 0.22),
    0 0 12px var(--team-color-soft, rgba(46, 242, 122, 0.34)),
    0 6px 14px rgba(0, 0, 0, 0.36);
  font-size: clamp(17px, 21cqw, 34px);
  font-weight: 950;
  line-height: 1;
  white-space: nowrap;
}

.overlay-player-card__stat-badge--goals {
  left: clamp(0px, 4cqw, 8px);
  transform: translate(-12%, 50%);
}

.overlay-player-card__stat-badge--assists {
  right: clamp(0px, 4cqw, 8px);
  transform: translate(12%, 50%);
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
  opacity: 0.8;
  transform: translate(-50%, -50%);
}

.overlay-standings,
.overlay-intermission__side {
  min-width: 0;
}

.overlay-standings {
  overflow: hidden;
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

.overlay-leaders {
  min-width: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
  padding-top: 12px;
  overflow: hidden;
  border-top: 1px solid rgba(255, 255, 255, 0.14);
}

.overlay-leader-board {
  min-width: 0;
  overflow: hidden;
}

.overlay-leader-board h3 {
  margin: 0 0 8px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 13px;
  font-weight: 950;
  line-height: 1;
  text-transform: uppercase;
}

.overlay-leader-list {
  display: grid;
  gap: 8px;
}

.overlay-leader {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.overlay-leader__name {
  min-width: 0;
  overflow: hidden;
  color: #ffffff;
  font-size: 13px;
  font-weight: 950;
  line-height: 1;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-shadow: 0 2px 6px rgba(0, 0, 0, 0.62);
}

.overlay-leader__row {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.overlay-leader__photo {
  width: 44px;
  height: 44px;
  flex: 0 0 auto;
  overflow: hidden;
  border: 3px solid var(--team-color, #087a2f);
  border-radius: 50%;
  opacity: 0.95;
  background: radial-gradient(circle at 30% 20%, #ffffff, #b7c4c9);
  box-shadow:
    0 0 12px var(--team-color-soft, rgba(46, 242, 122, 0.34)),
    0 8px 16px rgba(0, 0, 0, 0.28);
}

.overlay-leader__photo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.overlay-leader__count {
  min-width: 0;
  overflow: hidden;
  color: #ffffff;
  font-size: 18px;
  font-weight: 950;
  line-height: 1;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-shadow: 0 2px 7px rgba(0, 0, 0, 0.68);
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

  .overlay-scoreboard {
    transform: none;
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
