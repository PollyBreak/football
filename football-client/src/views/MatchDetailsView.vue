<template>
  <section class="stack" v-if="match">
    <RouterLink class="ghost-button back-button" :to="`/sessions/${sessionId}`">Назад к сессии</RouterLink>

    <div class="card stack-sm" :class="matchCardStatusClass(match.status)">
      <div class="section-header match-details-header">
        <div class="match-details-heading">
          <p class="eyebrow match-title-meta">
            <span>Матч #{{ match.matchNumber }}</span>
            <span class="status-pill" :class="matchStatusClass(match.status)">{{ matchStatusLabel(match.status) }}</span>
          </p>
          <h2 class="section-title">{{ match.teamAName }} против {{ match.teamBName }}</h2>
        </div>
      </div>

      <div class="match-scoreboard" :class="{ 'is-overtime': matchIsOvertime }">
        <div class="scoreboard-team scoreboard-team--left">
          <strong>{{ teamColorEmoji(match.teamAId) }} {{ match.teamAScore }}</strong>
        </div>
        <div class="scoreboard-center">
          <strong>:</strong>
        </div>
        <div class="scoreboard-team scoreboard-team--right">
          <strong>{{ match.teamBScore }} {{ teamColorEmoji(match.teamBId) }}</strong>
        </div>
        <span class="scoreboard-time">{{ elapsedLabel }}</span>
        <div v-if="teamAGoals.length || teamBGoals.length" class="scoreboard-goals-board">
          <div class="scoreboard-goals scoreboard-goals--left">
            <p v-for="goal in teamAGoals" :key="goal.id">
              <span v-if="goal.timeLabel" class="scoreboard-goal-time">{{ goal.timeLabel }}</span>
              <PlayerAvatar class="scoreboard-goal-avatar" :sources="[goal.playerPhotoUrl, goal.playerTelegramPhotoUrl]" :initials="playerInitials(goal.label)" alt="Фото игрока" />
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
          <div class="scoreboard-goals scoreboard-goals--right">
            <p v-for="goal in teamBGoals" :key="goal.id">
              <span v-if="goal.timeLabel" class="scoreboard-goal-time">{{ goal.timeLabel }}</span>
              <PlayerAvatar class="scoreboard-goal-avatar" :sources="[goal.playerPhotoUrl, goal.playerTelegramPhotoUrl]" :initials="playerInitials(goal.label)" alt="Фото игрока" />
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

      <div class="button-row match-details-actions match-actions-row">
        <button v-if="!sessionIsFinished" class="ghost-button" @click="startMatch" :disabled="match.status !== 'PLANNED'">Начать</button>
        <button v-if="!sessionIsFinished" class="ghost-button" :class="{ 'is-danger': matchIsOvertime }" @click="finishMatch" :disabled="match.status !== 'IN_PROGRESS'">Завершить</button>
        <button v-if="!sessionIsFinished && (match.status === 'FINISHED' || match.status === 'PAUSED')" class="ghost-button" @click="resumeMatch">Возобновить</button>
        <button class="ghost-button" @click="loadMatch">Обновить</button>
      </div>
      <button
        v-if="nextScheduledMatch"
        class="ghost-button current-match-button"
        type="button"
        @click="openNextScheduledMatch"
      >
        Перейти к следующему матчу
      </button>
    </div>

    <div v-if="!sessionIsFinished" class="card stack-sm">
      <div class="section-header">
        <h3 class="section-title">Гол и передача</h3>
      </div>
      <div class="grid-form">
        <select v-model.number="goalForm.teamId" class="input" :disabled="sessionIsFinished">
          <option :value="match.teamAId">{{ teamColorEmoji(match.teamAId) }} {{ match.teamAName }}</option>
          <option :value="match.teamBId">{{ teamColorEmoji(match.teamBId) }} {{ match.teamBName }}</option>
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

    <div v-if="!sessionIsFinished" class="card stack-sm">
      <div class="section-header">
        <h3 class="section-title">Замена</h3>
        <button class="ghost-button" type="button" @click="toggleSubstitutionPanel">
          {{ substitutionPanelOpen ? 'Скрыть' : 'Выбрать' }}
        </button>
      </div>
      <div v-if="substitutionPanelOpen" class="grid-form">
        <select v-model.number="substitutionForm.teamId" class="input" :disabled="!substitutionAvailable">
          <option :value="match.teamAId">{{ teamColorEmoji(match.teamAId) }} {{ match.teamAName }}</option>
          <option :value="match.teamBId">{{ teamColorEmoji(match.teamBId) }} {{ match.teamBName }}</option>
        </select>
        <select v-model.number="substitutionForm.playerOutId" class="input" :disabled="!substitutionAvailable">
          <option :value="undefined">Кого заменить</option>
          <option v-for="player in substitutionPlayerOutOptions" :key="`out-${player.playerId}`" :value="player.playerId">
            {{ player.label }}
          </option>
        </select>
        <select v-model.number="substitutionForm.playerInId" class="input" :disabled="!substitutionAvailable">
          <option :value="undefined">Кто выходит</option>
          <option v-for="player in substitutionPlayerInOptions" :key="`in-${player.playerId}`" :value="player.playerId">
            {{ player.label }}
          </option>
        </select>
        <button class="primary-button" type="button" @click="addSubstitution" :disabled="!canSubmitSubstitution">
          Заменить
        </button>
      </div>
      <p v-if="substitutionPanelOpen && !substitutionAvailable" class="muted">Замены доступны во время матча или паузы.</p>
    </div>

    <div class="card stack-sm">
      <div class="section-header">
        <h3 class="section-title">События матча</h3>
        <button class="ghost-button" @click="loadEvents">Обновить</button>
      </div>
      <p v-if="!events.length" class="muted">Пока нет событий.</p>
      <div v-else class="list">
        <article v-for="event in displayedEvents" :key="event.id" class="list-item">
          <div>
            <strong>
              <span v-if="event.teamId" class="event-team-emoji">{{ teamColorEmoji(event.teamId) }}</span>
              {{ matchEventLabel(event.eventType) }}
            </strong>
            <p v-if="eventMetaVisible(event)" class="muted">
              <span v-if="eventTimeLabel(event)" class="event-time-label">{{ eventTimeLabel(event) }}</span>
              <button
                v-if="event.playerId"
                type="button"
                class="player-link-button"
                @click="openPlayerProfile(event.playerId)"
              >
                {{ ownGoalEventLabel(event) }}
              </button>
              <span v-else-if="event.playerDisplayName || event.playerName">{{ ownGoalEventLabel(event) }}</span>
              <span v-if="event.relatedPlayerDisplayName || event.relatedPlayerName">
                ->
                <button
                  v-if="event.relatedPlayerId"
                  type="button"
                  class="player-link-button"
                  @click="openPlayerProfile(event.relatedPlayerId)"
                >
                  {{ event.relatedPlayerDisplayName || event.relatedPlayerName }}
                </button>
                <span v-else>{{ event.relatedPlayerDisplayName || event.relatedPlayerName }}</span>
              </span>
            </p>
          </div>
          <button v-if="!sessionIsFinished" class="danger-icon-button" type="button" aria-label="Удалить событие" @click="requestDeleteEvent(event)">
            <span class="danger-icon-button__icon">×</span>
          </button>
        </article>
      </div>
    </div>

    <div v-if="deleteDialogEvent" class="settings-overlay" @click.self="closeDeleteEventDialog">
      <div class="settings-window stack-sm delete-event-dialog">
        <div>
          <p class="eyebrow">Удаление</p>
          <h3 class="section-title">Точно удалить событие?</h3>
        </div>
        <p class="muted">Это действие нельзя отменить.</p>
        <div class="button-row">
          <button class="ghost-button" type="button" @click="closeDeleteEventDialog">Отмена</button>
          <button class="ghost-button is-danger" type="button" @click="confirmDeleteEvent">Удалить</button>
        </div>
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
import type { GameSession, MatchEvent, MatchPlayer, SessionMatch, SessionPlayer, SessionTeamPlayer } from '../types';
import PlayerAvatar from '../components/PlayerAvatar.vue';

