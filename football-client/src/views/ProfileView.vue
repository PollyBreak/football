<template>
  <section class="stack">
    <div class="card stack-sm">
      <div class="profile-summary">
        <div>
          <p class="eyebrow">Мой аккаунт</p>
          <h2 class="section-title">{{ profileTitle }}</h2>
          <label v-if="authState.player" class="profile-photo-upload-button" :class="{ 'is-disabled': photoUploadPending }">
            <input
              class="venue-photo-input"
              type="file"
              accept="image/png,image/jpeg,image/webp"
              :disabled="photoUploadPending"
              @change="uploadOwnPlayerPhoto"
            />
            {{ photoUploadPending ? 'Загружаем...' : 'Загрузить фото' }}
          </label>
        </div>
        <div class="profile-photo-column">
          <button
            class="profile-photo profile-photo--clickable"
            type="button"
            aria-label="Открыть фото профиля"
            :disabled="!profilePhotoPreviewUrl"
            @click="openPhotoPreview"
          >
            <PlayerAvatar :sources="profilePhotoSources" :initials="profileInitials" alt="Фото профиля" @loaded="profilePhotoPreviewUrl = $event" />
          </button>
        </div>
      </div>
    </div>

    <div v-if="photoPreviewOpen" class="photo-preview-overlay" @click.self="closePhotoPreview">
      <button class="photo-preview-close" type="button" aria-label="Закрыть фото" @click="closePhotoPreview">×</button>
      <img v-if="profilePhotoPreviewUrl" class="photo-preview-image" :src="profilePhotoPreviewUrl" alt="Фото профиля" />
    </div>

    <div v-if="authState.player" class="card stack-sm">
      <div class="section-header">
        <h3 class="section-title">Моя статистика</h3>
      </div>
      <div class="profile-details">
        <div>
          <span class="muted">Рейтинг</span>
          <strong>{{ authState.player.rating }}</strong>
        </div>
        <div>
          <span class="muted">Голы</span>
          <strong>{{ authState.player.stats.goals }} ⚽</strong>
        </div>
        <div>
          <span class="muted">Голевые передачи</span>
          <strong>{{ authState.player.stats.assists }} 👟</strong>
        </div>
      </div>
    </div>

    <div v-if="authState.player" class="card stack-sm">
      <div class="section-header">
        <h2 class="section-title">Личная информация</h2>
      </div>

      <div class="grid-form">
        <label class="field-label">
          <span>Имя</span>
          <input v-model="profileForm.firstName" class="input" placeholder="Имя" required />
        </label>
        <label class="field-label">
          <span>Фамилия</span>
          <input v-model="profileForm.lastName" class="input" placeholder="Фамилия" />
        </label>
        <label class="field-label">
          <span>Отображаемое имя в приложении</span>
          <input v-model="profileForm.displayName" class="input" placeholder="Username" required />
        </label>
        <label class="field-label">
          <span>Откуда ты?</span>
          <input v-model="profileForm.homeCity" class="input" placeholder="Родной город" />
        </label>
        <label class="field-label">
          <span>Дата рождения</span>
          <input
            v-model="profileForm.birthDate"
            class="input"
            type="text"
            inputmode="numeric"
            maxlength="10"
            placeholder="ДД.ММ.ГГГГ"
            @input="profileForm.birthDate = formatBirthDateInput(profileForm.birthDate)"
          />
        </label>
        <label class="field-label">
          <span>Имя пользователя Telegram</span>
          <input class="input" :value="telegramUsername" placeholder="Не указан" readonly />
        </label>
        <label class="field-label">
          <span>Позиция</span>
          <select v-model="profileForm.defaultPosition" class="input" required>
            <option v-for="position in positions" :key="position" :value="position">{{ playerPositionLabel(position) }}</option>
          </select>
        </label>
      </div>

      <button class="primary-button form-submit" @click="saveProfile" :disabled="pending">Сохранить изменения</button>
    </div>

    <div v-if="authState.player" class="card stack-sm">
      <div class="section-header">
        <h3 class="section-title">Мои сессии</h3>
      </div>
      <p v-if="!authState.player.sessions.length" class="muted">Вы пока не участвовали в сессиях.</p>
      <div v-else class="list">
        <RouterLink
          v-for="session in authState.player.sessions"
          :key="session.sessionId"
          :to="`/sessions/${session.sessionId}`"
          class="list-item player-list-item"
        >
          <div>
            <strong>{{ session.title }}</strong>
            <p class="muted">{{ session.sessionDate }} {{ session.sessionTime?.slice(0, 5) }}</p>
          </div>
          <span class="status-pill" :class="sessionStatusClass(session.status)">{{ sessionStatusLabel(session.status) }}</span>
        </RouterLink>
      </div>
    </div>

    <div v-else class="card stack-sm">
      <div class="section-header">
        <h2 class="section-title">Создать профиль игрока</h2>
      </div>

      <div class="grid-form">
        <label class="field-label">
          <span>Имя</span>
          <input v-model="registrationForm.firstName" class="input" placeholder="Имя" required />
        </label>
        <label class="field-label">
          <span>Фамилия</span>
          <input v-model="registrationForm.lastName" class="input" placeholder="Фамилия" />
        </label>
        <label class="field-label">
          <span>Отображаемое имя в приложении</span>
          <input v-model="registrationForm.displayName" class="input" placeholder="Username" required />
        </label>
        <label class="field-label">
          <span>Откуда ты?</span>
          <input v-model="registrationForm.homeCity" class="input" placeholder="Родной город" />
        </label>
        <label class="field-label">
          <span>Дата рождения</span>
          <input
            v-model="registrationForm.birthDate"
            class="input"
            type="text"
            inputmode="numeric"
            maxlength="10"
            placeholder="ДД.ММ.ГГГГ"
            @input="registrationForm.birthDate = formatBirthDateInput(registrationForm.birthDate)"
          />
        </label>
        <label class="field-label">
          <span>Имя пользователя Telegram</span>
          <input class="input" :value="telegramUsername" placeholder="Не указан" readonly />
        </label>
        <label class="field-label">
          <span>Позиция</span>
          <select v-model="registrationForm.defaultPosition" class="input" required>
            <option v-for="position in positions" :key="position" :value="position">{{ playerPositionLabel(position) }}</option>
          </select>
        </label>
      </div>

      <button class="primary-button form-submit" @click="createPlayer" :disabled="pending">Зарегистрироваться</button>
    </div>

    <p v-if="error" class="error-text">{{ error }}</p>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { RouterLink } from 'vue-router';
