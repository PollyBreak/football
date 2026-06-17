<template>
  <section class="stack" v-if="match">
    <RouterLink class="ghost-button back-button" :to="`/sessions/${sessionId}`">Назад к сессии</RouterLink>

    <div class="card stack-sm" :class="matchCardStatusClass(match.status)">
      <div class="section-header">
        <div>
          <p class="eyebrow">Матч #{{ match.matchNumber }}</p>
          <h2 class="section-title">{{ match.teamAName }} против {{ match.teamBName }}</h2>
        </div>
        <span class="status-pill" :class="matchStatusClass(match.status)">{{ matchStatusLabel(match.status) }}</span>
      </div>

      <div class="match-scoreboard" :class="{ 'is-overtime': matchIsOvertime }">
        <div class="scoreboard-team">
          <strong>{{ match.teamAScore }}</strong>
          <div v-if="teamAGoals.length" class="scoreboard-goals">
            <p v-for="goal in teamAGoals" :key="goal.id">
              <span v-if="goal.timeLabel" class="scoreboard-goal-time">{{ goal.timeLabel }}</span>
              <img v-if="goal.playerPhotoUrl" :src="goal.playerPhotoUrl" alt="Фото игрока" class="scoreboard-goal-avatar" />
              <button
                v-if="goal.playerId"
                type="button"
                class="player-link-button scoreboard-goal-name"
                @click="openPlayerProfile(goal.playerId)"
              >
                {{ goal.label }}
              </button>
              <span v-else class="scoreboard-goal-name">{{ goal.label }}</span>
            </p>
          </div>
        </div>
        <div class="scoreboard-center">
          <strong>:</strong>
          <span>{{ elapsedLabel }}</span>
        </div>
        <div class="scoreboard-team">
          <strong>{{ match.teamBScore }}</strong>
          <div v-if="teamBGoals.length" class="scoreboard-goals">
            <p v-for="goal in teamBGoals" :key="goal.id">
              <span v-if="goal.timeLabel" class="scoreboard-goal-time">{{ goal.timeLabel }}</span>
              <img v-if="goal.playerPhotoUrl" :src="goal.playerPhotoUrl" alt="Фото игрока" class="scoreboard-goal-avatar" />
              <button
                v-if="goal.playerId"
                type="button"
                class="player-link-button scoreboard-goal-name"
                @click="openPlayerProfile(goal.playerId)"
              >
                {{ goal.label }}
              </button>
              <span v-else class="scoreboard-goal-name">{{ goal.label }}</span>
            </p>
          </div>
        </div>
      </div>

      <div class="button-row">
        <button v-if="!sessionIsFinished" class="ghost-button" @click="startMatch" :disabled="match.status !== 'PLANNED'">Начать</button>
        <button v-if="!sessionIsFinished" class="ghost-button" :class="{ 'is-danger': matchIsOvertime }" @click="finishMatch" :disabled="match.status === 'FINISHED'">Завершить</button>
        <button class="ghost-button" @click="loadMatch">Обновить</button>
      </div>
    </div>

    <div v-if="!sessionIsFinished" class="card stack-sm">
      <div class="section-header">
        <h3 class="section-title">Гол и передача</h3>
      </div>
      <div class="grid-form">
        <select v-model.number="goalForm.teamId" class="input" :disabled="sessionIsFinished">
          <option :value="match.teamAId">{{ match.teamAName }}</option>
          <option :value="match.teamBId">{{ match.teamBName }}</option>
        </select>
        <label class="field-label field-label--checkbox">
          <span>Автогол</span>
          <input v-model="goalForm.ownGoal" type="checkbox" :disabled="sessionIsFinished" />
        </label>
        <select v-model.number="goalForm.scorerPlayerId" class="input" :disabled="sessionIsFinished">
          <option :value="undefined">Автор гола</option>
          <option v-for="player in scorerOptions" :key="player.playerId" :value="player.playerId">
            {{ player.playerName }}
          </option>
        </select>
        <select v-if="!goalForm.ownGoal" v-model.number="goalForm.assistPlayerId" class="input" :disabled="sessionIsFinished">
          <option :value="undefined">Без голевой передачи</option>
          <option v-for="player in selectedTeamPlayers" :key="`assist-${player.playerId}`" :value="player.playerId">
            {{ player.playerName }}
          </option>
        </select>
        <button class="primary-button" @click="addGoal" :disabled="sessionIsFinished || !goalForm.teamId || !goalForm.scorerPlayerId">Добавить гол</button>
      </div>
    </div>

    <div class="card stack-sm">
      <div class="section-header">
        <h3 class="section-title">События матча</h3>
        <button class="ghost-button" @click="loadEvents">Обновить</button>
      </div>
      <p v-if="!events.length" class="muted">Пока нет событий.</p>
      <div v-else class="list">
        <article v-for="event in events" :key="event.id" class="list-item">
          <div>
            <strong>{{ matchEventLabel(event.eventType) }}</strong>
            <p class="muted">
              <span v-if="eventTimeLabel(event)" class="event-time-label">{{ eventTimeLabel(event) }}</span>
              <button
                v-if="event.playerId"
                type="button"
                class="player-link-button"
                @click="openPlayerProfile(event.playerId)"
              >
                {{ ownGoalEventLabel(event) }}
              </button>
              <span v-else>{{ ownGoalEventLabel(event) }}</span>
              <span> • {{ event.teamName || 'Команда не указана' }}</span>
              <span v-if="event.relatedPlayerName">
                • передача:
                <button
                  v-if="event.relatedPlayerId"
                  type="button"
                  class="player-link-button"
                  @click="openPlayerProfile(event.relatedPlayerId)"
                >
                  {{ event.relatedPlayerName }}
                </button>
                <span v-else>{{ event.relatedPlayerName }}</span>
              </span>
            </p>
          </div>
          <button v-if="!sessionIsFinished" class="ghost-button" @click="deleteEvent(event.id)">Удалить</button>
        </article>
      </div>
    </div>

    <p v-if="error" class="error-text">{{ error }}</p>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { api } from '../lib/api';
