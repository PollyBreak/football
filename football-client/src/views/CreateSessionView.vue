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
        <div class="date-time-row">
          <label class="field-label" :class="{ 'is-invalid': shouldShowFieldError('sessionDate') }">
            <span>Дата игры *</span>
            <input v-model="form.sessionDate" class="input" type="date" required />
          </label>
          <label class="field-label" :class="{ 'is-invalid': shouldShowFieldError('sessionTime') }">
            <span>Время *</span>
            <input v-model="form.sessionTime" class="input" type="time" required />
          </label>
        </div>
        <label class="field-label">
          <span>Место</span>
          <select v-model="form.venueId" class="input">
            <option :value="null">Новое место / вручную</option>
            <option v-for="venue in venues" :key="venue.id" :value="venue.id">
              {{ venue.name }}
            </option>
          </select>
          <input v-model="form.location" class="input" placeholder="Место" :disabled="Boolean(form.venueId)" />
        </label>
        <label class="field-label">
          <span>Адрес поля</span>
          <input v-model="form.locationAddress" class="input" placeholder="Улица, дом" :disabled="Boolean(form.venueId)" />
        </label>
        <label class="field-label">
          <span>Ссылка на поле на 2GIS / Google Maps / Яндекс картах</span>
          <input v-model="form.locationUrl" class="input" type="url" placeholder="https://..." :disabled="Boolean(form.venueId)" />
        </label>
        <div class="field-label">
          <span>Фотография поля</span>
          <div class="venue-photo-row">
            <label class="venue-photo-button" :class="{ 'is-disabled': photoUploadDisabled }">
              <span>{{ photoUploadPending ? 'Загружаем...' : 'Выбрать файл' }}</span>
              <input class="venue-photo-input" type="file" accept="image/jpeg,image/png,image/webp" :disabled="photoUploadDisabled" @change="uploadVenuePhoto" />
            </label>
            <img v-if="form.venuePhotoUrl" class="venue-photo-preview" :src="resolveMediaUrl(form.venuePhotoUrl)" alt="Фотография поля" />
          </div>
        </div>
        <label class="reminder-checkbox">
          <span>Сохранить поле для следующих сессий</span>
          <input v-model="form.saveVenue" type="checkbox" :disabled="Boolean(form.venueId)" />
        </label>
        <label class="field-label">
          <span>Ссылка на трансляцию</span>
          <input v-model="form.broadcastUrl" class="input" type="url" placeholder="https://..." />
        </label>
        <label class="field-label">
          <span>Telegram chat ID</span>
          <input v-model.number="form.telegramChatId" class="input" type="number" placeholder="-100..." />
        </label>
        <div class="settings-group">
          <div>
            <p class="settings-group__title">Формат игры</p>
          </div>
          <label class="field-label" :class="{ 'is-invalid': shouldShowFieldError('formatType') }">
            <span>Тип турнира *</span>
            <select v-model="form.formatType" class="input" required>
              <option value="ROUND_ROBIN">{{ sessionFormatLabel('ROUND_ROBIN') }}</option>
              <option value="KNOCKOUT">{{ sessionFormatLabel('KNOCKOUT') }}</option>
            </select>
          </label>
          <div class="format-metrics-row">
            <label class="field-label">
              <span>Длительность игры, минут</span>
              <input v-model.number="form.plannedMatchDurationMinutes" class="input" type="number" min="1" />
            </label>
            <label class="field-label">
              <span>Длительность сессии, минут</span>
              <input v-model.number="form.sessionDurationMinutes" class="input" type="number" min="1" />
            </label>
            <label class="field-label">
              <span>Максимум игроков</span>
              <input v-model.number="form.maxPlayers" class="input" type="number" min="1" />
            </label>
          </div>
          <div class="field-label">
            <span>Команды</span>
            <div class="reminder-options-row">
              <label v-for="format in visiblePlayerFormatOptions" :key="format" class="reminder-checkbox">
                <span>{{ format }}</span>
                <input type="radio" name="player-format" :checked="form.playerFormat === format" @change="form.playerFormat = format" />
              </label>
              <div class="reminder-form reminder-form--compact">
                <input v-model.number="customPlayerFormatSize" class="input" type="number" min="1" placeholder="N" />
                <button class="ghost-button reminder-add-button" type="button" @click="applyCustomPlayerFormat">+</button>
              </div>
            </div>
          </div>
        </div>
        <div class="settings-group">
          <p class="settings-group__title">Оплата</p>
          <label class="field-label">
            <span>Взнос, тенге</span>
            <input v-model.number="form.feeAmount" class="input" type="number" min="0" />
          </label>
          <label class="field-label">
            <span>Кому скидывать взнос</span>
            <input v-model="form.feeRecipient" class="input" placeholder="Kaspi / имя / телефон" />
          </label>
          <div class="field-label reminder-settings reminder-settings--compact">
            <span class="reminder-settings__title">Настройте напоминания по взносам</span>
            <p class="reminder-settings__hint">Уведомления будут приходить перед игрой за ...</p>
            <div class="reminder-options-row">
              <label v-for="hours in reminderOptionHours" :key="hours" class="reminder-checkbox">
                <span>{{ hours }} ч.</span>
                <input type="checkbox" :checked="selectedReminderHours.includes(hours)" @change="toggleReminderHour(hours, ($event.target as HTMLInputElement).checked)" />
              </label>
              <div class="reminder-form reminder-form--compact">
                <input v-model.number="customReminderHoursBefore" class="input" type="number" min="1" placeholder="Свое" />
                <button class="ghost-button reminder-add-button" type="button" @click="addCustomReminderHour">+</button>
              </div>
            </div>
          </div>
        </div>
        <textarea v-model="form.notes" class="input textarea" placeholder="Заметки"></textarea>
        <button class="primary-button form-submit" type="button" @click="createSession" :disabled="pending">Создать</button>
      </form>
      <p v-if="error" class="error-text">{{ error }}</p>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { api, resolveMediaUrl } from '../lib/api';