import { api } from '../lib/api';
import { authState, setRegisteredPlayer } from '../lib/auth';
import { playerPositionLabel, selectablePlayerPositions, sessionStatusClass, sessionStatusLabel } from '../lib/labels';
import type { PlayerProfile, PlayerPosition } from '../types';
import PlayerAvatar from '../components/PlayerAvatar.vue';

const pending = ref(false);
const photoUploadPending = ref(false);
const error = ref('');
const photoPreviewOpen = ref(false);
const profilePhotoPreviewUrl = ref('');
const positions = selectablePlayerPositions;

const registrationForm = reactive({
  displayName: '',
  firstName: '',
  lastName: '',
  homeCity: '',
  birthDate: '',
  defaultPosition: 'MIDFIELDER' as PlayerPosition
});

const profileForm = reactive({
  displayName: '',
  firstName: '',
  lastName: '',
  homeCity: '',
  birthDate: '',
  defaultPosition: 'MIDFIELDER' as PlayerPosition
});

const profilePhotoSources = computed(() => [
  authState.player?.photoUrl,
  authState.player?.telegramPhotoUrl,
  authState.user?.photoUrl
]);
const telegramUsername = computed(() => {
  const username = authState.user?.username ?? authState.player?.username ?? '';
  return username ? `@${username}` : '';
});
const profileTitle = computed(() => authState.player?.displayName || authState.user?.displayName || 'Профиль');
const profileInitials = computed(() => {
  const source = authState.player
    ? `${authState.player.firstName} ${authState.player.lastName ?? ''}`
    : authState.user?.displayName ?? 'Игрок';

  return source
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0])
    .join('') || 'И';
});

function fillRegistrationFromTelegram() {
  if (!authState.user) return;

  registrationForm.displayName = authState.user.displayName;
  registrationForm.firstName = authState.user.displayName.split(' ')[0] ?? '';
  registrationForm.lastName = authState.user.displayName.split(' ').slice(1).join(' ');
}

function fillProfileForm(player: PlayerProfile) {
  profileForm.displayName = player.displayName ?? '';
  profileForm.firstName = player.firstName;
  profileForm.lastName = player.lastName ?? '';
  profileForm.homeCity = player.homeCity ?? '';
  profileForm.birthDate = formatBirthDateForInput(player.birthDate);
  profileForm.defaultPosition = player.defaultPosition ?? 'MIDFIELDER';
}

function formatBirthDateInput(value: string): string {
  const digits = value.replace(/\D/g, '').slice(0, 8);
  const parts = [digits.slice(0, 2), digits.slice(2, 4), digits.slice(4, 8)].filter(Boolean);
  return parts.join('.');
}

