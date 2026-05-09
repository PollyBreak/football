<template>
  <div class="app-shell" :class="{ 'app-shell--locked': !canUseApp }" @click="closeThemeMenu">
    <header class="app-header">
      <div>
        <p class="eyebrow">Мини-приложение Telegram</p>
        <h1>Pozitiv</h1>
        <p class="header-subtitle" v-if="authState.user">
          <img v-if="authState.user.photoUrl" :src="authState.user.photoUrl" alt="Аватар Telegram" class="avatar" />
          <span>{{ authState.user.displayName }}</span>
        </p>
        <p class="header-subtitle" v-else-if="authState.error">{{ authState.error }}</p>
      </div>

      <div class="header-actions">
        <div class="theme-picker" @click.stop>
          <button
            class="theme-button"
            type="button"
            :aria-expanded="themeMenuOpen"
            aria-label="Выбрать тему"
            @click="themeMenuOpen = !themeMenuOpen"
          >
            <span class="theme-button__swatch"></span>
          </button>
          <div v-if="themeMenuOpen" class="theme-menu">
            <button
              v-for="theme in themes"
              :key="theme.value"
              class="theme-menu__item"
              type="button"
              :class="{ 'is-active': currentTheme === theme.value }"
              @click="selectTheme(theme.value)"
            >
              <span class="theme-menu__swatch" :data-theme-swatch="theme.value"></span>
              <span>{{ theme.label }}</span>
            </button>
          </div>
        </div>
        <RouterLink v-if="canUseApp" class="ghost-button" to="/players">Игроки</RouterLink>
      </div>
    </header>

    <main class="app-main">
      <div v-if="!authState.ready" class="card stack-sm">
        <h2 class="section-title">Загрузка</h2>
        <p class="muted">Проверяем вход через Telegram.</p>
      </div>

      <div v-else-if="authBlocked" class="card stack-sm">
        <h2 class="section-title">Откройте через Telegram</h2>
        <p class="muted">Это приложение работает только как Telegram Mini App. Откройте его из вашего Telegram-бота.</p>
      </div>

      <RouterView v-else />
    </main>

    <nav v-if="canUseApp" class="bottom-nav">
      <RouterLink to="/sessions" class="bottom-nav__item">Сессии</RouterLink>
      <RouterLink to="/players" class="bottom-nav__item">Игроки</RouterLink>
      <RouterLink to="/profile" class="bottom-nav__item">Профиль</RouterLink>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router';
import { computed, onMounted, ref } from 'vue';
import { authState } from './lib/auth';

type ThemeValue = 'sky' | 'dark-blue' | 'green' | 'light' | 'dark';

const themes: { value: ThemeValue; label: string }[] = [
  { value: 'sky', label: 'Светло-голубая' },
  { value: 'dark-blue', label: 'Темная с синими кнопками' },
  { value: 'green', label: 'Светло-зеленая' },
  { value: 'light', label: 'Светлая' },
  { value: 'dark', label: 'Темная' }
];

const currentTheme = ref<ThemeValue>('sky');
const themeMenuOpen = ref(false);
const canUseApp = computed(() => authState.ready && authState.authenticated && Boolean(authState.player));
const authBlocked = computed(() => authState.ready && (!authState.authenticated || !authState.user));

function applyTheme(theme: ThemeValue) {
  document.documentElement.dataset.theme = theme;
  localStorage.setItem('football-app-theme', theme);
}

function selectTheme(theme: ThemeValue) {
  currentTheme.value = theme;
  applyTheme(theme);
  themeMenuOpen.value = false;
}

function closeThemeMenu() {
  themeMenuOpen.value = false;
}

onMounted(() => {
  const savedTheme = localStorage.getItem('football-app-theme') as ThemeValue | null;
  if (savedTheme && themes.some((theme) => theme.value === savedTheme)) {
    currentTheme.value = savedTheme;
  }
  applyTheme(currentTheme.value);
});
</script>
