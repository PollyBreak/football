import type {
  TelegramAuthResponse,
  ContributionReminder,
  ContributionStatus,
  FileUploadResponse,
  GameSession,
  MatchEvent,
  OverlayState,
  PlayerProfile,
  SessionMatch,
  SessionMvpVoting,
  SessionPlayer,
  SessionJoinResponse,
  SessionVenue,
  SessionStandings,
  SessionTeamPlayer,
  SessionWaitlistEntry,
  StreamBroadcast,
  StreamTimeline,
  TelegramKnownChat
} from '../types';

const rawBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? '').trim();
const BASE_URL = rawBaseUrl.endsWith('/') ? rawBaseUrl.slice(0, -1) : rawBaseUrl;

export function resolveMediaUrl(url: string | null | undefined): string {
  const value = url?.trim();
  if (!value) {
    return '';
  }
  if (value.startsWith('/')) {
    return BASE_URL ? `${BASE_URL}${value}` : value;
  }
  if (value.startsWith('//')) {
    return `${window.location.protocol}${value}`;
  }

  try {
    const parsed = new URL(value);
    if (
      parsed.protocol === 'http:' &&
      window.location.protocol === 'https:' &&
      parsed.hostname !== 'localhost' &&
      parsed.hostname !== '127.0.0.1'
    ) {
      parsed.protocol = 'https:';
      return parsed.toString();
    }
  } catch {
    // Keep the original value below.
  }

  return value;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  const response = await fetch(`${BASE_URL}${normalizedPath}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(init?.headers ?? {})
    },
    ...init
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(parseErrorMessage(text, response.status));
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  if (!text.trim()) {
    return undefined as T;
  }

  return JSON.parse(text) as T;
}

function parseErrorMessage(text: string, status: number): string {
  if (!text) {
    return `Запрос не выполнен: ${status}`;
  }

  try {
    const payload = JSON.parse(text) as { message?: unknown; error?: unknown };
    if (typeof payload.message === 'string' && payload.message.trim()) {
      return payload.message;
    }
    if (typeof payload.error === 'string' && payload.error.trim()) {
      return payload.error;
    }
  } catch {
    // Keep the plain response text below.
  }

  return text;
}

async function uploadFile<T>(path: string, fieldName: string, file: File): Promise<T> {
  const formData = new FormData();
  formData.append(fieldName, file);
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  const response = await fetch(`${BASE_URL}${normalizedPath}`, {
    method: 'POST',
    body: formData
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(parseErrorMessage(text, response.status));
  }

  return JSON.parse(await response.text()) as T;
}

export const api = {
  telegramAuth(initData: string): Promise<TelegramAuthResponse> {
    return request('/api/auth/telegram', {
      method: 'POST',
      body: JSON.stringify({ initData })
    });
  },
  getAvailableTelegramChats(userId: number): Promise<TelegramKnownChat[]> {
    return request(`/api/telegram/chats?userId=${userId}`);
  },
  getPlayers(): Promise<PlayerProfile[]> {
    return request('/api/players?activeOnly=true');
  },
  getPlayer(playerId: number): Promise<PlayerProfile> {
    return request(`/api/players/${playerId}`);
  },
  createPlayer(payload: Record<string, unknown>): Promise<PlayerProfile> {
    return request('/api/players', { method: 'POST', body: JSON.stringify(payload) });
  },
  updatePlayer(playerId: number, payload: Record<string, unknown>): Promise<PlayerProfile> {
    return request(`/api/players/${playerId}`, { method: 'PUT', body: JSON.stringify(payload) });
  },
  getSessions(): Promise<GameSession[]> {
    return request('/api/sessions');
  },
  createSession(payload: Record<string, unknown>): Promise<GameSession> {
    return request('/api/sessions', { method: 'POST', body: JSON.stringify(payload) });
  },
  updateSession(sessionId: number, payload: Record<string, unknown>): Promise<GameSession> {
    return request(`/api/sessions/${sessionId}`, { method: 'PATCH', body: JSON.stringify(payload) });
  },
  getSessionVenues(): Promise<SessionVenue[]> {
    return request('/api/session-venues');
  },
  createSessionVenue(payload: Record<string, unknown>): Promise<SessionVenue> {
    return request('/api/session-venues', { method: 'POST', body: JSON.stringify(payload) });
  },
  updateSessionVenue(venueId: number, payload: Record<string, unknown>): Promise<SessionVenue> {
    return request(`/api/session-venues/${venueId}`, { method: 'PUT', body: JSON.stringify(payload) });
  },
  uploadSessionVenuePhoto(file: File): Promise<FileUploadResponse> {
    return uploadFile('/api/session-venues/photos', 'file', file);
  },
  validateTelegramChat(sessionId: number, payload: Record<string, unknown>): Promise<{ chatId: number; title: string | null; valid: boolean }> {
    return request(`/api/sessions/${sessionId}/telegram-chat/validate`, { method: 'POST', body: JSON.stringify(payload) });
  },
  startSessionRegistration(sessionId: number, payload: Record<string, unknown>): Promise<{ chatId: number; messageId: number; messageUrl: string | null }> {
    return request(`/api/sessions/${sessionId}/registration/start`, { method: 'POST', body: JSON.stringify(payload) });
  },
  startContributionCollection(sessionId: number, payload: Record<string, unknown>): Promise<{ chatId: number; messageId: number; messageUrl: string | null }> {
    return request(`/api/sessions/${sessionId}/contributions/start`, { method: 'POST', body: JSON.stringify(payload) });
  },
  getContributionStatuses(sessionId: number): Promise<ContributionStatus[]> {
    return request(`/api/sessions/${sessionId}/contributions/statuses`);
  },
  getContributionReminders(sessionId: number): Promise<ContributionReminder[]> {
    return request(`/api/sessions/${sessionId}/contribution-reminders`);
  },
  createContributionReminder(sessionId: number, payload: Record<string, unknown>): Promise<ContributionReminder> {
    return request(`/api/sessions/${sessionId}/contribution-reminders`, { method: 'POST', body: JSON.stringify(payload) });
  },
  deleteContributionReminder(sessionId: number, hoursBefore: number): Promise<void> {
    return request(`/api/sessions/${sessionId}/contribution-reminders/${hoursBefore}`, { method: 'DELETE' });
  },
  getSession(sessionId: number): Promise<GameSession> {
    return request(`/api/sessions/${sessionId}`);
  },
  getSessionMvpVoting(sessionId: number, userId?: number | null): Promise<SessionMvpVoting> {
    const search = userId ? `?userId=${userId}` : '';
    return request(`/api/sessions/${sessionId}/mvp${search}`);
  },
  voteForSessionMvp(sessionId: number, payload: Record<string, unknown>): Promise<SessionMvpVoting> {
    return request(`/api/sessions/${sessionId}/mvp/votes`, { method: 'POST', body: JSON.stringify(payload) });
  },
  getSessionPlayers(sessionId: number): Promise<SessionPlayer[]> {
    return request(`/api/sessions/${sessionId}/players`);
  },
  addPlayerToSession(sessionId: number, payload: Record<string, unknown>): Promise<SessionPlayer> {
    return request(`/api/sessions/${sessionId}/players`, { method: 'POST', body: JSON.stringify(payload) });
  },
  createGuestSessionPlayer(sessionId: number, payload: Record<string, unknown>): Promise<SessionPlayer> {
    return request(`/api/sessions/${sessionId}/players/guest`, { method: 'POST', body: JSON.stringify(payload) });
  },
  joinSession(sessionId: number, payload: Record<string, unknown>): Promise<SessionJoinResponse> {
    return request(`/api/sessions/${sessionId}/players/join`, { method: 'POST', body: JSON.stringify(payload) });
  },
  removePlayerFromSession(sessionId: number, playerId: number): Promise<void> {
    return request(`/api/sessions/${sessionId}/players/${playerId}`, { method: 'DELETE' });
  },
  getSessionWaitlist(sessionId: number): Promise<SessionWaitlistEntry[]> {
    return request(`/api/sessions/${sessionId}/players/waitlist`);
  },
  leaveSessionWaitlist(sessionId: number, playerId: number): Promise<void> {
    return request(`/api/sessions/${sessionId}/players/waitlist/${playerId}`, { method: 'DELETE' });
  },
  getTeamPlayers(teamId: number): Promise<SessionTeamPlayer[]> {
    return request(`/api/session-teams/${teamId}/players`);
  },
  bulkAssignPlayers(teamId: number, playerIds: number[]): Promise<SessionTeamPlayer[]> {
    return request(`/api/session-teams/${teamId}/players/bulk`, {
      method: 'POST',
      body: JSON.stringify({ playerIds })
    });
  },
  removePlayerFromTeam(teamId: number, playerId: number): Promise<void> {
    return request(`/api/session-teams/${teamId}/players/${playerId}`, { method: 'DELETE' });
  },
  getMatches(sessionId: number): Promise<SessionMatch[]> {
    return request(`/api/sessions/${sessionId}/matches`);
  },
  getMatch(sessionId: number, matchId: number): Promise<SessionMatch> {
    return request(`/api/sessions/${sessionId}/matches/${matchId}`);
  },
  createMatch(sessionId: number, payload: Record<string, unknown>): Promise<SessionMatch> {
    return request(`/api/sessions/${sessionId}/matches`, { method: 'POST', body: JSON.stringify(payload) });
  },
  startMatch(sessionId: number, matchId: number): Promise<SessionMatch> {
    return request(`/api/sessions/${sessionId}/matches/${matchId}/start`, { method: 'POST', body: JSON.stringify({}) });
  },
  finishMatch(sessionId: number, matchId: number): Promise<SessionMatch> {
    return request(`/api/sessions/${sessionId}/matches/${matchId}/finish`, { method: 'POST', body: JSON.stringify({}) });
  },
  pauseMatch(sessionId: number, matchId: number): Promise<SessionMatch> {
    return request(`/api/sessions/${sessionId}/matches/${matchId}/pause`, { method: 'POST', body: JSON.stringify({}) });
  },
  resumeMatch(sessionId: number, matchId: number): Promise<SessionMatch> {
    return request(`/api/sessions/${sessionId}/matches/${matchId}/resume`, { method: 'POST', body: JSON.stringify({}) });
  },
  getStandings(sessionId: number): Promise<SessionStandings> {
    return request(`/api/sessions/${sessionId}/standings`);
  },
  startStream(sessionId: number): Promise<StreamBroadcast> {
    return request(`/api/sessions/${sessionId}/streams/start`, { method: 'POST', body: JSON.stringify({}) });
  },
  restartStream(sessionId: number): Promise<StreamBroadcast> {
    return request(`/api/sessions/${sessionId}/streams/restart`, { method: 'POST', body: JSON.stringify({}) });
  },
  getStreams(sessionId: number): Promise<StreamBroadcast[]> {
    return request(`/api/sessions/${sessionId}/streams`);
  },
  addStreamShift(sessionId: number, streamId: number, shiftSeconds: number): Promise<StreamBroadcast> {
    return request(`/api/sessions/${sessionId}/streams/${streamId}/shifts`, {
      method: 'POST',
      body: JSON.stringify({ shiftSeconds })
    });
  },
  getStreamTimeline(sessionId: number, streamId: number): Promise<StreamTimeline> {
    return request(`/api/sessions/${sessionId}/streams/${streamId}/timeline`);
  },
  getMatchEvents(matchId: number): Promise<MatchEvent[]> {
    return request(`/api/session-matches/${matchId}/events`);
  },
  addGoal(matchId: number, payload: Record<string, unknown>): Promise<unknown> {
    return request(`/api/session-matches/${matchId}/events/goals`, { method: 'POST', body: JSON.stringify(payload) });
  },
  deleteEvent(matchId: number, eventId: number): Promise<void> {
    return request(`/api/session-matches/${matchId}/events/${eventId}`, { method: 'DELETE' });
  },
  getOverlayState(sessionId: number, matchId?: number | null): Promise<OverlayState> {
    const search = matchId ? `?matchId=${matchId}` : '';
    return request(`/api/overlay/sessions/${sessionId}/state${search}`);
  },
  getOverlayStreamUrl(sessionId: number): string {
    return `${BASE_URL}/api/overlay/sessions/${sessionId}/stream`;
  }
};
