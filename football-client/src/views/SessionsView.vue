<template>
  <section class="stack">
    <div class="sessions-page-header">
      <h2 class="section-title">Сессии</h2>
      <div class="sessions-page-header__actions">
        <button class="ghost-button" @click="openVenuesDialog" :disabled="venuesPending">Поля</button>
        <button class="ghost-button" @click="loadSessions" :disabled="pending">Обновить</button>
      </div>
    </div>

    <RouterLink class="primary-button create-session-link" to="/sessions/new">Создать сессию</RouterLink>

    <div class="card">
      <p v-if="error" class="error-text">{{ error }}</p>
      <p v-else-if="isInitialLoading" class="muted">Загрузка сессий... Подождите</p>
      <p v-else-if="!sessions.length" class="muted">Пока нет сессий.</p>
      <div v-else class="list">
        <RouterLink
          v-for="session in paginatedSessions"
          :key="session.id"
          :to="`/sessions/${session.id}`"
          class="session-card"
        >
          <div class="session-card__main">
            <img
              v-if="session.venuePhotoUrl"
              class="session-card__venue-photo"
              :src="resolveMediaUrl(session.venuePhotoUrl)"
              alt="Фото поля"
            />
            <div class="session-card__text">
              <strong>{{ session.title }}</strong>
              <p class="muted session-card__date">
                {{ sessionCardDateText(session) }}<template v-if="session.sessionTime">, <strong>{{ session.sessionTime.slice(0, 5) }}</strong></template>
              </p>
              <p class="muted session-card__location">
                📍 {{ session.location || 'Место не указано' }}
              </p>
            </div>
          </div>
          <span class="status-pill" :class="sessionStatusClass(session.status)">{{ sessionStatusLabel(session.status) }}</span>
        </RouterLink>
      </div>
      <div v-if="totalPages > 1" class="pagination-row">
        <button class="ghost-button" type="button" :disabled="currentPage === 1" @click="currentPage -= 1">Назад</button>
        <span>{{ currentPage }} / {{ totalPages }}</span>
        <button class="ghost-button" type="button" :disabled="currentPage === totalPages" @click="currentPage += 1">Вперед</button>
      </div>
    </div>

    <div v-if="venuesDialogOpen" class="settings-overlay" @click.self="closeVenuesDialog">
      <div class="settings-window stack-sm">
        <div class="section-header">
          <div>
            <p class="eyebrow">Поля</p>
            <h3 class="section-title">Справочник полей</h3>
          </div>
          <button class="ghost-button" type="button" @click="closeVenuesDialog" :disabled="venueSavePending">Закрыть</button>
        </div>

        <p v-if="venuesError" class="error-text">{{ venuesError }}</p>
        <p v-if="venuesPending" class="muted">Загружаем поля...</p>
        <div v-else class="venue-manager">
          <div class="venue-manager__list">
            <button
              v-for="venue in venues"
              :key="venue.id"
              class="venue-manager__item"
              type="button"
              :class="{ 'is-active': venue.id === selectedVenueId }"
              @click="selectVenue(venue)"
            >
              <img v-if="venue.photoUrl" :src="resolveMediaUrl(venue.photoUrl)" alt="" />
              <span v-else class="venue-manager__placeholder">П</span>
              <strong>{{ venue.name }}</strong>
            </button>
            <p v-if="!venues.length" class="muted">Поля пока не добавлены.</p>
          </div>

          <form v-if="selectedVenueId" class="venue-manager__form" @submit.prevent="saveVenue">
            <label class="field-label">
              <span>Название</span>
              <input v-model="venueForm.name" class="input" :disabled="venueSavePending" />
            </label>
            <label class="field-label">
              <span>Адрес</span>
              <input v-model="venueForm.address" class="input" :disabled="venueSavePending" />
            </label>
            <label class="field-label">
              <span>Ссылка на карту</span>
              <input v-model="venueForm.gisUrl" class="input" type="url" placeholder="https://..." :disabled="venueSavePending" />
            </label>
            <label class="field-label">
              <span>Фото поля</span>
              <input class="input" type="file" accept="image/png,image/jpeg,image/webp" :disabled="venueSavePending" @change="uploadVenuePhoto" />
            </label>
            <div v-if="venueForm.photoUrl" class="venue-manager__photo-preview">
              <img :src="resolveMediaUrl(venueForm.photoUrl)" alt="Фото поля" />
            </div>
            <button class="primary-button" type="submit" :disabled="venueSavePending">Сохранить поле</button>
          </form>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { RouterLink } from 'vue-router';
