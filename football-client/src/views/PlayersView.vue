<template>
  <section class="stack">
    <div class="card stack-sm">
      <div class="section-header players-header">
        <h2 class="section-title">Leaderboard</h2>
        <button class="ghost-button refresh-button" @click="loadPlayers" :disabled="pending">Обновить</button>
      </div>

      <div v-if="leaderboardPlayers.length" class="players-leaderboard">
        <RouterLink
          v-for="entry in leaderboardPlayers"
          :key="entry.player.playerId"
          :to="`/players/${entry.player.playerId}`"
          class="leaderboard-card"
          :class="`leaderboard-card--${entry.rank}`"
        >
          <span class="leaderboard-card__medal">{{ leaderboardMedal(entry.rank) }}</span>
          <div class="player-avatar leaderboard-card__avatar">
            <img v-if="playerPhotoUrl(entry.player)" :src="playerPhotoUrl(entry.player)" alt="Фото игрока" />
            <span v-else>{{ playerInitials(entry.player) }}</span>
          </div>
          <strong>{{ playerDisplayName(entry.player) }}</strong>
          <span class="leaderboard-card__name">{{ playerShortFullName(entry.player) }}</span>
          <p>{{ entry.player.stats.goals }} ⚽ {{ entry.player.stats.assists }} 👟</p>
        </RouterLink>
      </div>

      <div class="leaderboard-sort">
        <button
          class="ghost-button"
          type="button"
          :class="{ 'is-active': sortMode === 'goals' }"
          @click="sortMode = 'goals'"
        >
          Голы
        </button>
        <button
          class="ghost-button"
          type="button"
          :class="{ 'is-active': sortMode === 'assists' }"
          @click="sortMode = 'assists'"
        >
          Ассисты
        </button>
      </div>

      <div class="search-row">
        <input v-model="searchDraft" class="input" placeholder="Поиск игрока" @keyup.enter="applySearch" />
        <button class="primary-button search-button" @click="applySearch" :disabled="pending">Найти</button>
      </div>

      <p v-if="error" class="error-text">{{ error }}</p>
      <p v-else-if="isInitialLoading" class="muted">Загрузка игроков... Подождите</p>
      <p v-else-if="!paginatedPlayers.length" class="muted">Игроки не найдены.</p>

      <div v-else class="list">
        <RouterLink
          v-for="(player, index) in paginatedPlayers"
          :key="player.playerId"
          :to="`/players/${player.playerId}`"
          class="list-item player-list-item"
        >
          <div class="list-item__lead">
            <span class="player-rank">{{ playerListRank(index) }}</span>
            <div class="player-avatar player-avatar--sm">
              <img v-if="playerPhotoUrl(player)" :src="playerPhotoUrl(player)" alt="Фото игрока" />
              <span v-else>{{ playerInitials(player) }}</span>
            </div>
            <div>
              <strong>{{ playerDisplayName(player) }}</strong>
              <p class="muted player-subline">
                <span>{{ playerFullName(player) }}</span>
                <span class="player-stats-chip">{{ player.stats.goals }} ⚽ {{ player.stats.assists }} 👟</span>
              </p>
            </div>
          </div>
          <div class="item-meta">
            <span>{{ playerPositionLabel(player.defaultPosition) }}</span>
            <span class="player-city">{{ player.homeCity || 'Город не указан' }}</span>
          </div>
        </RouterLink>
      </div>

      <div v-if="totalPages > 1" class="pagination">
        <button class="pagination-button" @click="goToPage(currentPage - 1)" :disabled="currentPage === 1" aria-label="Предыдущая страница">‹</button>
        <span class="pagination-page">{{ currentPage }}</span>
        <button class="pagination-button" @click="goToPage(currentPage + 1)" :disabled="currentPage === totalPages" aria-label="Следующая страница">›</button>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { RouterLink } from 'vue-router';
