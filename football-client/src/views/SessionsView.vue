<template>
  <section class="stack">
    <div class="card">
      <div class="section-header">
        <div>
          <p class="eyebrow">Быстрый доступ</p>
          <h2 class="section-title">Сессии</h2>
        </div>
        <button class="ghost-button" @click="loadSessions" :disabled="pending">Обновить</button>
      </div>
      <p class="muted">Создавайте игровые сессии и открывайте их как отдельные игровые пространства.</p>
    </div>

    <RouterLink class="primary-button create-session-link" to="/sessions/new">Создать сессию</RouterLink>

    <div class="card">
      <p v-if="error" class="error-text">{{ error }}</p>
      <p v-if="!sessions.length && !error" class="muted">Пока нет сессий.</p>
      <div v-else class="list">
        <RouterLink
          v-for="session in sessions"
          :key="session.id"
          :to="`/sessions/${session.id}`"
          class="session-card"
        >
          <div>
            <strong>{{ session.title }}</strong>
            <p class="muted">{{ session.sessionDate }} • {{ session.location || 'Место не указано' }}</p>
          </div>
          <span class="status-pill">{{ sessionStatusLabel(session.status) }}</span>
        </RouterLink>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { api } from '../lib/api';
import { sessionStatusLabel } from '../lib/labels';
import type { GameSession } from '../types';

const sessions = ref<GameSession[]>([]);
const pending = ref(false);
const error = ref('');

async function loadSessions() {
  pending.value = true;
  error.value = '';
  try {
    sessions.value = await api.getSessions();
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Не удалось загрузить сессии';
  } finally {
    pending.value = false;
  }
}

onMounted(loadSessions);
</script>