import { api, resolveMediaUrl } from '../lib/api';
import { sessionStatusClass, sessionStatusLabel } from '../lib/labels';
import type { GameSession, SessionVenue } from '../types';

const sessions = ref<GameSession[]>([]);
const venues = ref<SessionVenue[]>([]);
const pending = ref(false);
const hasLoadedSessions = ref(false);
const venuesPending = ref(false);
const venueSavePending = ref(false);
const venuesDialogOpen = ref(false);
const selectedVenueId = ref<number | null>(null);
const currentPage = ref(1);
const error = ref('');
const venuesError = ref('');
const pageSize = 5;

const venueForm = reactive({
  name: '',
  address: '',
  gisUrl: '',
  photoUrl: ''
});

const totalPages = computed(() => Math.max(1, Math.ceil(sessions.value.length / pageSize)));
const isInitialLoading = computed(() => pending.value && !hasLoadedSessions.value);
const paginatedSessions = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return sessions.value.slice(start, start + pageSize);
});

watch(totalPages, (pages) => {
  if (currentPage.value > pages) {
    currentPage.value = pages;
  }
});

const shortWeekdays = ['Вс', 'Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб'];
const monthNames = [
  'января',
  'февраля',
  'марта',
  'апреля',
  'мая',
  'июня',
  'июля',
  'августа',
  'сентября',
  'октября',
  'ноября',
  'декабря'
];

async function loadSessions() {
  pending.value = true;
  error.value = '';
  try {
    sessions.value = await api.getSessions();
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Не удалось загрузить сессии';
  } finally {
    hasLoadedSessions.value = true;
    pending.value = false;
  }
}

async function openVenuesDialog() {
  venuesDialogOpen.value = true;
  await loadVenues();
}

function closeVenuesDialog() {
  if (venueSavePending.value) {
    return;
  }
  venuesDialogOpen.value = false;
  venuesError.value = '';
}

async function loadVenues() {
  venuesPending.value = true;
  venuesError.value = '';
  try {
    venues.value = await api.getSessionVenues();
    if (!selectedVenueId.value && venues.value.length) {
      selectVenue(venues.value[0]);
    } else if (selectedVenueId.value) {
      const selected = venues.value.find((venue) => venue.id === selectedVenueId.value);
      if (selected) {
        selectVenue(selected);
      }
    }
  } catch (err) {
    venuesError.value = err instanceof Error ? err.message : 'Не удалось загрузить поля';
  } finally {
    venuesPending.value = false;
  }
}

function selectVenue(venue: SessionVenue) {
  selectedVenueId.value = venue.id;
  venueForm.name = venue.name;
  venueForm.address = venue.address ?? '';
  venueForm.gisUrl = venue.gisUrl ?? '';
  venueForm.photoUrl = venue.photoUrl ?? '';
  venuesError.value = '';
}

function sessionCardDateText(session: GameSession): string {
  const date = parseSessionDate(session.sessionDate);
  if (!date) {
    return session.sessionDate || 'Дата не указана';
  }
  return `${shortWeekdays[date.getDay()]}, ${date.getDate()} ${monthNames[date.getMonth()]} ${date.getFullYear()}`;
}

function parseSessionDate(value: string): Date | null {
  const match = value.match(/^(\d{4})-(\d{2})-(\d{2})$/);
  if (!match) {
    return null;
  }
  return new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3]));
}

async function uploadVenuePhoto(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) {
    return;
  }

  venueSavePending.value = true;
  venuesError.value = '';
  try {
    const response = await api.uploadSessionVenuePhoto(file);
    venueForm.photoUrl = response.url;
  } catch (err) {
    venuesError.value = err instanceof Error ? err.message : 'Не удалось загрузить фото поля';
  } finally {
    venueSavePending.value = false;
    input.value = '';
  }
}

async function saveVenue() {
  if (!selectedVenueId.value) {
    return;
  }
  if (!venueForm.name.trim()) {
    venuesError.value = 'Укажите название поля';
    return;
  }

  venueSavePending.value = true;
  venuesError.value = '';
  try {
    const updated = await api.updateSessionVenue(selectedVenueId.value, {
      name: venueForm.name.trim(),
      address: venueForm.address.trim() || null,
      gisUrl: venueForm.gisUrl.trim() || null,
      photoUrl: venueForm.photoUrl || null
    });
    venues.value = venues.value.map((venue) => venue.id === updated.id ? updated : venue);
    selectVenue(updated);
  } catch (err) {
    venuesError.value = err instanceof Error ? err.message : 'Не удалось сохранить поле';
  } finally {
    venueSavePending.value = false;
  }
}

onMounted(loadSessions);
</script>