import { matchEventLabel, matchStatusLabel } from '../lib/labels';
import type { GameSession, MatchEvent, SessionMatch, SessionTeamPlayer } from '../types';

const props = defineProps<{ sessionId: string; matchId: string }>();
const router = useRouter();

const session = ref<GameSession | null>(null);
const match = ref<SessionMatch | null>(null);
const events = ref<MatchEvent[]>([]);
const teamPlayers = ref<Record<number, SessionTeamPlayer[]>>({});
const error = ref('');
const now = ref(Date.now());
const localStartedAt = ref<number | null>(null);
let timerId: number | undefined;

const sessionIdNumber = computed(() => Number(props.sessionId));
const matchIdNumber = computed(() => Number(props.matchId));
const matchStartStorageKey = computed(() => `football-match-start-${matchIdNumber.value}`);
const sessionIsFinished = computed(() => session.value?.status === 'FINISHED');

const goalForm = reactive({
  teamId: undefined as number | undefined,
  scorerPlayerId: undefined as number | undefined,
  ownGoal: false,
  assistPlayerId: undefined as number | undefined
});

const selectedTeamPlayers = computed(() => {
  if (!goalForm.teamId) return [];
  return teamPlayers.value[goalForm.teamId] ?? [];
});
const oppositeTeamPlayers = computed(() => {
  if (!match.value || !goalForm.teamId) return [];
  const oppositeTeamId = goalForm.teamId === match.value.teamAId ? match.value.teamBId : match.value.teamAId;
  return teamPlayers.value[oppositeTeamId] ?? [];
});
const scorerOptions = computed(() => goalForm.ownGoal ? oppositeTeamPlayers.value : selectedTeamPlayers.value);

const elapsedSeconds = computed(() => {
  if (!match.value?.startedAt) {
    return 0;
  }

  const startedAt = localStartedAt.value ?? new Date(match.value.startedAt).getTime();
  const endedAt = match.value.endedAt ? new Date(match.value.endedAt).getTime() : now.value;
  return Math.max(0, Math.floor((endedAt - startedAt) / 1000));
});

const elapsedLabel = computed(() => {
  const minutes = Math.floor(elapsedSeconds.value / 60).toString().padStart(2, '0');
  const seconds = (elapsedSeconds.value % 60).toString().padStart(2, '0');
  return `${minutes}:${seconds}`;
});

