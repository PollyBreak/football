<template>
  <section class="stack">
    <RouterLink class="ghost-button back-button" to="/sessions">Назад к сессиям</RouterLink>

    <div class="card stack-sm">
      <div class="section-header">
        <h2 class="section-title">Создать сессию</h2>
      </div>
      <form id="create-session-form" class="grid-form" novalidate @submit.prevent="createSession">
        <label class="field-label" :class="{ 'is-invalid': shouldShowFieldError('title') }">
          <span>Название сессии *</span>
          <input v-model="form.title" class="input" placeholder="Название сессии" required />
        </label>
        <label class="field-label" :class="{ 'is-invalid': shouldShowFieldError('sessionDate') }">
          <span>Дата сессии *</span>
          <input v-model="form.sessionDate" class="input" type="date" required />
        </label>
        <label class="field-label" :class="{ 'is-invalid': shouldShowFieldError('sessionTime') }">
          <span>Время сессии *</span>
          <input v-model="form.sessionTime" class="input" type="time" required />
        </label>
        <input v-model="form.location" class="input" placeholder="Место" />
        <label class="field-label">
          <span>Ссылка на поле на 2GIS / Google Maps / Яндекс картах</span>
          <input v-model="form.locationUrl" class="input" type="url" placeholder="https://..." />
        </label>
        <label class="field-label">
          <span>Ссылка на трансляцию</span>
          <input v-model="form.broadcastUrl" class="input" type="url" placeholder="https://..." />
        </label>
        <label class="field-label" :class="{ 'is-invalid': shouldShowFieldError('formatType') }">
          <span>Формат игр *</span>
          <select v-model="form.formatType" class="input" required>
            <option value="ROUND_ROBIN">{{ sessionFormatLabel('ROUND_ROBIN') }}</option>
            <option value="KNOCKOUT">{{ sessionFormatLabel('KNOCKOUT') }}</option>
          </select>
        </label>
        <label class="field-label">
          <span>Длительность матча, минут</span>
          <input v-model.number="form.plannedMatchDurationMinutes" class="input" type="number" min="1" />
        </label>
        <label class="field-label">
          <span>Максимум игроков в сессии</span>
          <input v-model.number="form.maxPlayers" class="input" type="number" min="1" />
        </label>
        <textarea v-model="form.notes" class="input textarea" placeholder="Заметки"></textarea>
        <button class="primary-button form-submit" type="button" @click="createSession" :disabled="pending">Создать</button>
      </form>
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
type RequiredField = 'title' | 'sessionDate' | 'sessionTime' | 'formatType';

const form = reactive({
  title: '',
  sessionDate: new Date().toISOString().slice(0, 10),
  sessionTime: '20:00',
  location: '',
  locationUrl: '',
  broadcastUrl: '',
  formatType: 'ROUND_ROBIN' as SessionFormatType,
  plannedMatchDurationMinutes: 6,
  maxPlayers: 15,
  notes: ''
});

function fieldError(field: RequiredField): string {
  if (field === 'title' && !form.title.trim()) return 'Заполните название сессии';
  if (field === 'sessionDate' && !form.sessionDate) return 'Укажите дату сессии';
  if (field === 'sessionTime' && !form.sessionTime) return 'Укажите время сессии';
  if (field === 'formatType' && !form.formatType) return 'Выберите формат игр';
  return '';
}

function shouldShowFieldError(field: RequiredField): boolean {
  return Boolean(fieldError(field));
}

function validateForm(): boolean {
  const requiredFields: RequiredField[] = ['title', 'sessionDate', 'sessionTime', 'formatType'];
  const firstError = requiredFields
    .map((field) => fieldError(field))
    .find(Boolean);
  error.value = firstError || '';
  return !firstError;
}

async function createSession() {
  if (!validateForm()) {
    return;
  }

  pending.value = true;
  error.value = '';
  try {
    const session = await api.createSession({
      title: form.title,
      sessionDate: form.sessionDate,
      sessionTime: form.sessionTime,
      location: form.location || null,
      locationUrl: form.locationUrl || null,
      broadcastUrl: form.broadcastUrl || null,
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
