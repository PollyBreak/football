import { reactive } from 'vue';
import { api } from './api';
import type { AuthUser, PlayerProfile, TelegramAuthResponse } from '../types';

const telegramAuthRequired = import.meta.env.VITE_TELEGRAM_AUTH_REQUIRED !== 'false';
const mockTelegramId = Number(import.meta.env.VITE_DEV_AUTH_TELEGRAM_ID ?? 1001);
const mockPlayerId = Number(import.meta.env.VITE_DEV_AUTH_PLAYER_ID ?? 0);
const mockDisplayName = import.meta.env.VITE_DEV_AUTH_DISPLAY_NAME?.trim() || 'Local Dev Player';

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

  if (!telegramAuthRequired && !initData) {
    try {
      await authenticateAsLocalDevUser();
    } catch (error) {
      authState.ready = true;
      authState.authenticated = false;
      authState.onboardingRequired = false;
      authState.user = null;
      authState.player = null;
      authState.error = error instanceof Error ? error.message : 'Failed to load local dev player';
    }
    return;
  }

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

async function authenticateAsLocalDevUser(): Promise<void> {
  const createdAt = new Date().toISOString();
  const player = mockPlayerId > 0 ? await api.getPlayer(mockPlayerId) : null;
  const user: AuthUser = {
    id: player?.userId ?? mockTelegramId,
    telegramId: player?.telegramId ?? mockTelegramId,
    username: player?.username ?? 'local_dev',
    displayName: player?.displayName ?? mockDisplayName,
    photoUrl: player?.photoUrl ?? null,
    createdAt: player?.createdAt ?? createdAt
  };

  authState.ready = true;
  authState.authenticated = true;
  authState.onboardingRequired = !player;
  authState.user = user;
  authState.player = player;
  authState.error = '';
}

export function setRegisteredPlayer(player: PlayerProfile): void {
  authState.player = player;
  if (authState.user && player.displayName?.trim()) {
    authState.user.displayName = player.displayName;
  }
  authState.authenticated = true;
  authState.onboardingRequired = false;
}