const props = defineProps<{ sessionId: string; matchId: string }>();
const router = useRouter();
type SubstitutionPlayerOption = { playerId: number; label: string };

const session = ref<GameSession | null>(null);
const match = ref<SessionMatch | null>(null);
const sessionMatches = ref<SessionMatch[]>([]);
const events = ref<MatchEvent[]>([]);
const matchPlayers = ref<MatchPlayer[]>([]);
const sessionPlayers = ref<SessionPlayer[]>([]);
const teamPlayers = ref<Record<number, SessionTeamPlayer[]>>({});
const deleteDialogEvent = ref<MatchEvent | null>(null);
const substitutionPanelOpen = ref(false);
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

const substitutionForm = reactive({
  teamId: undefined as number | undefined,
  playerOutId: undefined as number | undefined,
  playerInId: undefined as number | undefined
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
const substitutionAvailable = computed(() => match.value?.status === 'IN_PROGRESS' || match.value?.status === 'PAUSED');
const activeMatchPlayers = computed(() => matchPlayers.value.filter((player) => !player.endedAt));
const substitutionPlayerOutOptions = computed<SubstitutionPlayerOption[]>(() => {
  if (!substitutionForm.teamId) return [];
  const activePlayers = activeMatchPlayers.value
    .filter((player) => player.teamId === substitutionForm.teamId)
    .map((player) => ({
      playerId: player.playerId,
      label: player.playerDisplayName || player.playerName
    }));
  if (activePlayers.length) {
    return activePlayers;
  }
  return (teamPlayers.value[substitutionForm.teamId] ?? []).map((player) => ({
    playerId: player.playerId,
    label: player.playerDisplayName || player.playerName
  }));
});
const substitutionCandidatePlayers = computed<SubstitutionPlayerOption[]>(() => {
  const candidates = new Map<number, SubstitutionPlayerOption>();
  sessionPlayers.value.forEach((player) => {
    candidates.set(player.playerId, {
      playerId: player.playerId,
      label: sessionPlayerLabel(player)
    });
  });
  Object.values(teamPlayers.value).flat().forEach((player) => {
    if (!candidates.has(player.playerId)) {
      candidates.set(player.playerId, {
        playerId: player.playerId,
        label: player.playerDisplayName || player.playerName
      });
    }
  });
  return [...candidates.values()].sort((left, right) => left.label.localeCompare(right.label, 'ru'));
});
const substitutionPlayerInOptions = computed<SubstitutionPlayerOption[]>(() => {
  const activePlayerIds = new Set(activeMatchPlayers.value.map((player) => player.playerId));
  return substitutionCandidatePlayers.value
    .filter((player) => !activePlayerIds.has(player.playerId))
    .filter((player) => player.playerId !== substitutionForm.playerOutId);
});
const canSubmitSubstitution = computed(() => {
  return Boolean(
    substitutionAvailable.value
    && substitutionForm.teamId
    && substitutionForm.playerOutId
    && substitutionForm.playerInId
    && substitutionForm.playerOutId !== substitutionForm.playerInId
  );
});

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
const nextScheduledMatch = computed(() => {
  if (!match.value) {
    return null;
  }
  return sessionMatches.value
    .filter((item) => item.matchNumber > match.value!.matchNumber)
    .sort((left, right) => left.matchNumber - right.matchNumber)[0] ?? null;
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
      const scorer = shortPlayerName(goal.playerDisplayName ?? goal.playerName) ?? 'Игрок';
      const assistName = shortPlayerName(assist?.playerDisplayName ?? assist?.playerName ?? null);
      const ownGoalMarker = goal.eventType === 'OWN_GOAL' ? ' (А)' : '';
      return {
        id: goal.id,
        teamId: goal.teamId as number,
        playerId: goal.playerId,
        timeLabel: goalTimeLabel(goal),
        playerPhotoUrl: goal.playerPhotoUrl,
        playerTelegramPhotoUrl: goal.playerTelegramPhotoUrl,
        label: goal.eventType === 'OWN_GOAL'
          ? `${scorer}${ownGoalMarker}`
          : assistName ? `${scorer} (${assistName})` : scorer
      };
    });
});
const displayedEvents = computed(() => {
  return [...events.value].sort(compareMatchEvents);
});
const teamAGoals = computed(() => match.value ? goalSummaries.value.filter((goal) => goal.teamId === match.value?.teamAId) : []);
const teamBGoals = computed(() => match.value ? goalSummaries.value.filter((goal) => goal.teamId === match.value?.teamBId) : []);