const matchDurationSeconds = computed(() => {
  return (match.value?.plannedDurationMinutes ?? session.value?.plannedMatchDurationMinutes ?? 0) * 60;
});
const matchIsOvertime = computed(() => {
  return matchDurationSeconds.value > 0 && elapsedSeconds.value > matchDurationSeconds.value;
});
const goalSummaries = computed(() => {
  const assistsByGoalId = new Map<number, MatchEvent>();
  events.value
    .filter((event) => event.eventType === 'ASSIST' && event.linkedEventId)
    .forEach((event) => assistsByGoalId.set(event.linkedEventId as number, event));

  return events.value
    .filter((event) => (event.eventType === 'GOAL' || event.eventType === 'OWN_GOAL') && event.teamId)
    .map((goal) => {
      const assist = assistsByGoalId.get(goal.id);
      const scorer = shortPlayerName(goal.playerName) ?? 'Игрок';
      const assistName = shortPlayerName(assist?.playerName ?? null);
      const ownGoalMarker = goal.eventType === 'OWN_GOAL' ? ' (А)' : '';
      return {
        id: goal.id,
        teamId: goal.teamId as number,
        playerId: goal.playerId,
        timeLabel: goalTimeLabel(goal),
        playerPhotoUrl: goal.playerPhotoUrl,
        label: goal.eventType === 'OWN_GOAL'
          ? `${scorer}${ownGoalMarker}`
          : assistName ? `${scorer} (${assistName})` : scorer
      };
    });
});
const teamAGoals = computed(() => match.value ? goalSummaries.value.filter((goal) => goal.teamId === match.value?.teamAId) : []);
const teamBGoals = computed(() => match.value ? goalSummaries.value.filter((goal) => goal.teamId === match.value?.teamBId) : []);

function shortPlayerName(name: string | null): string | null {
  return name?.trim().split(/\s+/)[0] || null;
}

function goalTimeLabel(goal: MatchEvent): string | null {
  if (goal.minuteInMatch == null) {
    return null;
  }

  const minutes = goal.minuteInMatch.toString();
  if (goal.secondInMatch == null || goal.secondInMatch === 0) {
    return `${minutes}'`;
  }

  return `${minutes}'${goal.secondInMatch.toString().padStart(2, '0')}"`;
}

function ownGoalEventLabel(event: MatchEvent): string {
  const playerName = event.playerName || 'Игрок не указан';
  return event.eventType === 'OWN_GOAL' ? `${playerName} (А)` : playerName;
}

async function openPlayerProfile(playerId: number) {
  await router.push(`/players/${playerId}`);
}

function eventTimeLabel(event: MatchEvent): string | null {
  if (event.minuteInMatch == null) {
    return null;
  }

  const minutes = event.minuteInMatch.toString();
  if (event.secondInMatch == null || event.secondInMatch === 0) {
    return `${minutes}' `;
  }

  return `${minutes}'${event.secondInMatch.toString().padStart(2, '0')}" `;
}

function matchStatusClass(status: SessionMatch['status']): string {
  return `status-pill--match-${status.toLowerCase()}`;
}

function matchCardStatusClass(status: SessionMatch['status']): string {
  return `match-card--${status.toLowerCase()}`;
}

function readStoredMatchStart(): number | null {
  const rawValue = window.localStorage.getItem(matchStartStorageKey.value);
  if (!rawValue) {
    return null;
  }

  const parsedValue = Number(rawValue);
  return Number.isFinite(parsedValue) ? parsedValue : null;
}

function persistMatchStart(timestamp: number | null) {
  if (timestamp == null) {
    window.localStorage.removeItem(matchStartStorageKey.value);
    return;
  }

  window.localStorage.setItem(matchStartStorageKey.value, String(timestamp));
}

async function loadSession() {
  session.value = await api.getSession(sessionIdNumber.value);
  const entries = await Promise.all(
    session.value.teams.map(async (team) => [team.id, await api.getTeamPlayers(team.id)] as const)
  );
  teamPlayers.value = Object.fromEntries(entries);
}

async function loadMatch() {
  match.value = await api.getMatch(sessionIdNumber.value, matchIdNumber.value);
  if (match.value.status !== 'IN_PROGRESS') {
    localStartedAt.value = null;
    persistMatchStart(null);
  } else {
    localStartedAt.value = readStoredMatchStart();
  }
  goalForm.teamId = goalForm.teamId ?? match.value.teamAId;
}