function formatBirthDateForInput(value: string | null | undefined): string {
  if (!value) return '';
  const match = value.match(/^(\d{4})-(\d{2})-(\d{2})$/);
  return match ? `${match[3]}.${match[2]}.${match[1]}` : formatBirthDateInput(value);
}

function birthDateToApi(value: string): string | null {
  if (!value.trim()) return null;
  const match = value.match(/^(\d{2})\.(\d{2})\.(\d{4})$/);
  if (!match) {
    throw new Error('Введите дату рождения в формате ДД.ММ.ГГГГ');
  }

  const day = Number(match[1]);
  const month = Number(match[2]);
  const year = Number(match[3]);
  const date = new Date(Date.UTC(year, month - 1, day));
  const isValid = date.getUTCFullYear() === year
    && date.getUTCMonth() === month - 1
    && date.getUTCDate() === day;

  if (!isValid) {
    throw new Error('Введите корректную дату рождения');
  }

  return `${match[3]}-${match[2]}-${match[1]}`;
}

function openPhotoPreview() {
  if (profilePhotoPreviewUrl.value) {
    photoPreviewOpen.value = true;
  }
}

function closePhotoPreview() {
  photoPreviewOpen.value = false;
}

async function createPlayer() {
  if (!authState.user) {
    error.value = 'Пользователь Telegram недоступен';
    return;
  }

  const firstName = registrationForm.firstName.trim();
  const displayName = registrationForm.displayName.trim();
  if (!firstName || !displayName || !registrationForm.defaultPosition) {
    error.value = 'Заполните имя, Username и позицию';
    return;
  }

  let birthDate: string | null;
  try {
    birthDate = birthDateToApi(registrationForm.birthDate);
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Введите корректную дату рождения';
    return;
  }

  pending.value = true;
  error.value = '';
  try {
    const player = await api.createPlayer({
      telegramId: authState.user.telegramId,
      username: authState.user.username,
      displayName,
      firstName,
      lastName: registrationForm.lastName.trim() || null,
      nickname: null,
      homeCity: registrationForm.homeCity.trim() || null,
      birthDate,
      defaultPosition: registrationForm.defaultPosition
    });
    setRegisteredPlayer(player);
    fillProfileForm(player);
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Не удалось создать игрока';
  } finally {
    pending.value = false;
  }
}

async function saveProfile() {
  if (!authState.player) return;

  const firstName = profileForm.firstName.trim();
  const displayName = profileForm.displayName.trim();
  if (!firstName || !displayName || !profileForm.defaultPosition) {
    error.value = 'Заполните имя, Username и позицию';
    return;
  }

  let birthDate: string | null;
  try {
    birthDate = birthDateToApi(profileForm.birthDate);
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Введите корректную дату рождения';
    return;
  }

  pending.value = true;
  error.value = '';
  try {
    const player = await api.updatePlayer(authState.player.playerId, {
      username: authState.user?.username ?? authState.player.username ?? null,
      displayName,
      firstName,
      lastName: profileForm.lastName.trim() || null,
      nickname: null,
      homeCity: profileForm.homeCity.trim() || null,
      birthDate,
      defaultPosition: profileForm.defaultPosition,
      active: true
    });
    setRegisteredPlayer(player);
    fillProfileForm(player);
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Не удалось сохранить профиль';
  } finally {
    pending.value = false;
  }
}

async function uploadOwnPlayerPhoto(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file || !authState.player || !authState.user) {
    return;
  }

  photoUploadPending.value = true;
  error.value = '';
  try {
    const player = await api.uploadOwnPlayerPhoto(authState.user.id, file);
    setRegisteredPlayer(player);
    fillProfileForm(player);
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Не удалось загрузить фото';
  } finally {
    photoUploadPending.value = false;
    input.value = '';
  }
}

async function refreshCurrentPlayerProfile() {
  if (!authState.player?.playerId) {
    return;
  }

  try {
    const player = await api.getPlayer(authState.player.playerId);
    setRegisteredPlayer(player);
    fillProfileForm(player);
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Не удалось обновить профиль игрока';
  }
}

watch(
  () => authState.user,
  () => fillRegistrationFromTelegram(),
  { immediate: true }
);

watch(
  () => authState.player,
  (player) => {
    if (player) {
      fillProfileForm(player);
    }
  },
  { immediate: true }
);

onMounted(fillRegistrationFromTelegram);
onMounted(() => {
  void refreshCurrentPlayerProfile();
});
</script>
