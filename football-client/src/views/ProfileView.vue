<template>
  <section class="stack">
    <div class="card profile-summary">
      <div>
        <p class="eyebrow">Мой аккаунт</p>
        <h2 class="section-title">{{ profileTitle }}</h2>
        <p class="muted">{{ authState.player ? 'Вы вошли в приложение.' : 'Заполните профиль игрока, чтобы войти в приложение.' }}</p>
      </div>
      <div class="profile-photo" aria-label="Фото профиля">
        <img v-if="telegramPhotoUrl" :src="telegramPhotoUrl" alt="Фото из Telegram" />
        <span v-else>{{ profileInitials }}</span>
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
          <input v-model="profileForm.birthDate" class="input" type="date" />
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
          <input v-model="registrationForm.birthDate" class="input" type="date" />
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
import { api } from '../lib/api';
import { authState, setRegisteredPlayer } from '../lib/auth';
import { playerPositionLabel } from '../lib/labels';
import type { PlayerProfile, PlayerPosition } from '../types';

const pending = ref(false);
const error = ref('');
const positions: PlayerPosition[] = ['GOALKEEPER', 'DEFENDER', 'MIDFIELDER', 'FORWARD', 'UNIVERSAL'];

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

const telegramPhotoUrl = computed(() => authState.user?.photoUrl ?? '');
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
  profileForm.birthDate = player.birthDate ?? '';
  profileForm.defaultPosition = player.defaultPosition ?? 'MIDFIELDER';
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
      birthDate: registrationForm.birthDate || null,
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
      birthDate: profileForm.birthDate || null,
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
</script>
