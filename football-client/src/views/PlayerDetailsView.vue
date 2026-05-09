<template>
  <section class="stack">
    <RouterLink class="ghost-button back-button" to="/players">Назад к игрокам</RouterLink>

    <div v-if="player" class="card stack-sm">
      <div class="profile-summary">
        <div>
          <p class="eyebrow">Игрок</p>
          <h2 class="section-title">{{ player.firstName }} {{ player.lastName ?? '' }}</h2>
          <p class="muted">{{ player.nickname || player.displayName || 'Без никнейма' }}</p>
        </div>
        <div class="profile-photo" aria-label="Фото игрока">
          <img v-if="playerPhotoUrl" :src="playerPhotoUrl" alt="Фото игрока" />
          <span v-else>{{ playerInitials }}</span>
        </div>
      </div>

      <div class="profile-details">
        <div>
          <span class="muted">Позиция</span>
          <strong>{{ playerPositionLabel(player.defaultPosition) }}</strong>
        </div>
        <div>
          <span class="muted">Город</span>
          <strong>{{ player.homeCity || 'Не указан' }}</strong>
        </div>
        <div>
          <span class="muted">Telegram</span>
          <strong>{{ player.username ? `@${player.username}` : 'Не указан' }}</strong>
        </div>
        <div>
          <span class="muted">Дата рождения</span>
          <strong>{{ player.birthDate || 'Не указана' }}</strong>
        </div>
      </div>
    </div>

    <p v-if="error" class="error-text">{{ error }}</p>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { api } from '../lib/api';
import { authState } from '../lib/auth';
import { playerPositionLabel } from '../lib/labels';
import type { PlayerProfile } from '../types';

const props = defineProps<{ playerId: string }>();

const player = ref<PlayerProfile | null>(null);
const error = ref('');

const playerPhotoUrl = computed(() => {
  if (!player.value) return '';
  if (authState.player?.playerId === player.value.playerId) {
    return authState.user?.photoUrl ?? player.value.photoUrl ?? '';
  }
  return player.value.photoUrl ?? '';
});

const playerInitials = computed(() => {
  if (!player.value) return 'И';
  return [player.value.firstName, player.value.lastName]
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part?.[0])
    .join('') || 'И';
});

onMounted(async () => {
  try {
    player.value = await api.getPlayer(Number(props.playerId));
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Не удалось загрузить игрока';
  }
});
</script>
