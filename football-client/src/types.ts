export type PlayerPosition =
  | 'GOALKEEPER'
  | 'DEFENDER'
  | 'MIDFIELDER'
  | 'FORWARD'
  | 'UNIVERSAL';

export type SessionFormatType = 'ROUND_ROBIN' | 'KING_OF_THE_HILL' | 'CUSTOM';
export type SessionStatus = 'PLANNED' | 'IN_PROGRESS' | 'FINISHED' | 'CANCELLED';
export type MatchStatus = 'PLANNED' | 'IN_PROGRESS' | 'FINISHED' | 'CANCELLED';

export interface PlayerProfile {
  playerId: number;
  userId: number | null;
  telegramId: number | null;
  username: string | null;
  displayName: string | null;
  firstName: string;
  lastName: string | null;
  nickname: string | null;
  photoUrl?: string | null;
  homeCity: string | null;
  birthDate: string | null;
  defaultPosition: PlayerPosition | null;
  active: boolean;
  createdAt: string;
}

export interface AuthUser {
  id: number;
  telegramId: number;
  username: string | null;
  displayName: string;
  photoUrl: string | null;
  createdAt: string;
}

export interface TelegramAuthResponse {
  authenticated: boolean;
  onboardingRequired: boolean;
  user: AuthUser;
  player: PlayerProfile | null;
}

export interface SessionTeam {
  id: number;
  sessionId: number;
  name: string;
  color: string | null;
  displayOrder: number;
  createdAt: string;
}

export interface GameSession {
  id: number;
  title: string;
  sessionDate: string;
  location: string | null;
  formatType: SessionFormatType;
  status: SessionStatus;
  plannedMatchDurationMinutes: number | null;
  maxPlayers: number | null;
  notes: string | null;
  createdByUserId: number | null;
  createdAt: string;
  startedAt: string | null;
  endedAt: string | null;
  teams: SessionTeam[];
}

export interface SessionPlayer {
  id: number;
  sessionId: number;
  playerId: number;
  firstName: string;
  lastName: string | null;
  nickname: string | null;
  photoUrl: string | null;
  position: PlayerPosition | null;
  active: boolean;
  joinedAt: string;
  leftAt: string | null;
}

export interface SessionWaitlistEntry {
  id: number;
  sessionId: number;
  playerId: number;
  firstName: string;
  lastName: string | null;
  nickname: string | null;
  photoUrl: string | null;
  position: PlayerPosition | null;
  active: boolean;
  queuedAt: string;
  leftAt: string | null;
}

export interface SessionJoinResponse {
  status: 'ACTIVE' | 'QUEUED';
  player: SessionPlayer | null;
  waitlistEntry: SessionWaitlistEntry | null;
}

export interface SessionTeamPlayer {
  id: number;
  sessionTeamId: number;
  playerId: number;
  playerName: string;
  photoUrl: string | null;
  position: PlayerPosition | null;
  active: boolean;
  joinedAt: string;
  leftAt: string | null;
}

export interface SessionMatch {
  id: number;
  sessionId: number;
  teamAId: number;
  teamAName: string;
  teamBId: number;
  teamBName: string;
  matchNumber: number;
  status: MatchStatus;
  plannedDurationMinutes: number | null;
  teamAScore: number;
  teamBScore: number;
  winningTeamId: number | null;
  createdAt: string;
  startedAt: string | null;
  endedAt: string | null;
}

export interface MatchEvent {
  id: number;
  matchId: number;
  eventType: string;
  teamId: number | null;
  teamName: string | null;
  playerId: number | null;
  playerName: string | null;
  relatedPlayerId: number | null;
  relatedPlayerName: string | null;
  linkedEventId: number | null;
  minuteInMatch: number | null;
  secondInMatch: number | null;
  eventTime: string;
  createdByUserId: number | null;
  payload: Record<string, unknown> | null;
  createdAt: string;
}

export interface SessionStandingsRow {
  teamId: number;
  teamName: string;
  teamColor: string | null;
  played: number;
  wins: number;
  draws: number;
  losses: number;
  goalsFor: number;
  goalsAgainst: number;
  goalDifference: number;
  points: number;
}

export interface SessionStandings {
  sessionId: number;
  standings: SessionStandingsRow[];
}
