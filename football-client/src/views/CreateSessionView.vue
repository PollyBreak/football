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

        <div class="settings-group">
          <p class="settings-group__title">Место</p>

          <label class="field-label">
            <span>Поле</span>
            <select v-model="venueSelection" class="input">
              <option value="">Выбрать</option>
              <option v-for="venue in venues" :key="venue.id" :value="String(venue.id)">
                {{ venue.name }}
              </option>
              <option value="new">Создать новое поле</option>
            </select>
          </label>

          <template v-if="isCreatingNewVenue">
            <label class="field-label">
              <span>Название поля</span>
              <input v-model="form.location" class="input" placeholder="Место" />
            </label>

            <label class="field-label">
              <span>Адрес поля</span>
              <input v-model="form.locationAddress" class="input" placeholder="Улица, дом" />
            </label>

            <label class="field-label">
              <span>Ссылка на поле на 2GIS / Google Maps / Яндекс картах</span>
              <input v-model="form.locationUrl" class="input" type="url" placeholder="https://..." />
            </label>

            <div class="field-label">
              <span>Фотография поля</span>
              <div class="venue-photo-row">
                <label class="venue-photo-button" :class="{ 'is-disabled': photoUploadDisabled }">
                  <span>{{ photoUploadPending ? 'Загружаем...' : 'Выбрать файл' }}</span>
                  <input
                    class="venue-photo-input"
                    type="file"
                    accept="image/jpeg,image/png,image/webp"
                    :disabled="photoUploadDisabled"
                    @change="uploadVenuePhoto"
                  />
                </label>
                <img
                  v-if="form.venuePhotoUrl"
                  class="venue-photo-preview"
                  :src="resolveMediaUrl(form.venuePhotoUrl)"
                  alt="Фотография поля"
                />
              </div>
            </div>

            <label class="reminder-checkbox">
              <span>Сохранить поле для следующих сессий</span>
              <input v-model="form.saveVenue" type="checkbox" />
            </label>
          </template>
        </div>

        <label class="field-label">
          <span>Ссылка на трансляцию</span>
          <input v-model="form.broadcastUrl" class="input" type="url" placeholder="https://..." />
        </label>

        <div class="settings-group">
          <p class="settings-group__title">Формат игры</p>

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
          <p class="settings-group__title">Telegram чат</p>

          <label class="field-label">
            <span>Чат</span>
            <select v-model="telegramChatSelection" class="input" :disabled="telegramChatsPending">
              <option value="">Не указывать</option>
              <option v-for="chat in telegramChats" :key="chat.chatId" :value="String(chat.chatId)">
                {{ formatTelegramChatOption(chat) }}
              </option>
              <option value="manual">Ввести chat ID вручную</option>
            </select>

            <input
              v-if="telegramChatSelection === 'manual'"
              v-model.number="form.telegramChatId"
              class="input"
              type="number"
              placeholder="-100..."
            />

            <p v-else-if="telegramChatsPending" class="muted">Загружаем доступные Telegram-чаты...</p>
            <p v-else-if="!telegramChats.length" class="muted">
              Пока бот не видит общих чатов. Можно ввести `chat ID` вручную или сначала добавить бота в нужную группу.
            </p>
          </label>

          <label class="reminder-checkbox">
            <input v-model="form.autoStartRegistration" type="checkbox" />
            <span>Автоматически начать регистрацию на игру</span>
          </label>

          <label v-if="form.autoStartRegistration" class="field-label registration-delay-field">
            <span>Регистрация начнется за</span>
            <div class="reminder-form registration-delay-form">
              <input
                v-model.number="form.registrationOpenDaysBefore"
                class="input"
                type="number"
                min="1"
                step="1"
              />
              <span class="muted registration-delay-form__suffix">дня/дней до игры</span>
            </div>
          </label>

          <label class="reminder-checkbox">
            <input v-model="form.autoStartContributionCollection" type="checkbox" />
            <span>Автоматически начать сбор оплаты за 2 дня до игры</span>
          </label>
        </div>

        <div class="settings-group">
          <label class="reminder-checkbox recurring-event-toggle">
            <input v-model="form.mvpVotingEnabled" type="checkbox" />
            <span>Голосование за MVP</span>
          </label>

          <template v-if="form.mvpVotingEnabled">
            <label class="field-label">
              <span>Сколько часов после игры длится голосование?</span>
              <input v-model.number="form.mvpVotingDurationHours" class="input" type="number" min="1" />
            </label>

            <div class="field-label">
              <span>Участвовать в голосовании могут</span>
              <div class="reminder-options-row">
                <label class="reminder-checkbox">
                  <span>Все</span>
                  <input type="radio" name="mvp-voting-scope" value="ALL" :checked="form.mvpVotingParticipantScope === 'ALL'" @change="form.mvpVotingParticipantScope = 'ALL'" />
                </label>
                <label class="reminder-checkbox">
                  <span>Только те, кто играл</span>
                  <input type="radio" name="mvp-voting-scope" value="PLAYERS_ONLY" :checked="form.mvpVotingParticipantScope === 'PLAYERS_ONLY'" @change="form.mvpVotingParticipantScope = 'PLAYERS_ONLY'" />
                </label>
              </div>
            </div>

            <label class="reminder-checkbox" :class="{ 'is-disabled': !form.telegramChatId }">
              <input v-model="form.mvpVotingTelegramEnabled" type="checkbox" :disabled="!form.telegramChatId" />
              <span>Делать рассылку в чате</span>
            </label>
          </template>
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
                <input
                  type="checkbox"
                  :checked="selectedReminderHours.includes(hours)"
                  @change="toggleReminderHour(hours, ($event.target as HTMLInputElement).checked)"
                />
              </label>

              <div class="reminder-form reminder-form--compact">
                <input v-model.number="customReminderHoursBefore" class="input" type="number" min="1" placeholder="Свое" />
                <button class="ghost-button reminder-add-button" type="button" @click="addCustomReminderHour">+</button>
              </div>
            </div>
          </div>
        </div>

        <div class="settings-group">
          <label class="reminder-checkbox recurring-event-toggle">
            <input v-model="form.recurringEnabled" type="checkbox" />
            <span>Повторяющееся</span>
          </label>

          <template v-if="form.recurringEnabled">
            <label class="reminder-checkbox">
              <span>Повторяется раз в днях</span>
              <input type="radio" name="recurring-mode" :checked="form.recurringMode === 'days'" @change="form.recurringMode = 'days'" />
            </label>

            <label class="field-label" :class="{ 'is-disabled': form.recurringMode !== 'days' }">
              <span>Повторяется раз в</span>
              <div class="reminder-form recurring-event-form">
                <input
                  v-model.number="form.recurringEveryDays"
                  class="input"
                  type="number"
                  min="1"
                  placeholder="1"
                  :disabled="form.recurringMode !== 'days'"
                />
                <span class="muted recurring-event-form__suffix">дня / дней</span>
              </div>
            </label>

            <label class="reminder-checkbox">
              <span>Повторяется раз в месяц</span>
              <input type="radio" name="recurring-mode" :checked="form.recurringMode === 'month'" @change="form.recurringMode = 'month'" />
            </label>

            <label class="field-label" :class="{ 'is-disabled': form.recurringMode !== 'month' }">
              <span>Повторяется раз в месяц</span>
              <div class="reminder-form recurring-event-form">
                <input
                  v-model.number="form.recurringDayOfMonth"
                  class="input"
                  type="number"
                  min="1"
                  max="31"
                  placeholder="15"
                  :disabled="form.recurringMode !== 'month'"
                />
                <span class="muted recurring-event-form__suffix">числа</span>
              </div>
            </label>
          </template>
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
import { authState } from '../lib/auth';
import { api, resolveMediaUrl } from '../lib/api';
import { sessionFormatLabel } from '../lib/labels';
import type { SessionFormatType, SessionVenue, TelegramKnownChat } from '../types';

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
const venueSelection = ref('');
const telegramChats = ref<TelegramKnownChat[]>([]);
const telegramChatsPending = ref(false);
const telegramChatSelection = ref('');
const photoUploadPending = ref(false);