function teamColorEmoji(teamId: number): string {
  const team = session.value?.teams.find((item) => item.id === teamId);
  const normalized = [team?.color, team?.name].filter(Boolean).join(' ').trim().toLowerCase();
  if (normalized.includes('blue') || normalized.includes('син')) return '🔵';
  if (normalized.includes('red') || normalized.includes('крас')) return '🔴';
  if (normalized.includes('green') || normalized.includes('зелен') || normalized.includes('зелён')) return '🟢';
  if (normalized.includes('yellow') || normalized.includes('желт') || normalized.includes('жёлт')) return '🟡';
  if (normalized.includes('orange') || normalized.includes('оранж')) return '🟠';
  if (normalized.includes('purple') || normalized.includes('фиолет')) return '🟣';
  if (normalized.includes('black') || normalized.includes('черн') || normalized.includes('чёрн')) return '⚫';
  if (normalized.includes('white') || normalized.includes('бел')) return '⚪';
  return '⚪';
}

function shortPlayerName(name: string | null): string | null {
  return name?.trim().split(/\s+/)[0] || null;
}

function playerInitials(name: string): string {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0])
    .join('') || 'И';
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
  const playerName = event.playerDisplayName || event.playerName || 'Игрок не указан';
  return event.eventType === 'OWN_GOAL' ? `${playerName} (А)` : playerName;
}

