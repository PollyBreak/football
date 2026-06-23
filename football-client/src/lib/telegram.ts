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
  const telegramStartParam = window.Telegram?.WebApp?.initDataUnsafe?.start_param;
  if (telegramStartParam) {
    return telegramStartParam;
  }

  const searchParams = new URLSearchParams(window.location.search);
  return searchParams.get('tgWebAppStartParam')
    ?? searchParams.get('startapp')
    ?? undefined;
}

export function startParamTargetPath(): string | null {
  const startParam = getStartParam();
  const sessionMatch = startParam?.match(/^session_(\d+)$/);
  if (sessionMatch) {
    return `/sessions/${sessionMatch[1]}`;
  }

  const mvpMatch = startParam?.match(/^mvp_(\d+)$/);
  if (mvpMatch) {
    return `/sessions/${mvpMatch[1]}/mvp`;
  }

  const joinMatch = startParam?.match(/^join_(\d+)_(GOING|MAYBE|OUT)$/);
  if (joinMatch) {
    return `/sessions/${joinMatch[1]}`;
  }

  return null;
}