import { sessionFormatLabel } from '../lib/labels';
import type { SessionFormatType, SessionVenue } from '../types';

const router = useRouter();
const pending = ref(false);
const error = ref('');
type RequiredField = 'title' | 'sessionDate' | 'sessionTime' | 'formatType';
const reminderQuickHours = [10, 5, 2];
const playerFormatOptions = ['5x5', '6x6', '7x7'];
const selectedReminderHours = ref<number[]>([]);
const customReminderHours = ref<number[]>([]);
const customReminderHoursBefore = ref<number | null>(null);
const customPlayerFormatOptions = ref<string[]>([]);
const customPlayerFormatSize = ref<number | null>(null);
const venues = ref<SessionVenue[]>([]);
const photoUploadPending = ref(false);
const reminderOptionHours = computed(() => {
  return Array.from(new Set([...reminderQuickHours, ...customReminderHours.value, ...selectedReminderHours.value]))
    .sort((left, right) => right - left);
});
const visiblePlayerFormatOptions = computed(() => {
  return Array.from(new Set([...playerFormatOptions, ...customPlayerFormatOptions.value, form.playerFormat]))
    .filter(Boolean);
});
const photoUploadDisabled = computed(() => {
  return (!form.saveVenue && !form.venueId) || Boolean(form.venueId) || photoUploadPending.value;
});

