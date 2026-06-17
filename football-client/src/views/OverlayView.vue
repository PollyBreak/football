<template>
  <main class="overlay-page">
    <section class="overlay-scoreboard" :class="{ 'is-offline': connectionStatus !== 'live' }">
      <div class="overlay-scoreboard__top">
        <span class="overlay-status-dot"></span>
        <span>{{ statusLabel }}</span>
      </div>

      <div v-if="currentMatch" class="overlay-scoreboard__match">
        <div class="overlay-team-name">{{ currentMatch.teamAName }}</div>
        <div class="overlay-score">{{ currentMatch.teamAScore }}<span>:</span>{{ currentMatch.teamBScore }}</div>
        <div class="overlay-team-name">{{ currentMatch.teamBName }}</div>
      </div>

      <div v-else class="overlay-scoreboard__empty">Матч не выбран</div>

      <div class="overlay-scoreboard__meta">
        <strong>{{ minuteLabel }}</strong>
        <span v-if="currentMatch">Матч {{ currentMatch.matchNumber }}</span>
      </div>

      <div v-if="goalRows.length" class="overlay-goal-list">
        <p v-for="goal in goalRows" :key="goal.id">
          <span>{{ goal.minute }}</span>
          <strong>{{ goal.name }}</strong>
        </p>
      </div>
    </section>

    <section v-if="currentMatch" class="overlay-lineups">
      <div class="overlay-lineups__team">
        <strong :style="teamAccentStyle(leftTeam)">{{ currentMatch.teamAName }}</strong>
        <div class="overlay-lineups__players">
          <article v-for="player in visiblePlayers(leftTeam)" :key="player.id" class="overlay-player">
            <div class="overlay-player__photo">
              <img v-if="player.photoUrl" :src="player.photoUrl" :alt="player.playerName" />
              <span v-else>{{ initials(player.playerName) }}</span>
            </div>
            <span>{{ shortName(player.playerName) }}</span>
          </article>
        </div>
      </div>
      <div class="overlay-lineups__divider">VS</div>
      <div class="overlay-lineups__team overlay-lineups__team--right">
        <strong :style="teamAccentStyle(rightTeam)">{{ currentMatch.teamBName }}</strong>
        <div class="overlay-lineups__players overlay-lineups__players--right">
          <article v-for="player in visiblePlayers(rightTeam)" :key="player.id" class="overlay-player">
            <div class="overlay-player__photo">
              <img v-if="player.photoUrl" :src="player.photoUrl" :alt="player.playerName" />
              <span v-else>{{ initials(player.playerName) }}</span>
            </div>
            <span>{{ shortName(player.playerName) }}</span>
          </article>
        </div>
      </div>
    </section>

    <Transition name="goal-toast">
      <section v-if="goalToast" class="overlay-goal-toast">
        <p>{{ goalToast.cancelled ? 'Гол отменен' : 'ГОЛ' }}</p>
        <strong>{{ goalToast.scorer }}</strong>
        <span v-if="goalToast.assist && !goalToast.cancelled">Пас: {{ goalToast.assist }}</span>
      </section>
    </Transition>

    <section v-if="error" class="overlay-error">{{ error }}</section>
  </main>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { api } from '../lib/api';
import type { MatchEvent, OverlayEvent, OverlayState, OverlayTeam, SessionTeamPlayer } from '../types';

const props = defineProps<{
  sessionId: string;
}>();

const route = useRoute();
const state = ref<OverlayState | null>(null);
const now = ref(new Date());
const connectionStatus = ref<'connecting' | 'live' | 'offline'>('connecting');
const error = ref('');
const goalToast = ref<{ scorer: string; assist: string | null; cancelled: boolean } | null>(null);

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

const currentMatch = computed(() => state.value?.currentMatch ?? null);
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
      minute: formatEventMinute(event),
      name: event.playerName ? shortName(event.playerName) : 'Игрок'
    }))
    .slice(-5);
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
    scorer: event.playerName ? shortName(event.playerName) : 'Игрок',
    assist: assist?.playerName ? shortName(assist.playerName) : null,
    cancelled
  };
  if (toastTimer) {
    window.clearTimeout(toastTimer);
  }
  toastTimer = window.setTimeout(() => {
    goalToast.value = null;
  }, cancelled ? 3200 : 5200);
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
  return team.players.filter((player) => player.active).slice(0, 10);
}

