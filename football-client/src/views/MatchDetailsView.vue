<template>
  <section class="stack" v-if="match">
    <RouterLink class="ghost-button back-button" :to="`/sessions/${sessionId}`">Назад к сессии</RouterLink>

    <div class="card stack-sm">
      <div class="section-header">
        <div>
          <p class="eyebrow">Матч #{{ match.matchNumber }}</p>
          <h2 class="section-title">{{ match.teamAName }} против {{ match.teamBName }}</h2>
        </div>
        <span class="status-pill">{{ matchStatusLabel(match.status) }}</span>
      </div>

      <div class="match-scoreboard">
        <strong>{{ match.teamAScore }} : {{ match.teamBScore }}</strong>
        <span>{{ elapsedLabel }}</span>
      </div>

      <div class="button-row">
        <button class="ghost-button" @click="startMatch" :disabled="match.status !== 'PLANNED'">Начать</button>
        <button class="ghost-button" @click="finishMatch" :disabled="match.status === 'FINISHED'">Завершить</button>
        <button class="ghost-button" @click="loadMatch">Обновить</button>
      </div>
    </div>

    <div class="card stack-sm">
      <div class="section-header">
        <h3 class="section-title">Гол и передача</h3>
      </div>
      <div class="grid-form">
        <select v-model.number="goalForm.teamId" class="input">
          <option :value="match.teamAId">{{ match.teamAName }}</option>
          <option :value="match.teamBId">{{ match.teamBName }}</option>
        </select>
        <select v-model.number="goalForm.scorerPlayerId" class="input">
          <option :value="undefined">Автор гола</option>
          <option v-for="player in selectedTeamPlayers" :key="player.playerId" :value="player.playerId">
            {{ player.playerName }}
          </option>
        </select>
        <select v-model.number="goalForm.assistPlayerId" class="input">
          <option :value="undefined">Без голевой передачи</option>
          <option v-for="player in selectedTeamPlayers" :key="`assist-${player.playerId}`" :value="player.playerId">
            {{ player.playerName }}
          </option>
        </select>
        <button class="primary-button" @click="addGoal" :disabled="!goalForm.teamId || !goalForm.scorerPlayerId">Добавить гол</button>
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
              {{ event.playerName || 'Игрок не указан' }} • {{ event.teamName || 'Команда не указана' }}
              <span v-if="event.relatedPlayerName"> • передача: {{ event.relatedPlayerName }}</span>
            </p>
          </div>
          <button class="ghost-button" @click="deleteEvent(event.id)">Удалить</button>
        </article>
      </div>
    </div>

    <p v-if="error" class="error-text">{{ error }}</p>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { RouterLink } from 'vue-router';
import { api } from '../lib/api';
import { matchEventLabel, matchStatusLabel } from '../lib/labels';
import type { GameSession, MatchEvent, SessionMatch, SessionTeamPlayer } from '../types';

const props = defineProps<{ sessionId: string; matchId: string }>();

const session = ref<GameSession | null>(null);
const match = ref<SessionMatch | null>(null);
const events = ref<MatchEvent[]>([]);
const teamPlayers = ref<Record<number, SessionTeamPlayer[]>>({});
const error = ref('');
const now = ref(Date.now());
let timerId: number | undefined;

const sessionIdNumber = computed(() => Number(props.sessionId));
const matchIdNumber = computed(() => Number(props.matchId));

const goalForm = reactive({
  teamId: undefined as number | undefined,
  scorerPlayerId: undefined as number | undefined,
  assistPlayerId: undefined as number | undefined
});

const selectedTeamPlayers = computed(() => {
  if (!goalForm.teamId) return [];
  return teamPlayers.value[goalForm.teamId] ?? [];
});

const elapsedLabel = computed(() => {
  if (!match.value?.startedAt) {
    return '00:00';
  }

  const startedAt = new Date(match.value.startedAt).getTime();
  const endedAt = match.value.endedAt ? new Date(match.value.endedAt).getTime() : now.value;
  const totalSeconds = Math.max(0, Math.floor((endedAt - startedAt) / 1000));
  const minutes = Math.floor(totalSeconds / 60).toString().padStart(2, '0');
  const seconds = (totalSeconds % 60).toString().padStart(2, '0');
  return `${minutes}:${seconds}`;
});

async function loadSession() {
  session.value = await api.getSession(sessionIdNumber.value);
  const entries = await Promise.all(
    session.value.teams.map(async (team) => [team.id, await api.getTeamPlayers(team.id)] as const)
  );
  teamPlayers.value = Object.fromEntries(entries);
}

async function loadMatch() {
  match.value = await api.getMatch(sessionIdNumber.value, matchIdNumber.value);
  goalForm.teamId = goalForm.teamId ?? match.value.teamAId;
}

async function loadEvents() {
  events.value = await api.getMatchEvents(matchIdNumber.value);
}

async function startMatch() {
  if (!match.value) return;
  match.value = await api.startMatch(sessionIdNumber.value, match.value.id);
}

async function finishMatch() {
  if (!match.value) return;
  match.value = await api.finishMatch(sessionIdNumber.value, match.value.id);
}

async function addGoal() {
  if (!match.value || !goalForm.teamId || !goalForm.scorerPlayerId) return;
  await api.addGoal(match.value.id, {
    teamId: goalForm.teamId,
    scorerPlayerId: goalForm.scorerPlayerId,
    assistPlayerId: goalForm.assistPlayerId ?? null
  });
  goalForm.scorerPlayerId = undefined;
  goalForm.assistPlayerId = undefined;
  await Promise.all([loadMatch(), loadEvents()]);
}

async function deleteEvent(eventId: number) {
  if (!match.value) return;
  await api.deleteEvent(match.value.id, eventId);
  await Promise.all([loadMatch(), loadEvents()]);
}

watch(
  () => goalForm.teamId,
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