function sessionPlayerLabel(player: SessionPlayer): string {
  if (player.displayName?.trim()) {
    return player.displayName;
  }
  if (player.nickname?.trim()) {
    return player.nickname;
  }
  return [player.firstName, player.lastName].filter(Boolean).join(' ');
}

function eventMetaVisible(event: MatchEvent): boolean {
  return Boolean(
    eventTimeLabel(event)
    || event.playerDisplayName
    || event.playerName
    || event.relatedPlayerDisplayName
    || event.relatedPlayerName
  );
}

function compareMatchEvents(left: MatchEvent, right: MatchEvent): number {
  if (left.eventType === 'ASSIST' && String(left.linkedEventId) === String(right.id)) {
    return -1;
  }
  if (right.eventType === 'ASSIST' && String(right.linkedEventId) === String(left.id)) {
    return 1;
  }

  if (left.minuteInMatch != null && right.minuteInMatch != null) {
    const leftTime = eventSortTime(left);
    const rightTime = eventSortTime(right);
    if (leftTime !== rightTime) {
      return leftTime - rightTime;
    }
  }

  return left.id - right.id;
}

function eventSortTime(event: MatchEvent): number {
  return (event.minuteInMatch ?? 0) * 60 + (event.secondInMatch ?? 0);
}

async function openPlayerProfile(playerId: number) {
  await router.push(`/players/${playerId}`);
}

