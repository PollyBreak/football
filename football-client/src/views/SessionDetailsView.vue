<template>
  <section class="stack" v-if="session">
    <div class="card hero-card">
      <div>
        <p class="eyebrow">Сессия</p>
        <h2 class="section-title">{{ session.title }}</h2>
        <p class="muted">{{ session.sessionDate }} • {{ session.location || 'Место не указано' }}</p>
        <p class="muted">Игроки: {{ sessionPlayers.length }} / {{ session.maxPlayers || 'без лимита' }}</p>
      </div>
      <div class="hero-actions">
        <span class="status-pill">{{ sessionStatusLabel(session.status) }}</span>
        <button class="icon-button" type="button" aria-label="Настройки сессии" @click="settingsOpen = true">
          &#9881;
        </button>
      </div>
    </div>

    <div v-if="settingsOpen" class="settings-overlay" @click.self="settingsOpen = false">
      <div class="settings-window">
        <div class="section-header">
          <button class="ghost-button" type="button" @click="settingsOpen = false">Назад</button>
          <button class="primary-button" type="button" @click="saveSessionSettings" :disabled="pendingSessionUpdate">Сохранить</button>
        </div>
        <div class="stack-sm">
          <div>
            <p class="eyebrow">Настройки</p>
            <h3 class="section-title">Сессия</h3>
          </div>
          <label class="field-label">
            <span>Максимум игроков</span>
            <input v-model.number="sessionSettings.maxPlayers" class="input" type="number" min="1" />
          </label>
        </div>
      </div>
    </div>

    <div class="card stack-sm session-settings-inline">
      <div class="section-header">
        <h3 class="section-title">Настройки сессии</h3>
        <button class="ghost-button" @click="saveSessionSettings" :disabled="pendingSessionUpdate">Сохранить</button>
      </div>
      <label class="field-label">
        <span>Максимум игроков</span>
        <input v-model.number="sessionSettings.maxPlayers" class="input" type="number" min="1" />
      </label>
    </div>

    <button
      class="primary-button join-session-button"
      :class="{ 'is-danger': currentUserSessionPlayer || currentUserWaitlistEntry }"
      @click="toggleCurrentUserSession"
      :disabled="!authState.player || pendingMembership"
    >
      {{ membershipButtonLabel }}
    </button>

    <div class="tabs">
      <button v-for="tab in tabs" :key="tab" class="tab-button" :class="{ 'is-active': activeTab === tab }" @click="activeTab = tab">
        {{ tabLabels[tab] }}
      </button>
    </div>

    <div v-if="activeTab === 'Players'" class="stack-sm">
      <div class="card stack-sm">
        <div class="section-header">
          <h3 class="section-title">Игроки сессии</h3>
          <button class="ghost-button" @click="refreshAll">Обновить</button>
        </div>
        <p v-if="!sessionPlayers.length" class="muted">Пока нет игроков в сессии.</p>
        <div v-else class="list">
          <article v-for="player in sessionPlayers" :key="player.id" class="list-item">
            <div class="list-item__lead">
              <div class="player-avatar player-avatar--sm">
                <img v-if="player.photoUrl" :src="player.photoUrl" alt="Фото игрока" />
                <span v-else>{{ playerInitials(player) }}</span>
              </div>
              <div>
                <strong>{{ player.firstName }} {{ player.lastName ?? '' }}</strong>
                <p class="muted">{{ player.nickname || 'Без никнейма' }}</p>
              </div>
            </div>
            <span class="item-tag">{{ playerPositionLabel(player.position) }}</span>
          </article>
        </div>
      </div>

      <div class="card stack-sm">
        <div class="section-header">
          <h3 class="section-title">Очередь</h3>
        </div>
        <p v-if="!waitlist.length" class="muted">Очередь пуста.</p>
        <div v-else class="list">
          <article v-for="(entry, index) in waitlist" :key="entry.id" class="list-item">
            <div class="list-item__lead">
              <div class="player-avatar player-avatar--sm">
                <img v-if="entry.photoUrl" :src="entry.photoUrl" alt="Фото игрока" />
                <span v-else>{{ waitlistInitials(entry) }}</span>
              </div>
              <div>
                <strong>{{ index + 1 }}. {{ entry.firstName }} {{ entry.lastName ?? '' }}</strong>
                <p class="muted">{{ entry.nickname || 'Без никнейма' }}</p>
              </div>
            </div>
            <span class="item-tag">{{ playerPositionLabel(entry.position) }}</span>
          </article>
        </div>
      </div>

      <div class="card stack-sm">
        <div class="section-header">
          <h3 class="section-title">Добавить игрока</h3>
        </div>
        <div class="grid-form">
          <select v-model.number="sessionPlayerForm.playerId" class="input">
            <option :value="undefined">Выберите игрока</option>
            <option v-for="player in allPlayers" :key="player.playerId" :value="player.playerId">
              {{ player.firstName }} {{ player.lastName ?? '' }}
            </option>
          </select>
          <select v-model="sessionPlayerForm.position" class="input">
            <option v-for="position in positions" :key="position" :value="position">{{ playerPositionLabel(position) }}</option>
          </select>
          <button class="primary-button" @click="addPlayerToSession">Добавить в сессию</button>
        </div>
      </div>
    </div>

    <div v-if="activeTab === 'Teams'" class="card stack-sm">
      <div class="section-header">
        <h3 class="section-title">Распределение по командам</h3>
      </div>
      <div v-for="team in session.teams" :key="team.id" class="team-block">
        <div class="team-block__header">
          <strong>{{ team.name }}</strong>
          <button class="primary-button" @click="assignSelectedPlayers(team.id)">Назначить выбранных</button>
        </div>
        <div class="chips">
          <label v-for="player in sessionPlayers" :key="`${team.id}-${player.playerId}`" class="chip">
            <input
              type="checkbox"
              :checked="isPlayerSelectedForTeam(team.id, player.playerId)"
              @change="toggleTeamPlayer(team.id, player.playerId, $event)"
            />
            <span>{{ player.firstName }}</span>
          </label>
        </div>
        <div class="list">
          <article v-for="member in teamPlayers[team.id] || []" :key="member.id" class="list-item">
            <div class="list-item__lead">
              <img v-if="member.photoUrl" :src="member.photoUrl" alt="Фото игрока" class="avatar avatar--sm" />
              <strong>{{ member.playerName }}</strong>
            </div>
            <span class="item-tag">{{ playerPositionLabel(member.position) }}</span>
          </article>
        </div>
      </div>
    </div>

    <div v-if="activeTab === 'Matches'" class="card stack-sm">
      <div class="section-header">
        <h3 class="section-title">Матчи</h3>
        <button class="primary-button" @click="createNextMatch">Создать следующий</button>
      </div>
      <div class="list">
        <article v-for="match in matches" :key="match.id" class="match-card">
          <div class="match-card__top">
            <strong>#{{ match.matchNumber }} {{ match.teamAName }} против {{ match.teamBName }}</strong>
            <span class="status-pill">{{ matchStatusLabel(match.status) }}</span>
          </div>
          <p class="score-line">{{ match.teamAScore }} : {{ match.teamBScore }}</p>
          <div class="button-row">
            <button class="ghost-button" @click="startMatch(match.id)" :disabled="match.status !== 'PLANNED'">Начать</button>
            <button class="ghost-button" @click="openMatch(match.id)">Открыть</button>
            <button class="ghost-button" @click="finishMatch(match.id)" :disabled="match.status === 'FINISHED'">Завершить</button>
          </div>
        </article>
      </div>
    </div>

    <div v-if="activeTab === 'Standings'" class="card stack-sm">
      <div class="section-header">
        <h3 class="section-title">Таблица</h3>
        <button class="ghost-button" @click="loadStandings">Обновить</button>
      </div>
      <div class="table-scroll">
        <table class="standings-table">
          <thead>
            <tr>
              <th>Команда</th>
              <th>Игры</th>
              <th>Очки</th>
              <th>В</th>
              <th>Н</th>
              <th>П</th>
              <th>ЗГ</th>
              <th>ПГ</th>
              <th>РГ</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in standings" :key="row.teamId">
              <td>{{ row.teamName }}</td>
              <td>{{ row.played }}</td>
              <td>{{ row.points }}</td>
              <td>{{ row.wins }}</td>
              <td>{{ row.draws }}</td>
              <td>{{ row.losses }}</td>
              <td>{{ row.goalsFor }}</td>
              <td>{{ row.goalsAgainst }}</td>
              <td>{{ row.goalDifference }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <p v-if="error" class="error-text">{{ error }}</p>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { api } from '../lib/api';
import { authState } from '../lib/auth';
import { matchStatusLabel, playerPositionLabel, sessionStatusLabel } from '../lib/labels';
import { getStartParam } from '../lib/telegram';
import type {
  GameSession,
  PlayerPosition,
  PlayerProfile,
  SessionMatch,
  SessionPlayer,
  SessionStandingsRow,
  SessionTeamPlayer,
  SessionWaitlistEntry
} from '../types';

const props = defineProps<{ sessionId: string }>();
const router = useRouter();

const tabs = ['Players', 'Teams', 'Matches', 'Standings'] as const;
const tabLabels: Record<(typeof tabs)[number], string> = {
  Players: 'Игроки',
  Teams: 'Команды',
  Matches: 'Матчи',
  Standings: 'Таблица'
};
const positions: PlayerPosition[] = ['GOALKEEPER', 'DEFENDER', 'MIDFIELDER', 'FORWARD', 'UNIVERSAL'];

const session = ref<GameSession | null>(null);
const sessionPlayers = ref<SessionPlayer[]>([]);
const waitlist = ref<SessionWaitlistEntry[]>([]);
const allPlayers = ref<PlayerProfile[]>([]);
const matches = ref<SessionMatch[]>([]);
const standings = ref<SessionStandingsRow[]>([]);
const teamPlayers = ref<Record<number, SessionTeamPlayer[]>>({});
const selectedPlayersByTeam = reactive<Record<number, number[]>>({});
const activeTab = ref<(typeof tabs)[number]>('Players');
const error = ref('');
const pendingMembership = ref(false);
const pendingSessionUpdate = ref(false);
const settingsOpen = ref(false);

const sessionIdNumber = computed(() => Number(props.sessionId));
const currentUserSessionPlayer = computed(() => {
  const currentPlayerId = authState.player?.playerId;
  return currentPlayerId
    ? sessionPlayers.value.find((player) => player.playerId === currentPlayerId)
    : undefined;
});
const currentUserWaitlistEntry = computed(() => {
  const currentPlayerId = authState.player?.playerId;
  return currentPlayerId
    ? waitlist.value.find((entry) => entry.playerId === currentPlayerId)
    : undefined;
});
const sessionIsFull = computed(() => {
  return Boolean(session.value?.maxPlayers && sessionPlayers.value.length >= session.value.maxPlayers);
});
const membershipButtonLabel = computed(() => {
  if (currentUserSessionPlayer.value) return 'Покинуть игру';
  if (currentUserWaitlistEntry.value) return 'Покинуть очередь';
  return sessionIsFull.value ? 'Встать в очередь' : 'Присоединиться к игре';
});

const sessionPlayerForm = reactive({
  playerId: undefined as number | undefined,
  position: 'MIDFIELDER' as PlayerPosition
});
const sessionSettings = reactive({
  maxPlayers: 15 as number | null
});

function playerInitials(player: SessionPlayer): string {
  return [player.firstName, player.lastName]
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part?.[0])
    .join('') || 'И';
}