const isCreatingNewVenue = computed(() => venueSelection.value === 'new');

const reminderOptionHours = computed(() => {
  return Array.from(new Set([...reminderQuickHours, ...customReminderHours.value, ...selectedReminderHours.value]))
    .sort((left, right) => right - left);
});

const visiblePlayerFormatOptions = computed(() => {
  return Array.from(new Set([...playerFormatOptions, ...customPlayerFormatOptions.value, form.playerFormat])).filter(Boolean);
});

const photoUploadDisabled = computed(() => !isCreatingNewVenue.value || photoUploadPending.value);

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
  telegramChatTitle: '',
  autoStartRegistration: false,
  registrationOpenDaysBefore: 5,
  autoStartContributionCollection: false,
  mvpVotingEnabled: false,
  mvpVotingDurationHours: 24,
  mvpVotingParticipantScope: 'ALL' as 'ALL' | 'PLAYERS_ONLY',
  mvpVotingTelegramEnabled: false,
  recurringEnabled: false,
  recurringMode: null as 'days' | 'month' | null,
  recurringEveryDays: null as number | null,
  recurringDayOfMonth: null as number | null,
  feeAmount: null as number | null,
  feeRecipient: '',
  formatType: 'ROUND_ROBIN' as SessionFormatType,
  plannedMatchDurationMinutes: 7,
  sessionDurationMinutes: 90,
  maxPlayers: 18,
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
  const firstError = requiredFields.map((field) => fieldError(field)).find(Boolean);
  if (firstError) {
    error.value = firstError;
    return false;
  }

  if (isCreatingNewVenue.value && form.saveVenue && !form.location.trim()) {
    error.value = 'Укажите название места, чтобы сохранить его';
    return false;
  }

  if (
    isCreatingNewVenue.value &&
    form.saveVenue &&
    venues.value.some((venue) => venue.name.trim().toLowerCase() === form.location.trim().toLowerCase())
  ) {
    error.value = 'Место с таким названием уже есть. Выберите его из списка';
    return false;
  }

  if (form.recurringEnabled && form.recurringMode == null) {
    error.value = 'Выберите вариант повторения';
    return false;
  }

  if (form.recurringMode === 'days' && (!form.recurringEveryDays || form.recurringEveryDays < 1)) {
    error.value = 'Укажите, через сколько дней повторять событие';
    return false;
  }

  if (form.recurringMode === 'month' && (!form.recurringDayOfMonth || form.recurringDayOfMonth < 1 || form.recurringDayOfMonth > 31)) {
    error.value = 'Укажите корректное число месяца';
    return false;
  }

  error.value = '';
  return true;
}

