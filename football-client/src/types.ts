export type PlayerPosition =
  | 'GOALKEEPER'
  | 'DEFENDER'
  | 'MIDFIELDER'
  | 'FORWARD'
  | 'UNIVERSAL';

export type SessionFormatType = 'ROUND_ROBIN' | 'DUEL' | 'KNOCKOUT' | 'KING_OF_THE_HILL' | 'CUSTOM';
export type SessionRecurrenceType = 'DAYS' | 'MONTHLY';
export type SessionStatus = 'PLANNED' | 'IN_PROGRESS' | 'FINISHED' | 'CANCELLED';
export type MatchStatus = 'PLANNED' | 'IN_PROGRESS' | 'PAUSED' | 'FINISHED' | 'CANCELLED';
export type MvpVotingParticipantScope = 'ALL' | 'PLAYERS_ONLY';

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
  manualPhotoUrl?: string | null;
  telegramPhotoUrl?: string | null;
  homeCity: string | null;
  birthDate: string | null;
  defaultPosition: PlayerPosition | null;
  rating: number;
  stats: {
    goals: number;
    assists: number;
  };
  sessions: Array<{
    sessionId: number;
    title: string;
    sessionDate: string;
    sessionTime: string;
    status: SessionStatus;
  }>;
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

export interface TelegramKnownChat {
  chatId: number;
  title: string | null;
  username: string | null;
  chatType: string;
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
  sessionTime: string;
  location: string | null;
  locationAddress: string | null;
  locationUrl: string | null;
  venueId: number | null;
  venuePhotoUrl: string | null;
  broadcastUrl: string | null;
  telegramChatId: number | null;
  telegramChatTitle: string | null;
  telegramRegistrationMessageId: number | null;
  autoStartRegistration: boolean;
  registrationOpenHoursBefore: number | null;
  telegramContributionMessageId: number | null;
  mvpVotingEnabled: boolean;
  mvpVotingDurationHours: number | null;
  mvpVotingParticipantScope: MvpVotingParticipantScope;
  mvpVotingTelegramEnabled: boolean;
  mvpVotingStartedAt: string | null;
  mvpVotingEndsAt: string | null;
  telegramMvpVotingMessageId: number | null;
  telegramMvpResultSentAt: string | null;
  recurrenceRuleId: number | null;
  recurrenceType: SessionRecurrenceType | null;
  recurrenceIntervalDays: number | null;
  recurrenceDayOfMonth: number | null;
  recurrenceActive: boolean | null;
  feeAmount: number | null;
  feeRecipient: string | null;
  formatType: SessionFormatType;
  status: SessionStatus;
  plannedMatchDurationMinutes: number | null;
  sessionDurationMinutes: number | null;
  maxPlayers: number | null;
  teamCount: number | null;
  playerFormat: string | null;
  notes: string | null;
  createdByUserId: number | null;
  createdAt: string;
  startedAt: string | null;
  endedAt: string | null;
  teams: SessionTeam[];
}

export interface SessionVenue {
  id: number;
  name: string;
  address: string | null;
  gisUrl: string | null;
  photoUrl: string | null;
  createdAt: string;
}

export interface FileUploadResponse {
  url: string;
}

export interface ContributionReminder {
  id: number;
  sessionId: number;
  hoursBefore: number;
  sentAt: string | null;
}

export interface ContributionStatus {
  playerId: number;
  displayName: string;
  paid: boolean;
}

export interface SessionMvpCandidate {
  playerId: number;
  firstName: string;
  lastName: string | null;
  displayName: string | null;
  photoUrl: string | null;
  telegramPhotoUrl: string | null;
  position: PlayerPosition | null;
  teamId: number | null;
  teamName: string | null;
  teamColor: string | null;
  goals: number;
  assists: number;
  votes: number;
}

export interface SessionMvpVoting {
  sessionId: number;
  enabled: boolean;
  started: boolean;
  finished: boolean;
  startedAt: string | null;
  endsAt: string | null;
  participantScope: MvpVotingParticipantScope;
  canVote: boolean;
  cannotVoteReason: string | null;
  selectedPlayerId: number | null;
  candidates: SessionMvpCandidate[];
  winners: SessionMvpCandidate[];
}

