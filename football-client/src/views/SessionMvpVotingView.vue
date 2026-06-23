<template>
  <section class="stack">
    <RouterLink class="ghost-button back-button" :to="`/sessions/${sessionId}`">Назад к сессии</RouterLink>

    <div class="card stack-sm">
      <div class="section-header">
        <div>
          <h2 class="section-title">Голосование за MVP</h2>
        </div>
        <button v-if="canRevote" class="ghost-button" type="button" @click="revoteMode = true">Переголосовать</button>
      </div>

      <p v-if="voting?.endsAt && !votingFinished" class="muted">
        До конца голосования осталось <strong class="countdown-accent">{{ remainingLabel }}</strong>
      </p>
      <p v-else-if="votingFinished" class="muted">Голосование завершено.</p>
      <p class="muted">Выберите 1 самого полезного игрока за сегодня и нажмите на галочку рядом с его именем.</p>
      <p v-if="voting && !voting.canVote && voting.cannotVoteReason" class="error-text">{{ voting.cannotVoteReason }}</p>
    </div>

    <div v-if="voting?.winners.length && votingFinished" class="card stack-sm">
      <div class="section-header">
        <h3 class="section-title">MVP</h3>
      </div>
      <article v-for="winner in voting.winners" :key="winner.playerId" class="list-item">
        <div class="list-item__lead">
          <div class="player-avatar player-avatar--sm">
            <img v-if="winner.photoUrl" :src="winner.photoUrl" alt="Фото игрока" />
            <span v-else>{{ candidateInitials(winner) }}</span>
          </div>
          <div>
            <strong>{{ candidateDisplayName(winner) }}</strong>
            <p class="muted">{{ candidateStats(winner) }} · {{ winner.votes }} голосов</p>
          </div>
        </div>
      </article>
    </div>

    <div v-if="voting" class="stack-sm">
      <section v-for="group in groupedCandidates" :key="group.key" class="card stack-sm">
        <div class="team-block__header">
          <strong>{{ group.title }}</strong>
        </div>
        <article v-for="candidate in group.players" :key="candidate.playerId" class="list-item mvp-candidate-item">
          <span class="mvp-vote-count">{{ candidate.votes }}</span>
          <div class="list-item__lead">
            <div class="player-avatar player-avatar--xs">
              <img v-if="candidate.photoUrl" :src="candidate.photoUrl" alt="Фото игрока" />
              <span v-else>{{ candidateInitials(candidate) }}</span>
            </div>
            <div>
              <p class="mvp-candidate-name">
                <strong>{{ candidateDisplayName(candidate) }}</strong>
                <span>{{ candidateFirstName(candidate) }}</span>
              </p>
              <p class="session-player-stats mvp-candidate-stats">{{ candidate.goals }} ⚽ {{ candidate.assists }} 👟</p>
            </div>
          </div>
          <button
            class="mvp-vote-check"
            type="button"
            :class="{ 'is-selected': voting.selectedPlayerId === candidate.playerId }"
            :disabled="!canPickCandidate"
            :aria-label="`Проголосовать за ${candidateDisplayName(candidate)}`"
            @click="requestVote(candidate)"
          >
            ✓
          </button>
        </article>
      </section>
    </div>

    <div v-if="voteDialogCandidate" class="settings-overlay" @click.self="closeVoteDialog">
      <div class="settings-window stack-sm">
        <div>
          <p class="eyebrow">Голосование</p>
          <h3 class="section-title">Подтвердить голос?</h3>
        </div>
        <p class="muted">
          Точно вы хотите проголосовать за {{ candidateDisplayName(voteDialogCandidate) }}
          ({{ candidateFullName(voteDialogCandidate) }}) в голосовании за MVP?
        </p>
        <p v-if="error" class="error-text">{{ error }}</p>
        <div class="button-row">
          <button class="ghost-button" type="button" :disabled="pendingVote" @click="closeVoteDialog">Нет</button>
          <button class="primary-button" type="button" :disabled="pendingVote" @click="confirmVote">Да</button>
        </div>
      </div>
    </div>

    <p v-if="error && !voteDialogCandidate" class="error-text">{{ error }}</p>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { api } from '../lib/api';
import { authState } from '../lib/auth';
import type { SessionMvpCandidate, SessionMvpVoting } from '../types';