import { api, resolveMediaUrl } from '../lib/api';
import { authState } from '../lib/auth';
import { playerPositionLabel } from '../lib/labels';
import type { PlayerProfile } from '../types';

const pageSize = 15;
const players = ref<PlayerProfile[]>([]);
const pending = ref(false);
const isInitialLoading = ref(true);
const error = ref('');
const searchDraft = ref('');
const searchQuery = ref('');
const currentPage = ref(1);
const sortMode = ref<'goals' | 'assists'>('goals');

const filteredPlayers = computed(() => {
  const query = searchQuery.value.trim().toLowerCase();
  const source = [...players.value].sort(comparePlayers);
  if (!query) return source;

  return source.filter((player) => {
    const fields = [
      player.firstName,
      player.lastName,
      player.displayName,
      player.nickname,
      player.username,
      player.homeCity
    ];

    return fields.some((field) => field?.toLowerCase().includes(query));
  });
});

const leaderboardPlayers = computed(() => {
  return [...players.value]
    .filter((player) => player.stats.goals > 0 || player.stats.assists > 0)
    .sort(comparePlayers)
    .slice(0, 3)
    .map((player, index) => ({ player, rank: index + 1 }));
});

const totalPages = computed(() => Math.max(1, Math.ceil(filteredPlayers.value.length / pageSize)));
const paginatedPlayers = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return filteredPlayers.value.slice(start, start + pageSize);
});

function comparePlayers(left: PlayerProfile, right: PlayerProfile): number {
  const primaryKey = sortMode.value;
  const secondaryKey = primaryKey === 'goals' ? 'assists' : 'goals';
  return right.stats[primaryKey] - left.stats[primaryKey]
    || right.stats[secondaryKey] - left.stats[secondaryKey]
    || playerDisplayName(left).localeCompare(playerDisplayName(right));
}

function playerDisplayName(player: PlayerProfile): string {
  return player.displayName?.trim()
    || player.nickname?.trim()
    || `${player.firstName} ${player.lastName ?? ''}`.trim()
    || 'Игрок';
}

function playerFullName(player: PlayerProfile): string {
  return `${player.firstName} ${player.lastName ?? ''}`.trim() || 'Имя не указано';
}

function playerShortFullName(player: PlayerProfile): string {
  const firstName = player.firstName?.trim();
  const lastName = player.lastName?.trim();
  if (firstName && lastName) return `${firstName} ${lastName[0]}.`;
  return firstName || lastName || 'Имя не указано';
}

function playerPhotoUrl(player: PlayerProfile): string {
  if (player.manualPhotoUrl) {
    return resolveMediaUrl(player.manualPhotoUrl);
  }
  if (authState.player?.playerId === player.playerId) {
    return resolveMediaUrl(authState.user?.photoUrl ?? player.photoUrl);
  }

  return resolveMediaUrl(player.photoUrl);
}

function playerInitials(player: PlayerProfile): string {
  return playerDisplayName(player)
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0])
    .join('') || 'И';
}

function leaderboardMedal(rank: number): string {
  if (rank === 1) return '🥇';
  if (rank === 2) return '🥈';
  return '🥉';
}

function applySearch() {
  searchQuery.value = searchDraft.value;
  currentPage.value = 1;
}

function goToPage(page: number) {
  currentPage.value = Math.min(Math.max(page, 1), totalPages.value);
}

function playerListRank(index: number): number {
  return (currentPage.value - 1) * pageSize + index + 1;
}

async function loadPlayers() {
  pending.value = true;
  error.value = '';
  try {
    players.value = await api.getPlayers();
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Не удалось загрузить игроков';
  } finally {
    pending.value = false;
    isInitialLoading.value = false;
  }
}

watch(filteredPlayers, () => {
  if (currentPage.value > totalPages.value) {
    currentPage.value = totalPages.value;
  }
});

watch(sortMode, () => {
  currentPage.value = 1;
});

onMounted(loadPlayers);
</script>