export interface SessionPlayer {
  id: number;
  sessionId: number;
  playerId: number;
  firstName: string;
  lastName: string | null;
  nickname: string | null;
  displayName: string | null;
  homeCity: string | null;
  photoUrl: string | null;
  telegramPhotoUrl: string | null;
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
  displayName: string | null;
  homeCity: string | null;
  photoUrl: string | null;
  telegramPhotoUrl: string | null;
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
  playerDisplayName: string | null;
  playerUsername: string | null;
  photoUrl: string | null;
  telegramPhotoUrl: string | null;
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
  roundNumber: number | null;
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
  playerDisplayName: string | null;
  playerUsername: string | null;
  playerPhotoUrl: string | null;
  playerTelegramPhotoUrl: string | null;
  relatedPlayerId: number | null;
  relatedPlayerName: string | null;
  relatedPlayerDisplayName: string | null;
  relatedPlayerUsername: string | null;
  relatedPlayerPhotoUrl: string | null;
  relatedPlayerTelegramPhotoUrl: string | null;
  linkedEventId: number | null;
  minuteInMatch: number | null;
  secondInMatch: number | null;
  eventTime: string;
  createdByUserId: number | null;
  payload: Record<string, unknown> | null;
  createdAt: string;
}

export interface MatchPlayer {
  id: number;
  matchId: number;
  teamId: number;
  teamName: string;
  playerId: number;
  playerName: string;
  playerDisplayName: string | null;
  playerUsername: string | null;
  playerPhotoUrl: string | null;
  playerTelegramPhotoUrl: string | null;
  startedAt: string;
  endedAt: string | null;
  source: string;
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

export interface StreamBroadcast {
  id: number;
  sessionId: number;
  title: string | null;
  youtubeVideoId: string | null;
  youtubeBroadcastId: string | null;
  streamStartedAt: string;
  streamEndedAt: string | null;
  timelineShiftSeconds: number;
  createdAt: string;
  updatedAt: string;
}

export interface StreamTimelineItem {
  eventId: number;
  matchId: number;
  matchNumber: number | null;
  roundNumber: number | null;
  eventType: string;
  streamOffsetSeconds: number | null;
  adjustedStreamOffsetSeconds: number | null;
  timecode: string;
  playerName: string | null;
  relatedPlayerName: string | null;
  teamName: string | null;
  text: string;
}

export interface StreamTimeline {
  streamId: number;
  sessionId: number;
  youtubeVideoId: string | null;
  timelineShiftSeconds: number;
  descriptionBlock: string;
  items: StreamTimelineItem[];
}

export interface OverlayTeam {
  id: number;
  name: string;
  color: string | null;
  displayOrder: number | null;
  players: SessionTeamPlayer[];
}

export interface OverlayState {
  sessionId: number;
  serverTime: string;
  currentMatch: SessionMatch | null;
  inProgressMatches: SessionMatch[];
  matches: SessionMatch[];
  teams: OverlayTeam[];
  currentMatchEvents: MatchEvent[];
  sessionEvents: MatchEvent[];
  standings?: SessionStandingsRow[];
  lastFinishedMatch?: SessionMatch | null;
  nextMatch?: SessionMatch | null;
  lastFinishedMatchEvents?: MatchEvent[];
}

export type OverlayEventType =
  | 'CONNECTED'
  | 'MATCH_STARTED'
  | 'MATCH_FINISHED'
  | 'MATCH_PAUSED'
  | 'MATCH_RESUMED'
  | 'GOAL_RECORDED'
  | 'SUBSTITUTION_RECORDED'
  | 'GOAL_CANCELLED';

export interface OverlayEvent {
  type: OverlayEventType;
  sessionId: number;
  matchId: number | null;
  state: OverlayState;
  event: MatchEvent | null;
  assistEvent: MatchEvent | null;
  emittedAt: string;
}