const form = reactive({
  title: '',
  sessionDate: new Date().toISOString().slice(0, 10),
  sessionTime: '20:00',
  venueId: null as number | null,
  location: '',
  locationAddress: '',
  locationUrl: '',
  venuePhotoUrl: '',
  saveVenue: false,
  broadcastUrl: '',
  telegramChatId: null as number | null,
  feeAmount: null as number | null,
  feeRecipient: '',
  formatType: 'ROUND_ROBIN' as SessionFormatType,
  plannedMatchDurationMinutes: 6,
  sessionDurationMinutes: 90,
  maxPlayers: 15,
  playerFormat: '6x6',
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
  if (firstError) {
    error.value = firstError;
    return false;
  }
  if (form.saveVenue && !form.venueId && !form.location.trim()) {
    error.value = 'Укажите название места, чтобы сохранить его';
    return false;
  }
  if (form.saveVenue && !form.venueId && venues.value.some((venue) => venue.name.trim().toLowerCase() === form.location.trim().toLowerCase())) {
    error.value = 'Место с таким названием уже есть. Выберите его из списка';
    return false;
  }
  error.value = '';
  return true;
}

const selectedVenue = computed(() => {
  return venues.value.find((venue) => venue.id === form.venueId) ?? null;
});

onMounted(async () => {
  try {
    venues.value = await api.getSessionVenues();
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Не удалось загрузить сохраненные места';
  }
});

watch(selectedVenue, (venue) => {
  if (!venue) {
    return;
  }
  form.location = venue.name;
  form.locationAddress = venue.address ?? '';
  form.locationUrl = venue.gisUrl ?? '';
  form.venuePhotoUrl = venue.photoUrl ?? '';
  form.saveVenue = false;
});

function toggleReminderHour(hoursBefore: number, checked: boolean) {
  if (checked) {
    if (!selectedReminderHours.value.includes(hoursBefore)) {
      selectedReminderHours.value = [...selectedReminderHours.value, hoursBefore];
    }
    return;
  }
  selectedReminderHours.value = selectedReminderHours.value.filter((value) => value !== hoursBefore);
}

function addCustomReminderHour() {
  const hoursBefore = Number(customReminderHoursBefore.value);
  if (!Number.isFinite(hoursBefore) || hoursBefore < 1) {
    error.value = 'Укажите, за сколько часов до игры напомнить';
    return;
  }
  if (!customReminderHours.value.includes(hoursBefore)) {
    customReminderHours.value = [...customReminderHours.value, hoursBefore];
  }
  toggleReminderHour(hoursBefore, true);
  customReminderHoursBefore.value = null;
  error.value = '';
}

function applyCustomPlayerFormat() {
  const size = Number(customPlayerFormatSize.value);
  if (!Number.isFinite(size) || size < 1) {
    error.value = 'Укажите размер команд';
    return;
  }
  const format = `${size}x${size}`;
  if (!customPlayerFormatOptions.value.includes(format) && !playerFormatOptions.includes(format)) {
    customPlayerFormatOptions.value = [...customPlayerFormatOptions.value, format];
  }
  form.playerFormat = format;
  customPlayerFormatSize.value = null;
  error.value = '';
}

async function uploadVenuePhoto(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) {
    return;
  }

  photoUploadPending.value = true;
  error.value = '';
  try {
    const response = await api.uploadSessionVenuePhoto(file);
    form.venuePhotoUrl = response.url;
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Не удалось загрузить фото поля';
    input.value = '';
  } finally {
    photoUploadPending.value = false;
  }
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
      venueId: form.venueId,
      location: form.location || null,
      locationAddress: form.locationAddress || null,
      locationUrl: form.locationUrl || null,
      saveVenue: form.saveVenue,
      venuePhotoUrl: form.venuePhotoUrl || null,
      broadcastUrl: form.broadcastUrl || null,
      telegramChatId: form.telegramChatId || null,
      feeAmount: form.feeAmount || null,
      feeRecipient: form.feeRecipient || null,
      formatType: form.formatType,
      plannedMatchDurationMinutes: form.plannedMatchDurationMinutes,
      sessionDurationMinutes: form.sessionDurationMinutes || null,
      maxPlayers: form.maxPlayers || null,
      playerFormat: form.playerFormat || null,
      notes: form.notes || null,
      teams: [
        { name: 'Красные', color: 'red', displayOrder: 1 },
        { name: 'Зеленые', color: 'green', displayOrder: 2 },
        { name: 'Синие', color: 'blue', displayOrder: 3 }
      ]
    });
    await Promise.all(
      selectedReminderHours.value.map((hoursBefore) => api.createContributionReminder(session.id, { hoursBefore }))
    );
    await router.push(`/sessions/${session.id}`);
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Не удалось создать сессию';
  } finally {
    pending.value = false;
  }
}
</script>
