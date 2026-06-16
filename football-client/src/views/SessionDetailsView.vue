<template>
  <section class="stack" v-if="session">
    <div class="card hero-card">
      <div>
        <p class="eyebrow">Сессия</p>
        <h2 class="section-title">{{ session.title }}</h2>
        <p class="muted">{{ session.sessionDate }} {{ session.sessionTime?.slice(0, 5) }} • {{ session.location || 'Место не указано' }}</p>
        <a v-if="session.locationUrl" class="muted map-link" :href="session.locationUrl" target="_blank" rel="noreferrer">
          Открыть поле на карте
        </a>
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
            <span>Название сессии</span>
            <input v-model="sessionSettings.title" class="input" placeholder="Название сессии" />
          </label>
          <label class="field-label">
            <span>Дата сессии</span>
            <input v-model="sessionSettings.sessionDate" class="input" type="date" />
          </label>
          <label class="field-label">
            <span>Время сессии</span>
            <input v-model="sessionSettings.sessionTime" class="input" type="time" />
          </label>
          <label class="field-label">
            <span>Место</span>
            <input v-model="sessionSettings.location" class="input" placeholder="Место" />
          </label>
          <label class="field-label">
            <span>Ссылка на поле на 2GIS / Google Maps / Яндекс картах</span>
            <input v-model="sessionSettings.locationUrl" class="input" type="url" placeholder="https://..." />
          </label>
          <label class="field-label">
            <span>Длительность матча, минут</span>
            <input v-model.number="sessionSettings.plannedMatchDurationMinutes" class="input" type="number" min="1" />
          </label>
          <label class="field-label">
            <span>Максимум игроков</span>
            <input v-model.number="sessionSettings.maxPlayers" class="input" type="number" min="1" />
          </label>
          <label class="field-label">
            <span>Заметки</span>
            <textarea v-model="sessionSettings.notes" class="input textarea" placeholder="Заметки"></textarea>
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
                <strong>{{ sessionPersonDisplayName(player) }}</strong>
                <p class="muted">{{ sessionPersonDetails(player) }}</p>
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
                <strong>{{ index + 1 }}. {{ sessionPersonDisplayName(entry) }}</strong>
                <p class="muted">{{ sessionPersonDetails(entry) }}</p>
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
          <label
            v-for="player in sessionPlayers"
            :key="`${team.id}-${player.playerId}`"
            class="chip team-chip"
            :class="{
              'is-assigned': isPlayerAssignedToTeam(team.id, player.playerId),
              'is-locked': isPlayerLockedForTeam(team.id, player.playerId)
            }"
            :style="teamChipStyle(team.id, player.playerId)"
          >
            <input
              type="checkbox"
              :checked="isPlayerSelectedForTeam(team.id, player.playerId)"
              :disabled="isPlayerLockedForTeam(team.id, player.playerId)"
              @change="toggleTeamPlayer(team.id, player.playerId, $event)"
            />
            <span>{{ player.firstName }}</span>
            <small v-if="isPlayerLockedForTeam(team.id, player.playerId)">{{ assignedTeamName(player.playerId) }}</small>
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
        <button class="primary-button" @click="createNextMatch">{{ createMatchButtonLabel }}</button>
      </div>
      <div v-if="session.formatType === 'ROUND_ROBIN'" class="grid-form">
        <label class="field-label">
          <span>Первый матч</span>
          <select v-model="roundRobinFirstPairKey" class="input" :disabled="matches.length > 0">
            <option v-for="pair in roundRobinPairOptions" :key="pair.key" :value="pair.key">{{ pair.label }}</option>
          </select>
        </label>
      </div>
      <div v-if="session.formatType === 'KNOCKOUT'" class="grid-form">
        <label class="field-label">
          <span>Первая команда</span>
          <select v-model.number="knockoutMatchForm.teamAId" class="input">
            <option :value="undefined">Выберите команду</option>
            <option v-for="team in session.teams" :key="team.id" :value="team.id">{{ team.name }}</option>
          </select>
        </label>
        <label class="field-label">
          <span>Вторая команда</span>
          <select v-model.number="knockoutMatchForm.teamBId" class="input">
            <option :value="undefined">Выберите команду</option>
            <option v-for="team in session.teams" :key="team.id" :value="team.id">{{ team.name }}</option>
          </select>
        </label>
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
const roundRobinFirstPairKey = ref('');
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
const knockoutMatchForm = reactive({
  teamAId: undefined as number | undefined,
  teamBId: undefined as number | undefined
});
const sessionSettings = reactive({
  title: '',
  sessionDate: '',
  sessionTime: '',
  location: '',
  locationUrl: '',
  plannedMatchDurationMinutes: 6 as number | null,
  notes: '',
  maxPlayers: 15 as number | null
});
const createMatchButtonLabel = computed(() => {
  return session.value?.formatType === 'KNOCKOUT' ? 'Создать матч' : 'Создать следующий';
});
const roundRobinPairOptions = computed(() => {
  if (!session.value) return [];
  return buildTeamPairs(session.value.teams).map(([teamA, teamB]) => ({
    key: pairKey(teamA.id, teamB.id),
    label: `${teamA.name} - ${teamB.name}`
  }));
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

function sessionPersonDisplayName(person: SessionPlayer | SessionWaitlistEntry): string {
  return person.displayName?.trim() || person.firstName;
}

function sessionPersonDetails(person: SessionPlayer | SessionWaitlistEntry): string {
  const fullName = [person.firstName, person.lastName].filter(Boolean).join(' ');
  const city = person.homeCity?.trim();
  return city ? `${fullName} (${city})` : fullName;
}

function ensureTeamSelection(teamId: number) {
  if (!selectedPlayersByTeam[teamId]) {
    selectedPlayersByTeam[teamId] = [];
  }
}

function isPlayerSelectedForTeam(teamId: number, playerId: number): boolean {
  ensureTeamSelection(teamId);
  return isPlayerAssignedToTeam(teamId, playerId) || selectedPlayersByTeam[teamId].includes(playerId);
}

function toggleTeamPlayer(teamId: number, playerId: number, event: Event) {
  void handleTeamPlayerToggle(teamId, playerId, event);
}

async function handleTeamPlayerToggle(teamId: number, playerId: number, event: Event) {
  ensureTeamSelection(teamId);
  const checked = (event.target as HTMLInputElement).checked;
  if (!checked && isPlayerAssignedToTeam(teamId, playerId)) {
    await api.removePlayerFromTeam(teamId, playerId);
    selectedPlayersByTeam[teamId] = selectedPlayersByTeam[teamId].filter((id) => id !== playerId);
    await loadTeamPlayers();
    return;
  }
  if (isPlayerLockedForTeam(teamId, playerId)) {
    (event.target as HTMLInputElement).checked = false;
    return;
  }
  const current = selectedPlayersByTeam[teamId];
  selectedPlayersByTeam[teamId] = checked
    ? [...new Set([...current, playerId])]
    : current.filter((id) => id !== playerId);
}

function assignedTeamForPlayer(playerId: number) {
  if (!session.value) return undefined;
  const teamId = Object.entries(teamPlayers.value).find(([, members]) => {
    return members.some((member) => member.playerId === playerId);
  })?.[0];
  return teamId ? session.value.teams.find((team) => team.id === Number(teamId)) : undefined;
}

function isPlayerAssignedToTeam(teamId: number, playerId: number): boolean {
  return Boolean(teamPlayers.value[teamId]?.some((member) => member.playerId === playerId));
}

function isPlayerLockedForTeam(teamId: number, playerId: number): boolean {
  const assignedTeam = assignedTeamForPlayer(playerId);
  return Boolean(assignedTeam && assignedTeam.id !== teamId);
}

function assignedTeamName(playerId: number): string {
  return assignedTeamForPlayer(playerId)?.name ?? '';
}

function teamChipStyle(teamId: number, playerId: number) {
  const assignedTeam = assignedTeamForPlayer(playerId);
  const color = assignedTeam?.color;
  if (!color) return {};
  return {
    borderColor: color,
    background: `color-mix(in srgb, ${color} 18%, white)`
  };
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
  fillSessionSettings();
  session.value.teams.forEach((team) => ensureTeamSelection(team.id));
  ensureRoundRobinPairSelection();
}

function fillSessionSettings() {
  if (!session.value) return;
  sessionSettings.title = session.value.title;
  sessionSettings.sessionDate = session.value.sessionDate;
  sessionSettings.sessionTime = session.value.sessionTime?.slice(0, 5) ?? '';
  sessionSettings.location = session.value.location ?? '';
  sessionSettings.locationUrl = session.value.locationUrl ?? '';
  sessionSettings.plannedMatchDurationMinutes = session.value.plannedMatchDurationMinutes ?? null;
  sessionSettings.notes = session.value.notes ?? '';
  sessionSettings.maxPlayers = session.value.maxPlayers ?? null;
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
  ensureRoundRobinPairSelection();
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
  if (!sessionSettings.title.trim()) {
    error.value = 'Заполните название сессии';
    return;
  }
  if (!sessionSettings.sessionDate || !sessionSettings.sessionTime) {
    error.value = 'Укажите дату и время сессии';
    return;
  }

  pendingSessionUpdate.value = true;
  error.value = '';
  try {
    session.value = await api.updateSession(sessionIdNumber.value, {
      title: sessionSettings.title.trim(),
      sessionDate: sessionSettings.sessionDate,
      sessionTime: sessionSettings.sessionTime,
      location: sessionSettings.location.trim() || null,
      locationUrl: sessionSettings.locationUrl.trim() || null,
      plannedMatchDurationMinutes: sessionSettings.plannedMatchDurationMinutes || null,
      notes: sessionSettings.notes.trim() || null,
      maxPlayers: sessionSettings.maxPlayers || null
    });
    fillSessionSettings();
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
  const ids = (selectedPlayersByTeam[teamId] ?? []).filter((playerId) => !assignedTeamForPlayer(playerId));
  if (!ids.length) return;
  try {
    await api.bulkAssignPlayers(teamId, ids);
    selectedPlayersByTeam[teamId] = [];
    await loadTeamPlayers();
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Не удалось распределить игроков по команде';
    await loadTeamPlayers();
  }
}

async function createNextMatch() {
  if (!session.value || session.value.teams.length < 2) return;

  const [teamA, teamB] = nextMatchTeams();
  if (!teamA || !teamB) {
    error.value = 'Выберите две команды для матча';
    return;
  }
  if (teamA.id === teamB.id) {
    error.value = 'Выберите разные команды';
    return;
  }

  await api.createMatch(sessionIdNumber.value, {
    teamAId: teamA.id,
    teamBId: teamB.id,
    plannedDurationMinutes: session.value.plannedMatchDurationMinutes ?? 6
  });
  knockoutMatchForm.teamAId = undefined;
  knockoutMatchForm.teamBId = undefined;
  await loadMatches();
}

function nextMatchTeams() {
  if (!session.value) return [undefined, undefined] as const;
  if (session.value.formatType === 'KNOCKOUT') {
    return [
      session.value.teams.find((team) => team.id === knockoutMatchForm.teamAId),
      session.value.teams.find((team) => team.id === knockoutMatchForm.teamBId)
    ] as const;
  }
  if (session.value.formatType === 'ROUND_ROBIN') {
    const pairs = orderedRoundRobinPairs();
    const pair = pairs[matches.value.length % pairs.length];
    return pair ?? [undefined, undefined] as const;
  }
  return [
    session.value.teams[matches.value.length % session.value.teams.length],
    session.value.teams[(matches.value.length + 1) % session.value.teams.length]
  ] as const;
}

function orderedRoundRobinPairs() {
  if (!session.value) return [];
  const pairs = buildTeamPairs(session.value.teams);
  const selectedIndex = pairs.findIndex(([teamA, teamB]) => pairKey(teamA.id, teamB.id) === roundRobinFirstPairKey.value);
  if (selectedIndex < 1) return pairs;
  return [...pairs.slice(selectedIndex), ...pairs.slice(0, selectedIndex)];
}

function buildTeamPairs(teams: GameSession['teams']) {
  const pairs: Array<[GameSession['teams'][number], GameSession['teams'][number]]> = [];
  teams.forEach((teamA, index) => {
    teams.slice(index + 1).forEach((teamB) => pairs.push([teamA, teamB]));
  });
  return pairs;
}

function pairKey(teamAId: number, teamBId: number): string {
  return [teamAId, teamBId].sort((left, right) => left - right).join(':');
}

function ensureRoundRobinPairSelection() {
  if (!session.value || session.value.formatType !== 'ROUND_ROBIN') return;
  const firstMatch = matches.value[0];
  if (firstMatch) {
    roundRobinFirstPairKey.value = pairKey(firstMatch.teamAId, firstMatch.teamBId);
    return;
  }
  const options = roundRobinPairOptions.value;
  if (!options.some((option) => option.key === roundRobinFirstPairKey.value)) {
    roundRobinFirstPairKey.value = options[0]?.key ?? '';
  }
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