function waitlistInitials(entry: SessionWaitlistEntry): string {
  return [entry.firstName, entry.lastName]
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part?.[0])
    .join('') || 'И';
}

function ensureTeamSelection(teamId: number) {
  if (!selectedPlayersByTeam[teamId]) {
    selectedPlayersByTeam[teamId] = [];
  }
}

function isPlayerSelectedForTeam(teamId: number, playerId: number): boolean {
  ensureTeamSelection(teamId);
  return selectedPlayersByTeam[teamId].includes(playerId);
}

function toggleTeamPlayer(teamId: number, playerId: number, event: Event) {
  ensureTeamSelection(teamId);
  const checked = (event.target as HTMLInputElement).checked;
  const current = selectedPlayersByTeam[teamId];
  selectedPlayersByTeam[teamId] = checked
    ? [...new Set([...current, playerId])]
    : current.filter((id) => id !== playerId);
}

async function refreshAll() {
  await Promise.all([
    loadSession(),
    loadSessionPlayers(),
    loadWaitlist(),
    loadPlayers(),
    loadMatches(),
    loadStandings()
  ]);
}

async function loadSession() {
  session.value = await api.getSession(sessionIdNumber.value);
  sessionSettings.maxPlayers = session.value.maxPlayers ?? null;
  session.value.teams.forEach((team) => ensureTeamSelection(team.id));
}

