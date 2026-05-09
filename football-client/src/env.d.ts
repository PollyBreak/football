/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string;
}

interface TelegramWebAppUser {
  id: number;
  first_name: string;
  last_name?: string;
  username?: string;
}

interface TelegramWebAppInitDataUnsafe {
  user?: TelegramWebAppUser;
  start_param?: string;
}

interface TelegramWebApp {
  initData: string;
  initDataUnsafe: TelegramWebAppInitDataUnsafe;
  ready(): void;
  expand(): void;
  colorScheme?: 'light' | 'dark';
  themeParams?: Record<string, string>;
}

interface Window {
  Telegram?: {
    WebApp: TelegramWebApp;
  };
}
