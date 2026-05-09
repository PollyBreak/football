import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import './styles.css';
import { bootstrapTelegramAuth } from './lib/auth';
import { initTelegramWebApp } from './lib/telegram';

initTelegramWebApp();

bootstrapTelegramAuth().finally(() => {
  createApp(App).use(router).mount('#app');
});