const selectedVenue = computed(() => {
  return venues.value.find((venue) => venue.id === form.venueId) ?? null;
});

onMounted(async () => {
  telegramChatsPending.value = Boolean(authState.user?.id);
  try {
    const [loadedVenues, loadedTelegramChats] = await Promise.all([
      api.getSessionVenues(),
      authState.user?.id ? api.getAvailableTelegramChats(authState.user.id) : Promise.resolve([])
    ]);
    venues.value = loadedVenues;
    telegramChats.value = loadedTelegramChats;
    if (!loadedTelegramChats.length) {
      telegramChatSelection.value = 'manual';
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Не удалось загрузить сохраненные места';
  } finally {
    telegramChatsPending.value = false;
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

watch(venueSelection, (value) => {
  if (value === 'new') {
    form.venueId = null;
    form.location = '';
    form.locationAddress = '';
    form.locationUrl = '';
    form.venuePhotoUrl = '';
    form.saveVenue = false;
    return;
  }

  if (!value) {
    form.venueId = null;
    form.location = '';
    form.locationAddress = '';
    form.locationUrl = '';
    form.venuePhotoUrl = '';
    form.saveVenue = false;
    return;
  }

  const venueId = Number(value);
  form.venueId = Number.isFinite(venueId) ? venueId : null;
});

watch(telegramChatSelection, (value) => {
  if (value === 'manual') {
    form.telegramChatId = null;
    form.telegramChatTitle = '';
    return;
  }

  if (!value) {
    form.telegramChatId = null;
    form.telegramChatTitle = '';
    return;
  }

  const chatId = Number(value);
  const selectedChat = telegramChats.value.find((chat) => chat.chatId === chatId);
  form.telegramChatId = Number.isFinite(chatId) ? chatId : null;
  form.telegramChatTitle = selectedChat?.title ?? '';
});

watch(
  () => form.telegramChatId,
  (chatId) => {
    if (!chatId) {
      form.mvpVotingTelegramEnabled = false;
    }
  }
);

watch(
  () => form.recurringEnabled,
  (enabled) => {
    if (enabled) {
      return;
    }
    form.recurringMode = null;
    form.recurringEveryDays = null;
    form.recurringDayOfMonth = null;
  }
);

watch(
  () => form.recurringMode,
  (mode) => {
    if (mode === 'days') {
      form.recurringDayOfMonth = null;
      return;
    }

    if (mode === 'month') {
      form.recurringEveryDays = null;
      return;
    }

    form.recurringEveryDays = null;
    form.recurringDayOfMonth = null;
  }
);

function toggleReminderHour(hoursBefore: number, checked: boolean) {
  if (checked) {
    if (!selectedReminderHours.value.includes(hoursBefore)) {
      selectedReminderHours.value = [...selectedReminderHours.value, hoursBefore];
    }
    return;
  }
  selectedReminderHours.value = selectedReminderHours.value.filter((value) => value !== hoursBefore);
}

function formatTelegramChatOption(chat: TelegramKnownChat): string {
  const title = chat.title?.trim();
  const username = chat.username?.trim();
  if (title && username) {
    return `${title} (@${username})`;
  }
  if (title) {
    return title;
  }
  if (username) {
    return `@${username}`;
  }
  return `Chat ${chat.chatId}`;
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

  if (form.autoStartRegistration && !authState.user?.id) {
    error.value = 'Откройте приложение через Telegram Mini App';
    return;
  }

  if ((form.autoStartRegistration || form.autoStartContributionCollection) && !form.telegramChatId) {
    error.value = 'Укажите Telegram чат для автоматических действий';
    return;
  }

  if (
    form.autoStartRegistration &&
    (!Number.isFinite(form.registrationOpenDaysBefore) || form.registrationOpenDaysBefore < 1)
  ) {
    error.value = 'Укажите, за сколько дней до игры начинать регистрацию';
    return;
  }

  if (form.mvpVotingEnabled && (!Number.isFinite(form.mvpVotingDurationHours) || form.mvpVotingDurationHours < 1)) {
    error.value = 'Укажите, сколько часов длится голосование за MVP';
    return;
  }

  if (form.mvpVotingEnabled && form.mvpVotingTelegramEnabled && !form.telegramChatId) {
    error.value = 'Укажите Telegram чат для рассылки голосования за MVP';
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
      saveVenue: isCreatingNewVenue.value ? form.saveVenue : false,
      venuePhotoUrl: form.venuePhotoUrl || null,
      broadcastUrl: form.broadcastUrl || null,
      telegramChatId: form.telegramChatId || null,
      telegramChatTitle: form.telegramChatTitle || null,
      autoStartRegistration: form.autoStartRegistration,
      registrationOpenHoursBefore: form.autoStartRegistration ? form.registrationOpenDaysBefore * 24 : null,
      autoStartContributionCollection: form.autoStartContributionCollection,
      mvpVotingEnabled: form.mvpVotingEnabled,
      mvpVotingDurationHours: form.mvpVotingEnabled ? form.mvpVotingDurationHours : null,
      mvpVotingParticipantScope: form.mvpVotingParticipantScope,
      mvpVotingTelegramEnabled: form.mvpVotingEnabled && form.mvpVotingTelegramEnabled,
      recurrenceType: form.recurringEnabled ? (form.recurringMode === 'days' ? 'DAYS' : 'MONTHLY') : null,
      recurrenceIntervalDays: form.recurringEnabled && form.recurringMode === 'days' ? form.recurringEveryDays || null : null,
      recurrenceDayOfMonth: form.recurringEnabled && form.recurringMode === 'month' ? form.recurringDayOfMonth || null : null,
      feeAmount: form.feeAmount || null,
      feeRecipient: form.feeRecipient || null,
      formatType: form.formatType,
      plannedMatchDurationMinutes: form.plannedMatchDurationMinutes,
      sessionDurationMinutes: form.sessionDurationMinutes || null,
      maxPlayers: form.maxPlayers || null,
      playerFormat: form.playerFormat || null,
      notes: form.notes || null,
      createdByUserId: authState.user?.id ?? null,
      teams: [
        { name: 'Красные', color: 'red', displayOrder: 1 },
        { name: 'Зеленые', color: 'green', displayOrder: 2 },
        { name: 'Синие', color: 'blue', displayOrder: 3 }
      ]
    });

    await Promise.all(selectedReminderHours.value.map((hoursBefore) => api.createContributionReminder(session.id, { hoursBefore })));
    await router.push(`/sessions/${session.id}`);
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Не удалось создать сессию';
  } finally {
    pending.value = false;
  }
}
</script>
