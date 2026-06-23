import { createRouter, createWebHistory } from 'vue-router';
import PlayersView from '../views/PlayersView.vue';
import PlayerDetailsView from '../views/PlayerDetailsView.vue';
import SessionsView from '../views/SessionsView.vue';
import CreateSessionView from '../views/CreateSessionView.vue';
import SessionDetailsView from '../views/SessionDetailsView.vue';
import MatchDetailsView from '../views/MatchDetailsView.vue';
import SessionMvpVotingView from '../views/SessionMvpVotingView.vue';
import OnboardingView from '../views/OnboardingView.vue';
import OverlayView from '../views/OverlayView.vue';
import ProfileView from '../views/ProfileView.vue';
import { authState } from '../lib/auth';
import { startParamTargetPath } from '../lib/telegram';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/sessions' },
    { path: '/onboarding', component: OnboardingView },
    { path: '/profile', component: ProfileView },
    { path: '/players', component: PlayersView },
    { path: '/players/:playerId', component: PlayerDetailsView, props: true },
    { path: '/sessions', component: SessionsView },
    { path: '/sessions/new', component: CreateSessionView },
    { path: '/overlay/sessions/:sessionId', component: OverlayView, props: true },
    { path: '/sessions/:sessionId/mvp', component: SessionMvpVotingView, props: true },
    { path: '/sessions/:sessionId/matches/:matchId', component: MatchDetailsView, props: true },
    { path: '/sessions/:sessionId', component: SessionDetailsView, props: true }
  ]
});

router.beforeEach((to, from) => {
  if (to.path.startsWith('/overlay/')) {
    return true;
  }

  if (!authState.authenticated || !authState.user) {
    return true;
  }

  if (authState.onboardingRequired && to.path !== '/onboarding') {
    return '/onboarding';
  }

  if (!authState.onboardingRequired && authState.player && to.path === '/onboarding') {
    return '/sessions';
  }

  if (!authState.player && to.path !== '/onboarding') {
    return '/onboarding';
  }

  const startTargetPath = startParamTargetPath();
  const isInitialDefaultRoute = to.path === '/' || (to.path === '/sessions' && from.path === '/');
  if (authState.player && startTargetPath && to.path !== startTargetPath && (isInitialDefaultRoute || to.path === '/onboarding')) {
    return startTargetPath;
  }

  return true;
});

export default router;