async function loadPlayers() {
  allPlayers.value = await api.getPlayers();
}

async function loadSessionPlayers() {
  sessionPlayers.value = await api.getSessionPlayers(sessionIdNumber.value);
}

async function loadWaitlist() {
  waitlist.value = await api.getSessionWaitlist(sessionIdNumber.value);
}

async function loadMatches() {
  matches.value = await api.getMatches(sessionIdNumber.value);
}

async function loadStandings() {
  const data = await api.getStandings(sessionIdNumber.value);
  standings.value = data.standings;
}

async function loadTeamPlayers() {
  if (!session.value) return;
  const entries = await Promise.all(
    session.value.teams.map(async (team) => [team.id, await api.getTeamPlayers(team.id)] as const)
  );
  teamPlayers.value = Object.fromEntries(entries);
}

async function saveSessionSettings() {
  pendingSessionUpdate.value = true;
  error.value = '';
  try {
    session.value = await api.updateSession(sessionIdNumber.value, {
      maxPlayers: sessionSettings.maxPlayers || null
    });
    await Promise.all([loadSessionPlayers(), loadWaitlist()]);
    settingsOpen.value = false;
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Не удалось сохранить настройки сессии';
  } finally {
    pendingSessionUpdate.value = false;
  }
}

async function addPlayerToSession() {
  if (!sessionPlayerForm.playerId) return;
  await api.addPlayerToSession(sessionIdNumber.value, {
    playerId: sessionPlayerForm.playerId,
    position: sessionPlayerForm.position
  });
  sessionPlayerForm.playerId = undefined;
  await Promise.all([loadSessionPlayers(), loadWaitlist()]);
}