function teamAccentStyle(team: OverlayTeam | null): Record<string, string> {
  return {
    borderColor: team?.color || '#2ef27a',
    color: team?.color || '#2ef27a',
    '--team-color': team?.color || '#2ef27a'
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
.overlay-lineups,
.overlay-goal-toast,
.overlay-error {
  background: linear-gradient(135deg, rgba(6, 15, 18, 0.9), rgba(16, 30, 34, 0.78));
  border: 1px solid rgba(255, 255, 255, 0.14);
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(16px);
}

.overlay-scoreboard {
  position: absolute;
  top: 28px;
  left: 28px;
  width: min(520px, calc(100vw - 56px));
  padding: 16px 18px;
  border-radius: 8px;
}

.overlay-scoreboard__top,
.overlay-scoreboard__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: rgba(247, 255, 249, 0.76);
  font-size: 13px;
  font-weight: 800;
  text-transform: uppercase;
}

.overlay-status-dot {
  width: 9px;
  height: 9px;
  border-radius: 999px;
  background: #2ef27a;
  box-shadow: 0 0 18px rgba(46, 242, 122, 0.9);
}

.overlay-scoreboard.is-offline .overlay-status-dot {
  background: #ffbf4d;
  box-shadow: 0 0 18px rgba(255, 191, 77, 0.8);
}

.overlay-scoreboard__match {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 16px;
  margin: 10px 0;
}

.overlay-team-name {
  min-width: 0;
  overflow: hidden;
  font-size: clamp(17px, 2vw, 25px);
  font-weight: 900;
  text-overflow: ellipsis;
  text-transform: uppercase;
  white-space: nowrap;
}

.overlay-team-name:last-child {
  text-align: right;
}

.overlay-score {
  display: flex;
  align-items: baseline;
  gap: 8px;
  min-width: 118px;
  justify-content: center;
  color: #2ef27a;
  font-size: clamp(38px, 5vw, 62px);
  font-weight: 950;
  line-height: 0.95;
}

.overlay-score span {
  color: rgba(247, 255, 249, 0.72);
  font-size: 0.7em;
}

.overlay-scoreboard__empty {
  padding: 18px 0 12px;
  color: rgba(247, 255, 249, 0.74);
  font-size: 22px;
  font-weight: 800;
}

.overlay-scoreboard__meta strong {
  color: #2ef27a;
  font-size: 18px;
}

.overlay-goal-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 5px 12px;
  margin-top: 12px;
  color: rgba(247, 255, 249, 0.8);
  font-size: 13px;
}

.overlay-goal-list p {
  min-width: 0;
  margin: 0;
  display: flex;
  gap: 6px;
}

.overlay-goal-list strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.overlay-player {
  min-width: 76px;
  max-width: 92px;
  display: grid;
  justify-items: center;
  align-items: center;
  gap: 7px;
  text-align: center;
}

.overlay-player__photo {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  overflow: hidden;
  border-radius: 50%;
  color: #061012;
  background: linear-gradient(135deg, #eaffef, #2ef27a);
  font-size: 13px;
  font-weight: 950;
}

.overlay-player__photo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.overlay-player strong,
.overlay-player span {
  display: block;
  width: 100%;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.overlay-player span {
  color: rgba(247, 255, 249, 0.86);
  font-size: 12px;
  font-weight: 850;
}

.overlay-lineups {
  position: absolute;
  left: 50%;
  bottom: 28px;
  width: min(1320px, calc(100vw - 56px));
  min-height: 144px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 64px minmax(0, 1fr);
  align-items: stretch;
  gap: 16px;
  padding: 14px 18px;
  border-radius: 8px;
  transform: translateX(-50%);
}

.overlay-lineups__team {
  min-width: 0;
}

.overlay-lineups__team--right {
  text-align: right;
}

.overlay-lineups__team strong {
  display: block;
  margin-bottom: 10px;
  border-bottom: 3px solid var(--team-color, #2ef27a);
  padding-bottom: 6px;
  font-size: 18px;
  font-weight: 950;
  text-transform: uppercase;
}

.overlay-lineups__players {
  display: flex;
  gap: 12px;
  min-width: 0;
  overflow: hidden;
}

.overlay-lineups__players--right {
  justify-content: flex-end;
}

.overlay-lineups__divider {
  display: grid;
  place-items: center;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  color: #061012;
  background: #2ef27a;
  font-weight: 950;
}

.overlay-goal-toast {
  position: absolute;
  left: 50%;
  top: 50%;
  width: min(620px, calc(100vw - 56px));
  padding: 28px 34px;
  border-radius: 8px;
  text-align: center;
  transform: translate(-50%, -50%);
}

.overlay-goal-toast p,
.overlay-goal-toast strong,
.overlay-goal-toast span {
  display: block;
}

.overlay-goal-toast p {
  margin: 0 0 8px;
  color: #2ef27a;
  font-size: clamp(36px, 6vw, 80px);
  font-weight: 950;
  line-height: 1;
  text-transform: uppercase;
}

.overlay-goal-toast strong {
  font-size: clamp(24px, 3vw, 38px);
}

.overlay-goal-toast span {
  margin-top: 10px;
  color: rgba(247, 255, 249, 0.76);
  font-size: 18px;
  font-weight: 800;
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
  .overlay-lineups,
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
    transform: none;
    grid-template-columns: minmax(0, 1fr);
  }

  .overlay-lineups__players,
  .overlay-lineups__players--right {
    justify-content: flex-start;
    overflow-x: auto;
  }

  .overlay-lineups__divider {
    display: none;
  }

  .overlay-lineups__team--right {
    text-align: left;
  }
}
</style>