async function loadEvents() {
  events.value = await api.getMatchEvents(matchIdNumber.value);
}

async function startMatch() {
  if (sessionIsFinished.value || !match.value) return;
  const clientStartTimestamp = Date.now();
  localStartedAt.value = clientStartTimestamp;
  persistMatchStart(clientStartTimestamp);
  now.value = clientStartTimestamp;
  match.value = await api.startMatch(sessionIdNumber.value, match.value.id);
}

async function finishMatch() {
  if (sessionIsFinished.value || !match.value) return;
  match.value = await api.finishMatch(sessionIdNumber.value, match.value.id);
  localStartedAt.value = null;
  persistMatchStart(null);
}

async function addGoal() {
  if (sessionIsFinished.value) return;
  if (!match.value || !goalForm.teamId || !goalForm.scorerPlayerId) return;
  const minuteInMatch = Math.floor(elapsedSeconds.value / 60);
  const secondInMatch = elapsedSeconds.value % 60;
  await api.addGoal(match.value.id, {
    teamId: goalForm.teamId,
    scorerPlayerId: goalForm.scorerPlayerId,
    ownGoal: goalForm.ownGoal,
    assistPlayerId: goalForm.ownGoal ? null : goalForm.assistPlayerId ?? null,
    minuteInMatch,
    secondInMatch
  });
  goalForm.scorerPlayerId = undefined;
  goalForm.ownGoal = false;
  goalForm.assistPlayerId = undefined;
  await Promise.all([loadMatch(), loadEvents()]);
}

async function deleteEvent(eventId: number) {
  if (sessionIsFinished.value || !match.value) return;
  const deletedEvent = events.value.find((event) => String(event.id) === String(eventId));
  applyDeletedEvent(deletedEvent);
  try {
    await api.deleteEvent(match.value.id, eventId);
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Не удалось удалить событие';
  } finally {
    await Promise.allSettled([loadMatch(), loadEvents()]);
  }
}

function applyDeletedEvent(deletedEvent: MatchEvent | undefined) {
  if (!deletedEvent || !match.value) return;

  const deletedEventIds = new Set<string>([String(deletedEvent.id)]);
  if (deletedEvent.eventType === 'GOAL' || deletedEvent.eventType === 'OWN_GOAL') {
    events.value
      .filter((event) => String(event.linkedEventId) === String(deletedEvent.id))
      .forEach((event) => deletedEventIds.add(String(event.id)));

    if (deletedEvent.teamId === match.value.teamAId) {
      match.value = nextMatchState({
        teamAScore: Math.max(0, match.value.teamAScore - 1)
      });
    }
    if (deletedEvent.teamId === match.value.teamBId) {
      match.value = nextMatchState({
        teamBScore: Math.max(0, match.value.teamBScore - 1)
      });
    }
  }

  events.value = events.value.filter((event) => !deletedEventIds.has(String(event.id)));
}

function nextMatchState(patch: Partial<SessionMatch>): SessionMatch {
  if (!match.value) {
    throw new Error('Match state is not loaded');
  }

  const nextMatch = { ...match.value, ...patch };
  nextMatch.winningTeamId = resolveWinningTeamId(nextMatch);
  return nextMatch;
}

function resolveWinningTeamId(currentMatch: SessionMatch): number | null {
  if (currentMatch.teamAScore > currentMatch.teamBScore) {
    return currentMatch.teamAId;
  }
  if (currentMatch.teamBScore > currentMatch.teamAScore) {
    return currentMatch.teamBId;
  }
  return null;
}

watch(
  () => [goalForm.teamId, goalForm.ownGoal],
  () => {
    goalForm.scorerPlayerId = undefined;
    goalForm.assistPlayerId = undefined;
  }
);

onMounted(async () => {
  try {
    await Promise.all([loadSession(), loadMatch(), loadEvents()]);
    timerId = window.setInterval(() => {
      now.value = Date.now();
    }, 1000);
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Не удалось загрузить матч';
  }
});

onBeforeUnmount(() => {
  if (timerId) {
    window.clearInterval(timerId);
  }
});
</script>
