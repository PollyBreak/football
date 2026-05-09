import type { MatchStatus, PlayerPosition, SessionFormatType, SessionStatus } from '../types';

export function playerPositionLabel(position: PlayerPosition | null | undefined): string {
  const labels: Record<PlayerPosition, string> = {
    GOALKEEPER: 'Вратарь',
    DEFENDER: 'Защитник',
    MIDFIELDER: 'Полузащитник',
    FORWARD: 'Нападающий',
    UNIVERSAL: 'Универсал'
  };

  return position ? labels[position] : 'Не указана';
}

export function sessionFormatLabel(format: SessionFormatType): string {
  const labels: Record<SessionFormatType, string> = {
    ROUND_ROBIN: 'Круговой турнир',
    KING_OF_THE_HILL: 'Царь горы',
    CUSTOM: 'Свободный формат'
  };

  return labels[format];
}

export function sessionStatusLabel(status: SessionStatus): string {
  const labels: Record<SessionStatus, string> = {
    PLANNED: 'Запланирована',
    IN_PROGRESS: 'Идет',
    FINISHED: 'Завершена',
    CANCELLED: 'Отменена'
  };

  return labels[status];
}

export function matchStatusLabel(status: MatchStatus): string {
  const labels: Record<MatchStatus, string> = {
    PLANNED: 'Запланирован',
    IN_PROGRESS: 'Идет',
    FINISHED: 'Завершен',
    CANCELLED: 'Отменен'
  };

  return labels[status];
}

export function matchEventLabel(type: string): string {
  const labels: Record<string, string> = {
    GOAL: 'Гол',
    ASSIST: 'Передача'
  };

  return labels[type] ?? type;
}
