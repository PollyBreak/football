<template>
  <section class="stack">
    <RouterLink class="ghost-button back-button" to="/sessions">Назад к сессиям</RouterLink>

    <div class="card stack-sm">
      <div class="section-header">
        <h2 class="section-title">Создать сессию</h2>
      </div>
      <div class="grid-form">
        <input v-model="form.title" class="input" placeholder="Название сессии" />
        <input v-model="form.sessionDate" class="input" type="date" />
        <input v-model="form.location" class="input" placeholder="Место" />
        <select v-model="form.formatType" class="input">
          <option value="ROUND_ROBIN">{{ sessionFormatLabel('ROUND_ROBIN') }}</option>
          <option value="KING_OF_THE_HILL">{{ sessionFormatLabel('KING_OF_THE_HILL') }}</option>
          <option value="CUSTOM">{{ sessionFormatLabel('CUSTOM') }}</option>
        </select>
        <label class="field-label">
          <span>Длительность матча, минут</span>
          <input v-model.number="form.plannedMatchDurationMinutes" class="input" type="number" min="1" />
        </label>
        <label class="field-label">
          <span>Максимум игроков в сессии</span>
          <input v-model.number="form.maxPlayers" class="input" type="number" min="1" />
        </label>
        <textarea v-model="form.notes" class="input textarea" placeholder="Заметки"></textarea>
      </div>
      <button class="primary-button form-submit" @click="createSession" :disabled="pending">Создать</button>
      <p v-if="error" class="error-text">{{ error }}</p>
    </div>
  </section>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { api } from '../lib/api';
import { sessionFormatLabel } from '../lib/labels';
import type { SessionFormatType } from '../types';

const router = useRouter();
const pending = ref(false);
const error = ref('');

const form = reactive({
  title: '',
  sessionDate: new Date().toISOString().slice(0, 10),
  location: '',
  formatType: 'ROUND_ROBIN' as SessionFormatType,
  plannedMatchDurationMinutes: 6,
  maxPlayers: 15,
  notes: ''
});

async function createSession() {
  pending.value = true;
  error.value = '';
  try {
    const session = await api.createSession({
      title: form.title,
      sessionDate: form.sessionDate,
      location: form.location || null,
      formatType: form.formatType,
      plannedMatchDurationMinutes: form.plannedMatchDurationMinutes,
      maxPlayers: form.maxPlayers || null,
      notes: form.notes || null,
      teams: [
        { name: 'Красные', color: 'red', displayOrder: 1 },
        { name: 'Зеленые', color: 'green', displayOrder: 2 },
        { name: 'Синие', color: 'blue', displayOrder: 3 }
      ]
    });
    await router.push(`/sessions/${session.id}`);
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Не удалось создать сессию';
  } finally {
    pending.value = false;
  }
}
</script>
