<template>
  <section class="stack">
    <div class="card">
      <p class="eyebrow">Первый вход</p>
      <h2 class="section-title">Заполните профиль игрока</h2>
      <p class="muted">
        Мы уже получили ваш Telegram-профиль. Осталось заполнить игровые поля, и после этого вы сможете
        создавать сессии и участвовать в матчах.
      </p>
    </div>

    <div class="card stack-sm">
      <div class="grid-form">
        <input v-model="form.firstName" class="input" placeholder="Имя" />
        <input v-model="form.lastName" class="input" placeholder="Фамилия" />
        <input v-model="form.displayName" class="input" placeholder="Отображаемое имя" />
        <input v-model="form.nickname" class="input" placeholder="Никнейм" />
        <input v-model="form.homeCity" class="input" placeholder="Город" />
        <input v-model="form.birthDate" class="input" type="date" />
        <select v-model="form.defaultPosition" class="input">
          <option v-for="position in positions" :key="position" :value="position">{{ playerPositionLabel(position) }}</option>
        </select>
      </div>

      <button class="primary-button form-submit" @click="completeRegistration" :disabled="pending">
        Зарегистрироваться
      </button>
      <p v-if="error" class="error-text">{{ error }}</p>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { api } from '../lib/api';
import { authState, setRegisteredPlayer } from '../lib/auth';
import { playerPositionLabel } from '../lib/labels';
import type { PlayerPosition } from '../types';

const router = useRouter();
const pending = ref(false);
const error = ref('');
const positions: PlayerPosition[] = ['GOALKEEPER', 'DEFENDER', 'MIDFIELDER', 'FORWARD', 'UNIVERSAL'];

const form = reactive({
  firstName: '',
  lastName: '',
  displayName: '',
  nickname: '',
  homeCity: '',
  birthDate: '',
  defaultPosition: 'MIDFIELDER' as PlayerPosition
});

async function completeRegistration() {
  if (!authState.user) {
    error.value = 'Пользователь Telegram недоступен';
    return;
  }

  pending.value = true;
  error.value = '';
  try {
    const player = await api.createPlayer({
      telegramId: authState.user.telegramId,
      username: authState.user.username,
      displayName: form.displayName,
      firstName: form.firstName,
      lastName: form.lastName || null,
      nickname: form.nickname || null,
      homeCity: form.homeCity || null,
      birthDate: form.birthDate || null,
      defaultPosition: form.defaultPosition
    });
    setRegisteredPlayer(player);
    await router.push('/sessions');
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Не удалось завершить регистрацию';
  } finally {
    pending.value = false;
  }
}

onMounted(() => {
  if (authState.player) {
    router.replace('/sessions');
    return;
  }

  const user = authState.user;
  form.displayName = user?.displayName ?? '';
  form.firstName = user?.displayName?.split(' ')[0] ?? '';
  form.lastName = user?.displayName?.split(' ').slice(1).join(' ') ?? '';
});
</script>
