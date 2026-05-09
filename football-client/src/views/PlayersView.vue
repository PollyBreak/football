<template>
  <section class="stack">
    <div class="card stack-sm">
      <div class="section-header players-header">
        <h2 class="section-title">Игроки</h2>
        <button class="ghost-button refresh-button" @click="loadPlayers" :disabled="pending">Обновить</button>
      </div>

      <div class="search-row">
        <input v-model="searchDraft" class="input" placeholder="Поиск игрока" @keyup.enter="applySearch" />
        <button class="primary-button search-button" @click="applySearch" :disabled="pending">Найти</button>
      </div>

      <p v-if="error" class="error-text">{{ error }}</p>
      <p v-if="!paginatedPlayers.length && !error" class="muted">Игроки не найдены.</p>

      <div v-else class="list">
        <RouterLink
          v-for="player in paginatedPlayers"
          :key="player.playerId"
          :to="`/players/${player.playerId}`"
          class="list-item player-list-item"
        >
          <div class="list-item__lead">
            <div class="player-avatar player-avatar--sm">
              <img v-if="playerPhotoUrl(player)" :src="playerPhotoUrl(player)" alt="Фото игрока" />
              <span v-else>{{ playerInitials(player) }}</span>
            </div>
            <div>
              <strong>{{ player.firstName }} {{ player.lastName ?? '' }}</strong>
              <p class="muted">{{ player.nickname || player.displayName || 'Без никнейма' }}</p>
            </div>
          </div>
          <div class="item-meta">
            <span>{{ playerPositionLabel(player.defaultPosition) }}</span>
            <span>{{ player.homeCity || 'Город не указан' }}</span>
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
import { api } from '../lib/api';
import { authState } from '../lib/auth';
import { playerPositionLabel } from '../lib/labels';
import type { PlayerProfile } from '../types';

const pageSize = 15;
const players = ref<PlayerProfile[]>([]);
const pending = ref(false);
const error = ref('');
const searchDraft = ref('');
const searchQuery = ref('');
const currentPage = ref(1);

const filteredPlayers = computed(() => {
  const query = searchQuery.value.trim().toLowerCase();
  if (!query) {
    return players.value;
  }

  return players.value.filter((player) => {
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

const totalPages = computed(() => Math.max(1, Math.ceil(filteredPlayers.value.length / pageSize)));
const paginatedPlayers = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return filteredPlayers.value.slice(start, start + pageSize);
});

function playerPhotoUrl(player: PlayerProfile): string {
  if (authState.player?.playerId === player.playerId) {
    return authState.user?.photoUrl ?? player.photoUrl ?? '';
  }

  return player.photoUrl ?? '';
}

function playerInitials(player: PlayerProfile): string {
  return [player.firstName, player.lastName]
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part?.[0])
    .join('') || 'И';
}

function applySearch() {
  searchQuery.value = searchDraft.value;
  currentPage.value = 1;
}

function goToPage(page: number) {
  currentPage.value = Math.min(Math.max(page, 1), totalPages.value);
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
  }
}

watch(filteredPlayers, () => {
  if (currentPage.value > totalPages.value) {
    currentPage.value = totalPages.value;
  }
});

onMounted(loadPlayers);
</script>
