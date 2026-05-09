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
        <input v-model="profileForm.firstName" class="input" placeholder="Имя" />
        <input v-model="profileForm.lastName" class="input" placeholder="Фамилия" />
        <input v-model="profileForm.displayName" class="input" placeholder="Отображаемое имя" />
        <input v-model="profileForm.nickname" class="input" placeholder="Никнейм" />
        <input v-model="profileForm.homeCity" class="input" placeholder="Город" />
        <input v-model="profileForm.birthDate" class="input" type="date" />
        <input v-model="profileForm.username" class="input" placeholder="Имя пользователя Telegram" />
        <select v-model="profileForm.defaultPosition" class="input">
          <option v-for="position in positions" :key="position" :value="position">{{ playerPositionLabel(position) }}</option>
        </select>
      </div>

      <button class="primary-button form-submit" @click="saveProfile" :disabled="pending">Сохранить изменения</button>
    </div>

    <div v-else class="card stack-sm">
      <div class="section-header">
        <h2 class="section-title">Создать профиль игрока</h2>
      </div>

      <div class="grid-form">
        <input v-model="registrationForm.firstName" class="input" placeholder="Имя" />
        <input v-model="registrationForm.lastName" class="input" placeholder="Фамилия" />
        <input v-model="registrationForm.displayName" class="input" placeholder="Отображаемое имя" />
        <input v-model="registrationForm.nickname" class="input" placeholder="Никнейм" />
        <input v-model="registrationForm.homeCity" class="input" placeholder="Город" />
        <input v-model="registrationForm.birthDate" class="input" type="date" />
        <input v-model="registrationForm.username" class="input" placeholder="Имя пользователя Telegram" />
        <input v-model.number="registrationForm.telegramId" class="input" type="number" placeholder="Telegram ID" />
        <select v-model="registrationForm.defaultPosition" class="input">
          <option v-for="position in positions" :key="position" :value="position">{{ playerPositionLabel(position) }}</option>
        </select>
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
  telegramId: undefined as number | undefined,
  username: '',
  displayName: '',
  firstName: '',
  lastName: '',
  nickname: '',
  homeCity: '',
  birthDate: '',
  defaultPosition: 'MIDFIELDER' as PlayerPosition
});

const profileForm = reactive({
  username: '',
  displayName: '',
  firstName: '',
  lastName: '',
  nickname: '',
  homeCity: '',
  birthDate: '',
  defaultPosition: 'MIDFIELDER' as PlayerPosition
});

const telegramPhotoUrl = computed(() => authState.user?.photoUrl ?? '');
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

  registrationForm.telegramId = authState.user.telegramId;
  registrationForm.username = authState.user.username ?? '';
  registrationForm.displayName = authState.user.displayName;
  registrationForm.firstName = authState.user.displayName.split(' ')[0] ?? '';
  registrationForm.lastName = authState.user.displayName.split(' ').slice(1).join(' ');
}

function fillProfileForm(player: PlayerProfile) {
  profileForm.username = player.username ?? authState.user?.username ?? '';
  profileForm.displayName = player.displayName ?? '';
  profileForm.firstName = player.firstName;
  profileForm.lastName = player.lastName ?? '';
  profileForm.nickname = player.nickname ?? '';
  profileForm.homeCity = player.homeCity ?? '';
  profileForm.birthDate = player.birthDate ?? '';
  profileForm.defaultPosition = player.defaultPosition ?? 'MIDFIELDER';
}

async function createPlayer() {
  pending.value = true;
  error.value = '';
  try {
    const player = await api.createPlayer({
      telegramId: registrationForm.telegramId,
      username: registrationForm.username || null,
      displayName: registrationForm.displayName,
      firstName: registrationForm.firstName,
      lastName: registrationForm.lastName || null,
      nickname: registrationForm.nickname || null,
      homeCity: registrationForm.homeCity || null,
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

  pending.value = true;
  error.value = '';
  try {
    const player = await api.updatePlayer(authState.player.playerId, {
      username: profileForm.username || null,
      displayName: profileForm.displayName,
      firstName: profileForm.firstName,
      lastName: profileForm.lastName || null,
      nickname: profileForm.nickname || null,
      homeCity: profileForm.homeCity || null,
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