async function toggleCurrentUserSession() {
  if (!authState.player) {
    error.value = 'Сначала заполните профиль игрока';
    return;
  }

  pendingMembership.value = true;
  error.value = '';
  try {
    if (currentUserSessionPlayer.value) {
      await api.removePlayerFromSession(sessionIdNumber.value, authState.player.playerId);
    } else if (currentUserWaitlistEntry.value) {
      await api.leaveSessionWaitlist(sessionIdNumber.value, authState.player.playerId);
    } else {
      await api.joinSession(sessionIdNumber.value, {
        playerId: authState.player.playerId,
        position: authState.player.defaultPosition
      });
    }
    await Promise.all([loadSessionPlayers(), loadWaitlist(), loadTeamPlayers()]);
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Не удалось изменить участие в игре';
  } finally {
    pendingMembership.value = false;
  }
}

async function assignSelectedPlayers(teamId: number) {
  const ids = selectedPlayersByTeam[teamId] ?? [];
  if (!ids.length) return;
  await api.bulkAssignPlayers(teamId, ids);
  selectedPlayersByTeam[teamId] = [];
  await loadTeamPlayers();
}

async function createNextMatch() {
  if (!session.value || session.value.teams.length < 2) return;
  const teamA = session.value.teams[matches.value.length % session.value.teams.length];
  const teamB = session.value.teams[(matches.value.length + 1) % session.value.teams.length];
  if (teamA.id === teamB.id) return;

  await api.createMatch(sessionIdNumber.value, {
    teamAId: teamA.id,
    teamBId: teamB.id,
    plannedDurationMinutes: session.value.plannedMatchDurationMinutes ?? 6
  });
  await loadMatches();
}

async function startMatch(matchId: number) {
  await api.startMatch(sessionIdNumber.value, matchId);
  await loadMatches();
}

async function finishMatch(matchId: number) {
  await api.finishMatch(sessionIdNumber.value, matchId);
  await Promise.all([loadMatches(), loadStandings()]);
}

async function openMatch(matchId: number) {
  await router.push(`/sessions/${sessionIdNumber.value}/matches/${matchId}`);
}

watch(
  () => activeTab.value,
  async (tab) => {
    try {
      if (tab === 'Teams') {
        await loadTeamPlayers();
      }
      if (tab === 'Standings') {
        await loadStandings();
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Не удалось загрузить вкладку';
    }
  }
);

onMounted(async () => {
  try {
    const startParam = getStartParam();
    if (startParam?.startsWith('session_')) {
      activeTab.value = 'Players';
    }
    await refreshAll();
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Не удалось загрузить сессию';
  }
});
</script>
