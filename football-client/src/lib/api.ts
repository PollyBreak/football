import type {
  TelegramAuthResponse,
  GameSession,
  MatchEvent,
  PlayerProfile,
  SessionMatch,
  SessionPlayer,
  SessionJoinResponse,
  SessionStandings,
  SessionTeamPlayer,
  SessionWaitlistEntry
} from '../types';

const rawBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? '').trim();
const BASE_URL = rawBaseUrl.endsWith('/') ? rawBaseUrl.slice(0, -1) : rawBaseUrl;

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

export const api = {
  telegramAuth(initData: string): Promise<TelegramAuthResponse> {
    return request('/api/auth/telegram', {
      method: 'POST',
      body: JSON.stringify({ initData })
    });
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
  getSession(sessionId: number): Promise<GameSession> {
    return request(`/api/sessions/${sessionId}`);
  },
  getSessionPlayers(sessionId: number): Promise<SessionPlayer[]> {
    return request(`/api/sessions/${sessionId}/players`);
  },
  addPlayerToSession(sessionId: number, payload: Record<string, unknown>): Promise<SessionPlayer> {
    return request(`/api/sessions/${sessionId}/players`, { method: 'POST', body: JSON.stringify(payload) });
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
  getStandings(sessionId: number): Promise<SessionStandings> {
    return request(`/api/sessions/${sessionId}/standings`);
  },
  getMatchEvents(matchId: number): Promise<MatchEvent[]> {
    return request(`/api/session-matches/${matchId}/events`);
  },
  addGoal(matchId: number, payload: Record<string, unknown>): Promise<unknown> {
    return request(`/api/session-matches/${matchId}/events/goals`, { method: 'POST', body: JSON.stringify(payload) });
  },
  deleteEvent(matchId: number, eventId: number): Promise<void> {
    return request(`/api/session-matches/${matchId}/events/${eventId}`, { method: 'DELETE' });
  }
};