const props = defineProps<{ sessionId: string }>();
const sessionIdNumber = computed(() => Number(props.sessionId));
const voting = ref<SessionMvpVoting | null>(null);
const voteDialogCandidate = ref<SessionMvpCandidate | null>(null);
const revoteMode = ref(false);
const pendingVote = ref(false);
const error = ref('');
const now = ref(Date.now());
let timerId: number | undefined;

const remainingLabel = computed(() => {
  if (!voting.value?.endsAt) {
    return '';
  }
  const ms = Math.max(0, new Date(voting.value.endsAt).getTime() - now.value);
  const totalMinutes = Math.ceil(ms / 60000);
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;
  if (hours <= 0) {
    return `${minutes} мин.`;
  }
  return `${hours} ч. ${minutes.toString().padStart(2, '0')} мин.`;
});

const canRevote = computed(() => {
  return Boolean(voting.value?.canVote && voting.value.selectedPlayerId && !votingFinished.value && !revoteMode.value);
});
const canPickCandidate = computed(() => {
  return Boolean(voting.value?.canVote && !votingFinished.value && (!voting.value.selectedPlayerId || revoteMode.value));
});
const votingFinished = computed(() => {
  if (voting.value?.finished) {
    return true;
  }
  return Boolean(voting.value?.endsAt && new Date(voting.value.endsAt).getTime() <= now.value);
});
const groupedCandidates = computed(() => {
  const groups = new Map<string, { key: string; title: string; players: SessionMvpCandidate[] }>();
  for (const candidate of voting.value?.candidates ?? []) {
    const key = candidate.teamId ? `team-${candidate.teamId}` : 'unassigned';
    if (!groups.has(key)) {
      groups.set(key, {
        key,
        title: candidate.teamName ?? 'Без команды',
        players: []
      });
    }
    groups.get(key)!.players.push(candidate);
  }
  return Array.from(groups.values());
});

function candidateDisplayName(candidate: SessionMvpCandidate): string {
  return candidate.displayName?.trim() || candidateFullName(candidate);
}

function candidateFullName(candidate: SessionMvpCandidate): string {
  return `${candidate.firstName} ${candidate.lastName ?? ''}`.trim() || 'Игрок';
}

function candidateFirstName(candidate: SessionMvpCandidate): string {
  return candidate.firstName?.trim() || candidateFullName(candidate);
}

function candidateInitials(candidate: SessionMvpCandidate): string {
  return candidateDisplayName(candidate)
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0])
    .join('') || 'M';
}

function candidateStats(candidate: SessionMvpCandidate): string {
  const parts = [];
  if (candidate.goals > 0) parts.push(`${candidate.goals} ⚽`);
  if (candidate.assists > 0) parts.push(`${candidate.assists} 👟`);
  return parts.length ? parts.join(' ') : 'без голов и передач';
}

async function loadVoting() {
  voting.value = await api.getSessionMvpVoting(sessionIdNumber.value, authState.user?.id ?? null);
}

function requestVote(candidate: SessionMvpCandidate) {
  if (!canPickCandidate.value) {
    return;
  }
  voteDialogCandidate.value = candidate;
  error.value = '';
}

function closeVoteDialog() {
  if (pendingVote.value) {
    return;
  }
  voteDialogCandidate.value = null;
  error.value = '';
}

async function confirmVote() {
  if (!voteDialogCandidate.value || !authState.user?.id) {
    error.value = 'Откройте приложение через Telegram Mini App';
    return;
  }
  pendingVote.value = true;
  error.value = '';
  try {
    voting.value = await api.voteForSessionMvp(sessionIdNumber.value, {
      userId: authState.user.id,
      playerId: voteDialogCandidate.value.playerId
    });
    revoteMode.value = false;
    voteDialogCandidate.value = null;
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Не удалось записать голос';
  } finally {
    pendingVote.value = false;
  }
}

onMounted(async () => {
  try {
    await loadVoting();
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Не удалось загрузить голосование';
  }
  timerId = window.setInterval(() => {
    now.value = Date.now();
    if (voting.value?.endsAt && !voting.value.finished && new Date(voting.value.endsAt).getTime() <= now.value) {
      void loadVoting();
    }
  }, 30000);
});

onUnmounted(() => {
  if (timerId) {
    window.clearInterval(timerId);
  }
});
</script>
