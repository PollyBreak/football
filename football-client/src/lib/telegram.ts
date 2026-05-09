export function initTelegramWebApp(): void {
  const webApp = window.Telegram?.WebApp;
  if (!webApp) {
    return;
  }

  webApp.ready();
  webApp.expand();
  document.documentElement.dataset.colorScheme = webApp.colorScheme ?? 'light';
}

export function getTelegramUserLabel(): string {
  const user = window.Telegram?.WebApp?.initDataUnsafe?.user;
  if (!user) {
    return 'Telegram';
  }

  const fullName = [user.first_name, user.last_name].filter(Boolean).join(' ');
  return user.username ? `${fullName} (@${user.username})` : fullName;
}

export function getStartParam(): string | undefined {
  return window.Telegram?.WebApp?.initDataUnsafe?.start_param;
}
