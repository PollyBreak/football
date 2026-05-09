import { reactive } from 'vue';
import { api } from './api';
import type { PlayerProfile, TelegramAuthResponse } from '../types';

export const authState = reactive({
  ready: false,
  authenticated: false,
  onboardingRequired: false,
  user: null as TelegramAuthResponse['user'] | null,
  player: null as TelegramAuthResponse['player'] | null,
  error: ''
});

let bootstrapPromise: Promise<void> | null = null;

export function bootstrapTelegramAuth(): Promise<void> {
  if (!bootstrapPromise) {
    bootstrapPromise = doBootstrapTelegramAuth();
  }

  return bootstrapPromise;
}

async function doBootstrapTelegramAuth(): Promise<void> {
  const initData = window.Telegram?.WebApp?.initData;

  if (!initData) {
    authState.ready = true;
    authState.authenticated = false;
    authState.onboardingRequired = false;
    authState.user = null;
    authState.player = null;
    authState.error = 'Откройте приложение через Telegram Mini App';
    return;
  }

  try {
    const result = await api.telegramAuth(initData);
    authState.ready = true;
    authState.authenticated = result.authenticated;
    authState.onboardingRequired = result.onboardingRequired;
    authState.user = result.user;
    authState.player = result.player;
    authState.error = '';
  } catch (error) {
    authState.ready = true;
    authState.authenticated = false;
    authState.onboardingRequired = false;
    authState.user = null;
    authState.player = null;
    authState.error = error instanceof Error ? error.message : 'Не удалось войти через Telegram';
  }
}

export function setRegisteredPlayer(player: PlayerProfile): void {
  authState.player = player;
  authState.authenticated = true;
  authState.onboardingRequired = false;
}