async function openNextScheduledMatch() {
  if (!nextScheduledMatch.value) {
    return;
  }
  await router.push(`/sessions/${sessionIdNumber.value}/matches/${nextScheduledMatch.value.id}`);
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
  sessionMatches.value = await api.getMatches(sessionIdNumber.value);
  sessionPlayers.value = await api.getSessionPlayers(sessionIdNumber.value);
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
  substitutionForm.teamId = substitutionForm.teamId ?? match.value.teamAId;
}

async function loadSessionMatches() {
  sessionMatches.value = await api.getMatches(sessionIdNumber.value);
}

async function loadEvents() {
  events.value = await api.getMatchEvents(matchIdNumber.value);
}

async function loadMatchPlayers() {
  matchPlayers.value = await api.getMatchPlayers(matchIdNumber.value);
}

async function startMatch() {
  if (sessionIsFinished.value || !match.value) return;
  const clientStartTimestamp = Date.now();
  localStartedAt.value = clientStartTimestamp;
  persistMatchStart(clientStartTimestamp);
  now.value = clientStartTimestamp;
  match.value = await api.startMatch(sessionIdNumber.value, match.value.id);
  await Promise.all([loadSession(), loadMatchPlayers()]);
}

async function finishMatch() {
  if (sessionIsFinished.value || !match.value) return;
  match.value = await api.finishMatch(sessionIdNumber.value, match.value.id);
  localStartedAt.value = null;
  persistMatchStart(null);
  await loadSessionMatches();
}

async function resumeMatch() {
  if (sessionIsFinished.value || !match.value) return;
  match.value = await api.resumeMatch(sessionIdNumber.value, match.value.id);
  localStartedAt.value = null;
  persistMatchStart(null);
  now.value = Date.now();
  await loadSessionMatches();
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

function toggleSubstitutionPanel() {
  substitutionPanelOpen.value = !substitutionPanelOpen.value;
  if (substitutionPanelOpen.value && match.value) {
    substitutionForm.teamId = substitutionForm.teamId ?? match.value.teamAId;
  }
}

async function addSubstitution() {
  if (!match.value || !canSubmitSubstitution.value) return;
  const minuteInMatch = Math.floor(elapsedSeconds.value / 60);
  const secondInMatch = elapsedSeconds.value % 60;
  await api.addSubstitution(match.value.id, {
    teamId: substitutionForm.teamId,
    playerInId: substitutionForm.playerInId,
    playerOutId: substitutionForm.playerOutId,
    minuteInMatch,
    secondInMatch
  });
  substitutionForm.playerInId = undefined;
  substitutionForm.playerOutId = undefined;
  substitutionPanelOpen.value = false;
  await Promise.all([loadEvents(), loadMatchPlayers()]);
}

function requestDeleteEvent(event: MatchEvent) {
  deleteDialogEvent.value = event;
}

function closeDeleteEventDialog() {
  deleteDialogEvent.value = null;
}

async function confirmDeleteEvent() {
  if (!deleteDialogEvent.value) {
    return;
  }
  const eventId = deleteDialogEvent.value.id;
  closeDeleteEventDialog();
  await deleteEvent(eventId);
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

watch(
  () => substitutionForm.teamId,
  () => {
    substitutionForm.playerInId = undefined;
    substitutionForm.playerOutId = undefined;
  }
);

watch(
  () => props.matchId,
  async () => {
    try {
      error.value = '';
      goalForm.teamId = undefined;
      goalForm.scorerPlayerId = undefined;
      goalForm.ownGoal = false;
      goalForm.assistPlayerId = undefined;
      substitutionPanelOpen.value = false;
      substitutionForm.teamId = undefined;
      substitutionForm.playerInId = undefined;
      substitutionForm.playerOutId = undefined;
      await Promise.all([loadMatch(), loadEvents(), loadMatchPlayers(), loadSessionMatches()]);
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Не удалось загрузить матч';
    }
  }
);

onMounted(async () => {
  try {
    await Promise.all([loadSession(), loadMatch(), loadEvents(), loadMatchPlayers()]);
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
