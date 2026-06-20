<template>
  <section class="stack" v-if="session">
    <div class="card hero-card">
      <div class="session-hero-main">
        <div class="session-hero-heading">
          <div v-if="sessionVenuePhotoUrl && !sessionVenuePhotoFailed" class="session-venue-photo">
            <img :src="sessionVenuePhotoUrl" alt="Фото поля" @error="sessionVenuePhotoFailed = true" />
          </div>
          <div class="session-hero-info">
            <p class="eyebrow">Сессия</p>
            <h2 class="section-title">{{ session.title }}</h2>
          </div>
        </div>
        <div class="session-hero-facts">
          <p>🕒 {{ sessionScheduleText }}</p>
          <p>
            📍 {{ sessionLocationText }}
            <a v-if="session.locationUrl" class="map-link" :href="session.locationUrl" target="_blank" rel="noreferrer">
              Адрес на карте
            </a>
          </p>
          <p class="session-hero-inline-facts">
            <span>⚽ Игроки {{ sessionPlayers.length }}/{{ session.maxPlayers || 'без лимита' }}</span>
            <span>🏆 {{ sessionFormatShortLabel }}</span>
          </p>
        </div>
        <div class="hero-utility-row">
          <button class="ghost-button admin-hero-button" type="button" @click="openAdminDialog">
            Администрирование
          </button>
          <button class="ghost-button overlay-hero-button" type="button" @click="overlayDialogOpen = true">
            Overlay
          </button>
          <button class="ghost-button stream-hero-button" type="button" :disabled="sessionIsFinished || pendingStreamStart" @click="streamDialogOpen = true">
            Stream
          </button>
        </div>
        <a v-if="session.broadcastUrl" class="broadcast-link" :href="session.broadcastUrl" target="_blank" rel="noreferrer">
          Открыть трансляцию
        </a>
      </div>
      <div class="hero-actions">
        <span class="status-pill">{{ sessionStatusLabel(session.status) }}</span>
        <button class="icon-button" type="button" aria-label="Настройки сессии" :disabled="sessionIsFinished" @click="settingsOpen = true">
          &#9881;
        </button>
      </div>
    </div>

    <div v-if="sessionIsFinished" class="card stack-sm session-results-card">
      <div class="section-header">
        <h3 class="section-title">Результаты</h3>
      </div>

      <div v-if="resultPodium.length" class="session-results-podium">
        <div v-for="team in resultPodium" :key="team.teamId" class="session-results-podium__row">
          <span class="session-results-podium__medal">{{ team.medal }}</span>
          <strong>{{ team.teamName }}</strong>
          <span class="session-results-podium__color">{{ team.colorEmoji }}</span>
          <span class="muted session-results-podium__stats">({{ team.points }} {{ pointsLabel(team.points) }} {{ team.goalsFor }}|{{ team.goalsAgainst }} разница {{ goalDifferenceLabel(team.goalDifference) }})</span>
        </div>
      </div>
      <p v-else class="muted">Результаты появятся после матчей.</p>

      <div class="session-results-leaders">
        <div class="session-results-leader">
          <p class="eyebrow">Бомбардир</p>
          <template v-if="topScorer">
            <div class="session-results-leader__person">
              <div class="player-avatar player-avatar--sm">
                <img v-if="topScorer.photoUrl" :src="topScorer.photoUrl" alt="Фото игрока" />
                <span v-else>{{ resultPlayerInitials(topScorer.name) }}</span>
              </div>
              <div>
                <strong>{{ topScorer.name }}</strong>
                <p class="muted">{{ topScorer.goals }} ⚽</p>
              </div>
            </div>
          </template>
          <p v-else class="muted">Пока нет голов.</p>
        </div>

        <div class="session-results-leader">
          <p class="eyebrow">MVP</p>
          <template v-if="mvpPlayer">
            <div class="session-results-leader__person">
              <div class="player-avatar player-avatar--sm">
                <img v-if="mvpPlayer.photoUrl" :src="mvpPlayer.photoUrl" alt="Фото игрока" />
                <span v-else>{{ resultPlayerInitials(mvpPlayer.name) }}</span>
              </div>
              <div>
                <strong>{{ mvpPlayer.name }}</strong>
                <p class="muted">{{ mvpPlayer.goals }} ⚽ {{ mvpPlayer.assists }} 👟</p>
              </div>
            </div>
          </template>
          <p v-else class="muted">Пока нет статистики.</p>
        </div>
      </div>
    </div>

    <div v-if="settingsOpen" class="settings-overlay" @click.self="settingsOpen = false">
      <div class="settings-window">
        <div class="section-header">
          <button class="ghost-button" type="button" @click="settingsOpen = false">Назад</button>
          <button class="primary-button" type="button" @click="saveSessionSettings" :disabled="pendingSessionUpdate || sessionIsFinished">Сохранить</button>
        </div>
        <div class="settings-modal-body">
          <div>
            <p class="eyebrow">Настройки</p>
            <h3 class="section-title">Сессия</h3>
          </div>

          <div class="settings-group">
            <p class="settings-group__title">Основное</p>
            <label class="field-label">
              <span>Название сессии</span>
              <input v-model="sessionSettings.title" class="input" placeholder="Название сессии" :disabled="sessionIsFinished" />
            </label>
            <div class="date-time-row">
              <label class="field-label">
                <span>Дата сессии</span>
                <input v-model="sessionSettings.sessionDate" class="input" type="date" :disabled="sessionIsFinished" />
              </label>
              <label class="field-label">
                <span>Время сессии</span>
                <input v-model="sessionSettings.sessionTime" class="input" type="time" :disabled="sessionIsFinished" />
              </label>
            </div>
          </div>

          <div class="settings-group">
            <p class="settings-group__title">Место</p>
            <label class="field-label">
              <span>Место</span>
              <input v-model="sessionSettings.location" class="input" placeholder="Место" :disabled="sessionIsFinished" />
            </label>
            <label class="field-label">
              <span>Адрес поля</span>
              <input v-model="sessionSettings.locationAddress" class="input" placeholder="Улица, дом" :disabled="sessionIsFinished" />
            </label>
            <label class="field-label">
              <span>Ссылка на поле на 2GIS / Google Maps / Яндекс картах</span>
              <input v-model="sessionSettings.locationUrl" class="input" type="url" placeholder="https://..." :disabled="sessionIsFinished" />
            </label>
          </div>

          <div class="settings-group">
            <p class="settings-group__title">Формат игры</p>
            <div class="format-metrics-row">
              <label class="field-label">
                <span>Длительность матча, минут</span>
                <input v-model.number="sessionSettings.plannedMatchDurationMinutes" class="input" type="number" min="1" :disabled="sessionIsFinished" />
              </label>
              <label class="field-label">
                <span>Длительность сессии, минут</span>
                <input v-model.number="sessionSettings.sessionDurationMinutes" class="input" type="number" min="1" :disabled="sessionIsFinished" />
              </label>
              <label class="field-label">
                <span>Максимум игроков</span>
                <input v-model.number="sessionSettings.maxPlayers" class="input" type="number" min="1" :disabled="sessionIsFinished" />
              </label>
            </div>
            <label class="field-label">
              <span>Формат игроков</span>
              <input v-model="sessionSettings.playerFormat" class="input" placeholder="6x6" :disabled="sessionIsFinished" />
            </label>
          </div>

          <div class="settings-group">
            <p class="settings-group__title">Оплата</p>
            <label class="field-label">
              <span>Взнос, тенге</span>
              <input v-model.number="sessionSettings.feeAmount" class="input" type="number" min="0" :disabled="sessionIsFinished" />
            </label>
            <label class="field-label">
              <span>Кому скидывать взнос</span>
              <input v-model="sessionSettings.feeRecipient" class="input" placeholder="Kaspi / имя / телефон" :disabled="sessionIsFinished" />
            </label>
            <div class="field-label reminder-settings reminder-settings--compact">
              <span class="reminder-settings__title">Настройте напоминания по взносам</span>
              <p class="reminder-settings__hint">Уведомления будут приходить перед игрой за ...</p>
              <div class="reminder-options-row">
                <label v-for="hours in contributionReminderOptionHours" :key="`modal-${hours}`" class="reminder-checkbox">
                  <span>{{ hours }} ч.</span>
                  <input
                    type="checkbox"
                    :checked="hasContributionReminder(hours)"
                    :disabled="pendingReminderUpdate || sessionIsFinished"
                    @change="toggleContributionReminder(hours, ($event.target as HTMLInputElement).checked)"
                  />
                </label>
                <form class="reminder-form reminder-form--compact" @submit.prevent="addCustomContributionReminderOption()">
                  <input
                    v-model.number="reminderForm.hoursBefore"
                    class="input"
                    type="number"
                    min="1"
                    placeholder="Свое"
                    :disabled="pendingReminderUpdate || sessionIsFinished"
                  />
                  <button class="ghost-button reminder-add-button" type="submit" :disabled="pendingReminderUpdate || sessionIsFinished">+</button>
                </form>
              </div>
            </div>
          </div>

          <div class="settings-group">
            <p class="settings-group__title">Дополнительно</p>
            <label class="field-label">
              <span>Telegram chat ID</span>
              <input v-model.number="sessionSettings.telegramChatId" class="input" type="number" placeholder="-100..." :disabled="sessionIsFinished" />
            </label>
            <label class="field-label">
              <span>Заметки</span>
              <textarea v-model="sessionSettings.notes" class="input textarea" placeholder="Заметки" :disabled="sessionIsFinished"></textarea>
            </label>
          </div>

          <div class="settings-group">
            <p class="settings-group__title">Трансляция</p>
            <label class="field-label">
              <span>Ссылка на трансляцию</span>
              <input v-model="sessionSettings.broadcastUrl" class="input" type="url" placeholder="https://..." :disabled="sessionIsFinished" />
            </label>
            <div class="stream-shift-card">
              <label class="reminder-checkbox">
                <span>Сдвиг вперед</span>
                <input v-model="streamShiftForward" type="checkbox" :disabled="pendingStreamShift" />
              </label>
              <label class="field-label">
                <span>Секунд</span>
                <input v-model.number="streamShiftSeconds" class="input" type="number" min="1" placeholder="10" :disabled="pendingStreamShift" />
              </label>
              <button class="ghost-button" type="button" :disabled="pendingStreamShift || !streamShiftSeconds" @click="applyStreamShift">Применить сдвиг</button>
            </div>
            <button class="ghost-button stream-timeline-button" type="button" :disabled="pendingTimelineGeneration" @click="generateStreamTimeline">
              Сгенерировать тайм-коды для трансляции
            </button>
            <div v-if="generatedTimelineText" class="stream-timeline-result">
              <textarea class="input textarea" readonly :value="generatedTimelineText"></textarea>
              <button class="ghost-button" type="button" @click="copyGeneratedTimeline">Скопировать</button>
            </div>
          </div>

          <button class="primary-button settings-bottom-save" type="button" @click="saveSessionSettings" :disabled="pendingSessionUpdate || sessionIsFinished">Сохранить</button>
        </div>
      </div>
    </div>

    <div v-if="resumePasswordDialogOpen" class="settings-overlay" @click.self="closeResumeSessionDialog">
      <form class="settings-window stack-sm" @submit.prevent="resumeSession">
        <div>
          <p class="eyebrow">Возобновление</p>
          <h3 class="section-title">Введите пароль</h3>
        </div>
        <label class="field-label">
          <span>Пароль</span>
          <input
            v-model="resumePassword"
            class="input"
            type="password"
            inputmode="numeric"
            autocomplete="off"
            placeholder="Пароль"
            :disabled="pendingSessionUpdate"
          />
        </label>
        <p v-if="resumePasswordError" class="error-text">{{ resumePasswordError }}</p>
        <div class="button-row">
          <button class="ghost-button" type="button" @click="closeResumeSessionDialog" :disabled="pendingSessionUpdate">Отмена</button>
          <button class="primary-button" type="submit" :disabled="pendingSessionUpdate">Возобновить</button>
        </div>
      </form>
    </div>

    <div v-if="adminDialogOpen" class="settings-overlay" @click.self="closeAdminDialog">
      <div class="settings-window stack-sm">
        <div class="section-header">
          <div>
            <p class="eyebrow">Администрирование</p>
            <h3 class="section-title">Управление сессией</h3>
          </div>
          <button class="ghost-button" type="button" @click="closeAdminDialog">Закрыть</button>
        </div>
        <form v-if="!adminUnlocked" class="admin-password-row" @submit.prevent="unlockAdminPanel">
          <input v-model="adminPassword" class="input" type="password" placeholder="Пароль" autocomplete="off" />
          <button class="primary-button" type="submit">Открыть</button>
        </form>
        <div v-else class="admin-tools">
          <div class="admin-actions">
            <button class="ghost-button" type="button" :disabled="pendingRegistrationStart || pendingSessionUpdate || sessionIsFinished" @click="startRegistration">
              Начать регистрацию
            </button>
            <button class="ghost-button" type="button" :disabled="pendingContributionStart || pendingSessionUpdate || sessionIsFinished" @click="startContributionCollection">
              Начать сбор взносов
            </button>
          </div>
          <div class="contribution-status-strip" v-if="contributionStatuses.length">
            <span v-for="status in contributionStatuses" :key="status.playerId" class="contribution-status-chip" :class="{ 'is-paid': status.paid }">
              {{ status.paid ? '✅' : '❌' }} {{ status.displayName }}
            </span>
          </div>
          <p v-else class="muted admin-panel__muted">Нет игроков для списка взносов.</p>
        </div>
      </div>
    </div>

    <div v-if="overlayDialogOpen" class="settings-overlay" @click.self="overlayDialogOpen = false">
      <div class="settings-window stack-sm">
        <div class="section-header">
          <div>
            <p class="eyebrow">Overlay</p>
            <h3 class="section-title">Ссылка на оверлей</h3>
          </div>
          <button class="ghost-button" type="button" @click="overlayDialogOpen = false">Закрыть</button>
        </div>
        <input class="input" :value="overlayPageUrl" readonly />
        <div class="button-row">
          <a class="ghost-button overlay-link-button" :href="overlayPageUrl" target="_blank" rel="noreferrer">Открыть</a>
          <button class="primary-button" type="button" @click="copyOverlayUrl">Скопировать</button>
        </div>
      </div>
    </div>

    <div v-if="streamDialogOpen" class="settings-overlay" @click.self="closeStreamDialog">
      <div class="settings-window stack-sm">
        <div class="section-header">
          <div>
            <p class="eyebrow">Stream</p>
            <h3 class="section-title">Начать трансляцию</h3>
          </div>
        </div>
        <div class="stream-instruction">
          <p>Для проведения трансляции ознакомьтесь с инструкцией. Данная кнопка записывает время старта трансляции на YouTube (или другой платформе).</p>
          <ol>
            <li>Подготовьте connection (url/etc) в приложении для записи видеотрансляции.</li>
            <li>Добавьте overlay, ссылку на него можно взять на странице конкретной сессии (бело-зеленая кнопка).</li>
            <li>Запустите запись трансляции. Как только трансляция начнется, сразу нажмите кнопку Stream и подтвердите трансляцию.</li>
          </ol>
          <p>После этого приложение передаст на сервер время начала трансляции на YouTube. Необязательно после этого сразу начинать игры, тайм-коды всех событий (в том числе начало матчей) автоматически подстроятся под это время. Если не получилось сразу нажать на кнопку, то потом в настройках сессии можно будет добавить нужное количество секунд для сдвига вперед или назад.</p>
          <strong>Вы готовы начать трансляцию?</strong>
        </div>
        <div class="button-row">
          <button class="ghost-button" type="button" @click="closeStreamDialog">Нет</button>
          <button class="primary-button" type="button" :disabled="pendingStreamStart" @click="confirmStartStream">Да</button>
        </div>
      </div>
    </div>

    <div class="card stack-sm session-settings-inline">
      <div class="section-header">
        <h3 class="section-title">Настройки сессии</h3>
        <button class="ghost-button" @click="saveSessionSettings" :disabled="pendingSessionUpdate || sessionIsFinished">Сохранить</button>
      </div>
      <label class="field-label">
        <span>Максимум игроков</span>
        <input v-model.number="sessionSettings.maxPlayers" class="input" type="number" min="1" :disabled="sessionIsFinished" />
      </label>
      <label class="field-label">
        <span>Формат игроков</span>
        <input v-model="sessionSettings.playerFormat" class="input" placeholder="6x6" :disabled="sessionIsFinished" />
      </label>
      <label class="field-label">
        <span>Длительность сессии, минут</span>
        <input v-model.number="sessionSettings.sessionDurationMinutes" class="input" type="number" min="1" :disabled="sessionIsFinished" />
      </label>
      <label class="field-label">
        <span>Telegram chat ID</span>
        <input v-model.number="sessionSettings.telegramChatId" class="input" type="number" placeholder="-100..." :disabled="sessionIsFinished" />
      </label>
      <div class="grid-form">
        <label class="field-label">
          <span>Взнос, тенге</span>
          <input v-model.number="sessionSettings.feeAmount" class="input" type="number" min="0" :disabled="sessionIsFinished" />
        </label>
        <label class="field-label">
          <span>Кому скидывать взнос</span>
          <input v-model="sessionSettings.feeRecipient" class="input" placeholder="Kaspi / имя / телефон" :disabled="sessionIsFinished" />
        </label>
      </div>
      <div class="field-label reminder-settings reminder-settings--compact">
        <span class="reminder-settings__title">Настройте напоминания по взносам</span>
        <p class="reminder-settings__hint">Уведомления будут приходить перед игрой за ...</p>
        <div class="reminder-options-row">
          <label v-for="hours in contributionReminderOptionHours" :key="`inline-${hours}`" class="reminder-checkbox">
            <span>{{ hours }} ч.</span>
            <input
              type="checkbox"
              :checked="hasContributionReminder(hours)"
              :disabled="pendingReminderUpdate || sessionIsFinished"
              @change="toggleContributionReminder(hours, ($event.target as HTMLInputElement).checked)"
            />
          </label>
          <form class="reminder-form reminder-form--compact" @submit.prevent="addCustomContributionReminderOption()">
            <input
              v-model.number="reminderForm.hoursBefore"
              class="input"
              type="number"
              min="1"
              placeholder="Свое"
              :disabled="pendingReminderUpdate || sessionIsFinished"
            />
            <button class="ghost-button reminder-add-button" type="submit" :disabled="pendingReminderUpdate || sessionIsFinished">+</button>
          </form>
        </div>
      </div>
    </div>

    <button
      class="primary-button join-session-button"
      :class="{ 'is-danger': currentUserSessionPlayer || currentUserWaitlistEntry }"
      @click="toggleCurrentUserSession"
      :disabled="!authState.player || pendingMembership || sessionIsFinished"
    >
      {{ membershipButtonLabel }}
    </button>

    <div class="tabs">
      <button v-for="tab in tabs" :key="tab" class="tab-button" :class="{ 'is-active': activeTab === tab }" @click="activeTab = tab">
        {{ tabLabels[tab] }}
      </button>
    </div>

    <div v-if="activeTab === 'Players'" class="stack-sm">
      <div class="card stack-sm">
        <div class="section-header">
          <h3 class="section-title">Игроки сессии</h3>
          <button class="ghost-button" @click="refreshAll">Обновить</button>
        </div>
        <div v-if="playersViewLoading" class="session-players-loading" aria-live="polite">
          <div v-for="index in 3" :key="index" class="session-players-loading__item"></div>
        </div>
        <p v-else-if="!sessionPlayers.length" class="muted">Пока нет игроков в сессии.</p>
        <div v-else class="stack-sm">
          <section v-for="group in groupedSessionPlayers" :key="group.key" class="session-player-group">
            <div v-if="group.title" class="session-player-group__header" :style="teamGroupHeaderStyle(group.color)">
              <strong>{{ group.title }}</strong>
            </div>
            <div class="list">
              <article
                v-for="player in group.players"
                :key="player.id"
                class="list-item player-list-item is-clickable"
                role="link"
                tabindex="0"
                @click="openPlayerProfile(player.playerId)"
                @keydown.enter.prevent="openPlayerProfile(player.playerId)"
              >
                <div class="list-item__lead">
                  <div class="player-avatar player-avatar--sm">
                    <img v-if="player.photoUrl" :src="player.photoUrl" alt="Фото игрока" />
                    <span v-else>{{ playerInitials(player) }}</span>
                  </div>
                  <div>
                    <strong>{{ sessionPersonDisplayName(player) }}</strong>
                    <p class="muted">{{ sessionPersonDetails(player) }}</p>
                    <p class="session-player-stats">{{ playerSessionStats(player.playerId).goals }} ⚽ {{ playerSessionStats(player.playerId).assists }} 👟</p>
                  </div>
                </div>
                <span class="item-tag" :aria-label="playerPositionLabel(player.position)">{{ compactPlayerPositionLabel(player.position) }}</span>
              </article>
            </div>
          </section>
        </div>
      </div>

      <div class="card stack-sm">
        <div class="section-header">
          <h3 class="section-title">Очередь</h3>
        </div>
        <p v-if="!waitlist.length" class="muted">Очередь пуста.</p>
        <div v-else class="list">
          <article
            v-for="(entry, index) in waitlist"
            :key="entry.id"
            class="list-item player-list-item is-clickable"
            role="link"
            tabindex="0"
            @click="openPlayerProfile(entry.playerId)"
            @keydown.enter.prevent="openPlayerProfile(entry.playerId)"
          >
            <div class="list-item__lead">
              <div class="player-avatar player-avatar--sm">
                <img v-if="entry.photoUrl" :src="entry.photoUrl" alt="Фото игрока" />
                <span v-else>{{ waitlistInitials(entry) }}</span>
              </div>
              <div>
                <strong>{{ index + 1 }}. {{ sessionPersonDisplayName(entry) }}</strong>
                <p class="muted">{{ sessionPersonDetails(entry) }}</p>
              </div>
            </div>
            <span class="item-tag" :aria-label="playerPositionLabel(entry.position)">{{ compactPlayerPositionLabel(entry.position) }}</span>
          </article>
        </div>
      </div>

      <div class="card stack-sm">
        <div class="section-header">
          <h3 class="section-title">Добавить игрока</h3>
        </div>
        <div class="grid-form">
          <select v-model.number="sessionPlayerForm.playerId" class="input" :disabled="sessionIsFinished">
            <option :value="undefined">Выберите игрока</option>
            <option v-for="player in allPlayers" :key="player.playerId" :value="player.playerId">
              {{ player.firstName }} {{ player.lastName ?? '' }}
            </option>
          </select>
          <select v-model="sessionPlayerForm.position" class="input" :disabled="sessionIsFinished">
            <option v-for="position in positions" :key="position" :value="position">{{ playerPositionLabel(position) }}</option>
          </select>
          <button class="primary-button" @click="addPlayerToSession" :disabled="sessionIsFinished">Добавить в сессию</button>
        </div>
      </div>
    </div>

    <div v-if="activeTab === 'Teams'" class="card stack-sm">
      <div class="section-header">
        <h3 class="section-title">Распределение по командам</h3>
      </div>
      <div v-for="team in session.teams" :key="team.id" class="team-block">
        <div class="team-block__header">
          <strong>{{ team.name }}</strong>
          <button v-if="!sessionIsFinished" class="primary-button" @click="assignSelectedPlayers(team.id)">Назначить выбранных</button>
        </div>
        <div class="chips">
          <label
            v-for="player in sessionPlayers"
            :key="`${team.id}-${player.playerId}`"
            class="chip team-chip"
            :class="{
              'is-assigned': isPlayerAssignedToTeam(team.id, player.playerId),
              'is-locked': isPlayerLockedForTeam(team.id, player.playerId)
            }"
            :style="teamChipStyle(team.id, player.playerId)"
          >
            <input
              type="checkbox"
              :checked="isPlayerSelectedForTeam(team.id, player.playerId)"
              :disabled="sessionIsFinished || isPlayerLockedForTeam(team.id, player.playerId)"
              @change="toggleTeamPlayer(team.id, player.playerId, $event)"
            />
            <span>{{ player.firstName }}</span>
            <small v-if="isPlayerLockedForTeam(team.id, player.playerId)">{{ assignedTeamName(player.playerId) }}</small>
          </label>
        </div>
        <div class="list">
          <article
            v-for="member in teamPlayers[team.id] || []"
            :key="member.id"
            class="list-item player-list-item is-clickable"
            role="link"
            tabindex="0"
            @click="openPlayerProfile(member.playerId)"
            @keydown.enter.prevent="openPlayerProfile(member.playerId)"
          >
            <div class="list-item__lead">
              <img v-if="member.photoUrl" :src="member.photoUrl" alt="Фото игрока" class="avatar avatar--sm" />
              <strong>{{ member.playerName }}</strong>
            </div>
            <span class="item-tag" :aria-label="playerPositionLabel(member.position)">{{ compactPlayerPositionLabel(member.position) }}</span>
          </article>
        </div>
      </div>
    </div>

    <div v-if="activeTab === 'Matches'" class="card stack-sm">
      <div class="section-header">
        <h3 class="section-title">Матчи</h3>
        <button v-if="!sessionIsFinished" class="primary-button" @click="createNextMatch">{{ createMatchButtonLabel }}</button>
      </div>
      <div v-if="session.formatType === 'ROUND_ROBIN'" class="grid-form">
        <label class="field-label">
          <span>Первый матч</span>
          <select v-model="roundRobinFirstPairKey" class="input" :disabled="sessionIsFinished || matches.length > 0">
            <option v-for="pair in roundRobinPairOptions" :key="pair.key" :value="pair.key">{{ pair.label }}</option>
          </select>
        </label>
      </div>
      <div v-if="session.formatType === 'KNOCKOUT'" class="grid-form">
        <label class="field-label">
          <span>Первая команда</span>
          <select v-model.number="knockoutMatchForm.teamAId" class="input" :disabled="sessionIsFinished">
            <option :value="undefined">Выберите команду</option>
            <option v-for="team in session.teams" :key="team.id" :value="team.id">{{ team.name }}</option>
          </select>
        </label>
        <label class="field-label">
          <span>Вторая команда</span>
          <select v-model.number="knockoutMatchForm.teamBId" class="input" :disabled="sessionIsFinished">
            <option :value="undefined">Выберите команду</option>
            <option v-for="team in session.teams" :key="team.id" :value="team.id">{{ team.name }}</option>
          </select>
        </label>
      </div>
      <div class="stack-sm">
        <section v-for="round in matchRounds" :key="round.roundNumber" class="match-round-group">
          <div class="match-round-group__header">
            <strong>{{ round.roundNumber }} круг</strong>
          </div>
          <div class="list">
            <article v-for="match in round.matches" :key="match.id" class="match-card" :class="matchCardStatusClass(match.status)">
              <div class="match-card__top">
                <strong>#{{ match.matchNumber }} {{ match.teamAName }} против {{ match.teamBName }}</strong>
                <span class="status-pill" :class="matchStatusClass(match.status)">{{ matchStatusLabel(match.status) }}</span>
              </div>
              <p class="score-line">{{ match.teamAScore }} : {{ match.teamBScore }}</p>
              <div v-if="matchGoalSummaries(match).length" class="match-goal-summary">
                <div class="match-goal-summary__column match-goal-summary__column--left">
                  <p v-for="goal in teamMatchGoals(match, match.teamAId)" :key="goal.id" class="match-goal-summary__row">
                    <img v-if="goal.playerPhotoUrl" :src="goal.playerPhotoUrl" alt="Р¤РѕС‚Рѕ РёРіСЂРѕРєР°" class="scoreboard-goal-avatar" />
                    <span v-if="goal.timeLabel" class="scoreboard-goal-time">{{ goal.timeLabel }}</span>
                    <span class="scoreboard-goal-name">{{ goal.label }}</span>
                  </p>
                </div>
                <div class="match-goal-summary__column match-goal-summary__column--right">
                  <p v-for="goal in teamMatchGoals(match, match.teamBId)" :key="goal.id" class="match-goal-summary__row">
                    <img v-if="goal.playerPhotoUrl" :src="goal.playerPhotoUrl" alt="Р¤РѕС‚Рѕ РёРіСЂРѕРєР°" class="scoreboard-goal-avatar" />
                    <span v-if="goal.timeLabel" class="scoreboard-goal-time">{{ goal.timeLabel }}</span>
                    <span class="scoreboard-goal-name">{{ goal.label }}</span>
                  </p>
                </div>
              </div>
              <div class="button-row">
                <button v-if="!sessionIsFinished" class="ghost-button" @click="startMatch(match.id)" :disabled="match.status !== 'PLANNED'">Начать</button>
                <button class="ghost-button" @click="openMatch(match.id)">Открыть</button>
                <button v-if="!sessionIsFinished" class="ghost-button" @click="finishMatch(match.id)" :disabled="match.status !== 'IN_PROGRESS'">Завершить</button>
                <button v-if="!sessionIsFinished && match.status === 'FINISHED'" class="ghost-button" @click="resumeMatch(match.id)">Возобновить</button>
              </div>
            </article>
          </div>
        </section>
      </div>
    </div>

    <div v-if="activeTab === 'Standings'" class="card stack-sm">
      <div class="section-header">
        <h3 class="section-title">Таблица</h3>
        <button class="ghost-button" @click="loadStandings">Обновить</button>
      </div>
      <div class="table-scroll">
        <table class="standings-table">
          <thead>
            <tr>
              <th>Команда</th>
              <th>Игры</th>
              <th>Очки</th>
              <th>В</th>
              <th>Н</th>
              <th>П</th>
              <th>ЗГ</th>
              <th>ПГ</th>
              <th>РГ</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in standings" :key="row.teamId">
              <td>{{ row.teamName }}</td>
              <td>{{ row.played }}</td>
              <td>{{ row.points }}</td>
              <td>{{ row.wins }}</td>
              <td>{{ row.draws }}</td>
              <td>{{ row.losses }}</td>
              <td>{{ row.goalsFor }}</td>
              <td>{{ row.goalsAgainst }}</td>
              <td>{{ row.goalDifference }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="standings-chart">
        <div class="section-header">
          <h4 class="section-title">Очки по кругам</h4>
        </div>
        <p v-if="!standingsProgressChart.rounds.length" class="muted">График появится после завершения матчей.</p>
        <template v-else>
          <div class="standings-chart__legend">
            <span
              v-for="series in standingsProgressChart.series"
              :key="series.teamId"
              class="standings-chart__legend-item"
            >
              <span class="standings-chart__legend-dot" :style="{ backgroundColor: series.color }"></span>
              <span>{{ series.teamName }}</span>
            </span>
          </div>
          <svg
            class="standings-chart__svg"
            viewBox="0 0 320 180"
            role="img"
            aria-label="График изменения очков команд по кругам"
          >
            <line
              v-for="tick in standingsProgressChart.yTicks"
              :key="`grid-${tick.value}`"
              :x1="28"
              :y1="tick.y"
              :x2="308"
              :y2="tick.y"
              class="standings-chart__grid-line"
            />
            <line x1="28" y1="12" x2="28" y2="152" class="standings-chart__axis-line" />
            <line x1="28" y1="152" x2="308" y2="152" class="standings-chart__axis-line" />

            <text
              v-for="tick in standingsProgressChart.yTicks"
              :key="`label-${tick.value}`"
              x="22"
              :y="tick.y + 4"
              class="standings-chart__axis-label standings-chart__axis-label--y"
            >
              {{ tick.value }}
            </text>

            <text
              v-for="round in standingsProgressChart.rounds"
              :key="`round-${round.roundNumber}`"
              :x="round.x"
              y="170"
              class="standings-chart__axis-label"
              text-anchor="middle"
            >
              {{ round.label }}
            </text>

            <path
              v-for="series in standingsProgressChart.series"
              :key="`line-${series.teamId}`"
              :d="series.path"
              class="standings-chart__line"
              :style="{ stroke: series.color }"
            />

            <g v-for="series in standingsProgressChart.series" :key="`points-${series.teamId}`">
              <circle
                v-for="point in series.points"
                :key="point.roundNumber"
                :cx="point.x"
                :cy="point.y"
                r="4"
                class="standings-chart__point"
                :style="{ fill: series.color }"
              />
            </g>
          </svg>
        </template>
      </div>
    </div>

    <button
      v-if="!sessionIsFinished"
      class="primary-button finish-session-button is-danger"
      @click="finishSession"
      :disabled="pendingSessionUpdate"
    >
      Завершить
    </button>
    <button
      v-else
      class="primary-button finish-session-button"
      @click="openResumeSessionDialog"
      :disabled="pendingSessionUpdate"
    >
      Возобновить
    </button>

    <p v-if="error" class="error-text">{{ error }}</p>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { api, resolveMediaUrl } from '../lib/api';
import { authState } from '../lib/auth';
import { matchStatusLabel, playerPositionLabel, sessionFormatLabel, sessionStatusLabel } from '../lib/labels';
import { getStartParam } from '../lib/telegram';
import type {
  ContributionReminder,
  ContributionStatus,
  GameSession,
  MatchEvent,
  PlayerPosition,
  PlayerProfile,
  SessionMatch,
  SessionPlayer,
  SessionStandingsRow,
  SessionTeamPlayer,
  SessionWaitlistEntry
} from '../types';

const props = defineProps<{ sessionId: string }>();
const router = useRouter();

const tabs = ['Players', 'Teams', 'Matches', 'Standings'] as const;
const tabLabels: Record<(typeof tabs)[number], string> = {
  Players: 'Игроки',
  Teams: 'Команды',
  Matches: 'Матчи',
  Standings: 'Таблица'
};
const positions: PlayerPosition[] = ['GOALKEEPER', 'DEFENDER', 'MIDFIELDER', 'FORWARD', 'UNIVERSAL'];
const reminderQuickHours = [10, 5, 2];

const session = ref<GameSession | null>(null);
const sessionVenuePhotoFailed = ref(false);
const contributionReminders = ref<ContributionReminder[]>([]);
const contributionStatuses = ref<ContributionStatus[]>([]);
const customContributionReminderHours = ref<number[]>([]);
const sessionPlayers = ref<SessionPlayer[]>([]);
const waitlist = ref<SessionWaitlistEntry[]>([]);
const allPlayers = ref<PlayerProfile[]>([]);
const matches = ref<SessionMatch[]>([]);
const matchEventsByMatchId = ref<Record<number, MatchEvent[]>>({});
const standings = ref<SessionStandingsRow[]>([]);
const teamPlayers = ref<Record<number, SessionTeamPlayer[]>>({});
const selectedPlayersByTeam = reactive<Record<number, number[]>>({});
const roundRobinFirstPairKey = ref('');
const activeTab = ref<(typeof tabs)[number]>('Players');
const error = ref('');
const pendingMembership = ref(false);
const pendingSessionUpdate = ref(false);
const pendingRegistrationStart = ref(false);
const pendingContributionStart = ref(false);
const pendingReminderUpdate = ref(false);
const pendingStreamStart = ref(false);
const pendingStreamShift = ref(false);
const pendingTimelineGeneration = ref(false);
const streamShiftForward = ref(true);
const streamShiftSeconds = ref<number | null>(null);
const generatedTimelineText = ref('');
const settingsOpen = ref(false);
const adminDialogOpen = ref(false);
const overlayDialogOpen = ref(false);
const streamDialogOpen = ref(false);
const adminUnlocked = ref(false);
const adminPassword = ref('');
const playersViewLoading = ref(false);
const resumeSessionPassword = '212229';
const resumePasswordDialogOpen = ref(false);
const resumePassword = ref('');
const resumePasswordError = ref('');
const adminPasswordValue = '12112001';

const sessionIdNumber = computed(() => Number(props.sessionId));
const overlayPageUrl = computed(() => {
  const origin = window.location.origin;
  return `${origin}/overlay/sessions/${sessionIdNumber.value}`;
});
const currentUserSessionPlayer = computed(() => {
  const currentPlayerId = authState.player?.playerId;
  return currentPlayerId
    ? sessionPlayers.value.find((player) => player.playerId === currentPlayerId)
    : undefined;
});
const currentUserWaitlistEntry = computed(() => {
  const currentPlayerId = authState.player?.playerId;
  return currentPlayerId
    ? waitlist.value.find((entry) => entry.playerId === currentPlayerId)
    : undefined;
});
const sessionIsFinished = computed(() => session.value?.status === 'FINISHED');
const sessionVenuePhotoUrl = computed(() => resolveMediaUrl(session.value?.venuePhotoUrl));
const sessionIsFull = computed(() => {
  return Boolean(session.value?.maxPlayers && sessionPlayers.value.length >= session.value.maxPlayers);
});
const sessionScheduleText = computed(() => {
  if (!session.value) return '';
  const date = new Date(`${session.value.sessionDate}T00:00:00`);
  const weekday = capitalizeFirst(new Intl.DateTimeFormat('ru-RU', { weekday: 'short' }).format(date).replace('.', ''));
  const dayMonth = new Intl.DateTimeFormat('ru-RU', { day: 'numeric', month: 'long' }).format(date);
  const startTime = session.value.sessionTime?.slice(0, 5) || '';
  const duration = session.value.sessionDurationMinutes;
  if (!startTime || !duration) {
    return `${weekday}, ${dayMonth}, ${startTime}`.trim();
  }

  const [hours, minutes] = startTime.split(':').map(Number);
  const start = new Date(date);
  start.setHours(hours, minutes, 0, 0);
  const end = new Date(start.getTime() + duration * 60_000);
  const endTime = end.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' });
  return `${weekday}, ${dayMonth}, ${startTime}-${endTime} (${duration} мин)`;
});
const sessionLocationText = computed(() => {
  if (!session.value) return 'Место не указано';
  return [session.value.location, session.value.locationAddress]
    .filter((part): part is string => Boolean(part?.trim()))
    .join(', ') || 'Место не указано';
});
const sessionFormatShortLabel = computed(() => {
  if (!session.value) return '';
  if (session.value.formatType === 'ROUND_ROBIN') return 'Круговой';
  return sessionFormatLabel(session.value.formatType);
});
const membershipButtonLabel = computed(() => {
  if (currentUserSessionPlayer.value) return 'Покинуть игру';
  if (currentUserWaitlistEntry.value) return 'Покинуть очередь';
  return sessionIsFull.value ? 'Встать в очередь' : 'Присоединиться к игре';
});
const hasContributionReminder = (hoursBefore: number) => {
  return contributionReminders.value.some((reminder) => reminder.hoursBefore === hoursBefore);
};
const contributionReminderOptionHours = computed(() => {
  return Array.from(new Set([
    ...reminderQuickHours,
    ...customContributionReminderHours.value,
    ...contributionReminders.value.map((reminder) => reminder.hoursBefore)
  ])).sort((left, right) => right - left);
});

function capitalizeFirst(value: string): string {
  return value ? value.charAt(0).toUpperCase() + value.slice(1) : value;
}

const sessionPlayerForm = reactive({
  playerId: undefined as number | undefined,
  position: 'MIDFIELDER' as PlayerPosition
});
const knockoutMatchForm = reactive({
  teamAId: undefined as number | undefined,
  teamBId: undefined as number | undefined
});
const sessionSettings = reactive({
  title: '',
  sessionDate: '',
  sessionTime: '',
  location: '',
  locationAddress: '',
  locationUrl: '',
  broadcastUrl: '',
  telegramChatId: null as number | null,
  telegramChatTitle: '',
  feeAmount: null as number | null,
  feeRecipient: '',
  plannedMatchDurationMinutes: 6 as number | null,
  sessionDurationMinutes: 90 as number | null,
  notes: '',
  maxPlayers: 15 as number | null,
  playerFormat: '6x6'
});
const reminderForm = reactive({
  hoursBefore: 10 as number | null
});
const createMatchButtonLabel = computed(() => {
  return session.value?.formatType === 'KNOCKOUT' ? 'Создать матч' : 'Создать следующий';
});
const sessionPlayerStats = computed<Record<number, { goals: number; assists: number }>>(() => {
  const stats: Record<number, { goals: number; assists: number }> = {};

  Object.values(matchEventsByMatchId.value)
    .flat()
    .forEach((event) => {
      if (!event.playerId) {
        return;
      }

      if (!stats[event.playerId]) {
        stats[event.playerId] = { goals: 0, assists: 0 };
      }

      if (event.eventType === 'GOAL') {
        stats[event.playerId].goals += 1;
      }

      if (event.eventType === 'ASSIST') {
        stats[event.playerId].assists += 1;
      }
    });

  return stats;
});
const resultPodium = computed(() => {
  const medals = ['🥇', '🥈', '🥉'];

  return standings.value
    .slice()
    .sort((left, right) => {
      return right.points - left.points
        || right.goalDifference - left.goalDifference
        || right.goalsFor - left.goalsFor
        || left.teamName.localeCompare(right.teamName);
    })
    .slice(0, 3)
    .map((team, index) => ({
      ...team,
      medal: medals[index],
      colorEmoji: teamColorEmoji(team.teamColor ?? team.teamName)
    }));
});
const resultPlayerStats = computed(() => {
  const stats = new Map<number, {
    playerId: number;
    name: string;
    photoUrl: string | null;
    goals: number;
    assists: number;
  }>();

  const ensurePlayer = (playerId: number, name: string | null, photoUrl: string | null) => {
    const existing = stats.get(playerId);
    if (existing) {
      if (!existing.photoUrl && photoUrl) {
        existing.photoUrl = photoUrl;
      }
      if (name?.trim() && existing.name === 'Игрок') {
        existing.name = name.trim();
      }
      return existing;
    }

    const player = sessionPlayers.value.find((entry) => entry.playerId === playerId);
    const created = {
      playerId,
      name: player ? sessionPersonDisplayName(player) : name?.trim() || 'Игрок',
      photoUrl: player?.photoUrl ?? photoUrl,
      goals: 0,
      assists: 0
    };
    stats.set(playerId, created);
    return created;
  };

  Object.values(matchEventsByMatchId.value)
    .flat()
    .forEach((event) => {
      if (!event.playerId) {
        return;
      }

      const player = ensurePlayer(event.playerId, event.playerName, event.playerPhotoUrl);
      if (event.eventType === 'GOAL') {
        player.goals += 1;
      }
      if (event.eventType === 'ASSIST') {
        player.assists += 1;
      }
    });

  return Array.from(stats.values());
});
const topScorer = computed(() => {
  return resultPlayerStats.value
    .filter((player) => player.goals > 0)
    .sort((left, right) => {
      return right.goals - left.goals
        || right.assists - left.assists
        || left.name.localeCompare(right.name);
    })[0];
});
const mvpPlayer = computed(() => {
  return resultPlayerStats.value
    .filter((player) => player.goals + player.assists > 0)
    .sort((left, right) => {
      return (right.goals + right.assists) - (left.goals + left.assists)
        || right.goals - left.goals
        || left.name.localeCompare(right.name);
    })[0];
});
const groupedSessionPlayers = computed(() => {
  if (!session.value) {
    return [];
  }

  const assignedPlayerIds = new Set<number>();
  const groups = session.value.teams
    .map((team) => {
      const playerIds = new Set((teamPlayers.value[team.id] ?? []).map((player) => player.playerId));
      playerIds.forEach((playerId) => assignedPlayerIds.add(playerId));

      return {
        key: `team-${team.id}`,
        title: team.name,
        color: team.color ?? null,
        players: sessionPlayers.value.filter((player) => playerIds.has(player.playerId))
      };
    });

  const unassignedPlayers = sessionPlayers.value.filter((player) => !assignedPlayerIds.has(player.playerId));
  if (unassignedPlayers.length) {
    groups.unshift({
      key: 'unassigned',
      title: 'Без команды',
      color: null,
      players: unassignedPlayers
    });
  }

  return groups;
});
const matchRounds = computed(() => {
  const groups = new Map<number, SessionMatch[]>();

  matches.value.forEach((match) => {
    const roundNumber = match.roundNumber ?? fallbackRoundNumber(match.matchNumber);
    const existing = groups.get(roundNumber) ?? [];
    existing.push(match);
    groups.set(roundNumber, existing);
  });

  return Array.from(groups.entries())
    .sort((left, right) => left[0] - right[0])
    .map(([roundNumber, roundMatches]) => ({
      roundNumber,
      matches: roundMatches.sort((left, right) => left.matchNumber - right.matchNumber)
    }));
});
const standingsProgressChart = computed(() => {
  if (!session.value) {
    return {
      rounds: [] as Array<{ roundNumber: number; label: string; x: number }>,
      series: [] as Array<{
        teamId: number;
        teamName: string;
        color: string;
        path: string;
        points: Array<{ roundNumber: number; x: number; y: number; points: number }>;
      }>,
      yTicks: [] as Array<{ value: number; y: number }>
    };
  }

  const chartWidth = 320;
  const chartHeight = 180;
  const paddingLeft = 28;
  const paddingRight = 12;
  const paddingTop = 12;
  const paddingBottom = 28;
  const innerWidth = chartWidth - paddingLeft - paddingRight;
  const innerHeight = chartHeight - paddingTop - paddingBottom;

  const finishedMatches = matches.value
    .filter((match) => match.status === 'FINISHED')
    .slice()
    .sort((left, right) => {
      const leftRound = left.roundNumber ?? fallbackRoundNumber(left.matchNumber);
      const rightRound = right.roundNumber ?? fallbackRoundNumber(right.matchNumber);
      return leftRound - rightRound || left.matchNumber - right.matchNumber;
    });

  const roundNumbers = Array.from(new Set(finishedMatches.map((match) => match.roundNumber ?? fallbackRoundNumber(match.matchNumber))));
  if (!roundNumbers.length) {
    return {
      rounds: [],
      series: [],
      yTicks: []
    };
  }

  const cumulativePoints = new Map<number, number>();
  session.value.teams.forEach((team) => cumulativePoints.set(team.id, 0));

  const roundSnapshots = roundNumbers.map((roundNumber) => {
    const roundMatches = finishedMatches.filter((match) => (match.roundNumber ?? fallbackRoundNumber(match.matchNumber)) === roundNumber);
    roundMatches.forEach((match) => {
      const currentA = cumulativePoints.get(match.teamAId) ?? 0;
      const currentB = cumulativePoints.get(match.teamBId) ?? 0;

      if (match.teamAScore > match.teamBScore) {
        cumulativePoints.set(match.teamAId, currentA + 3);
        return;
      }

      if (match.teamBScore > match.teamAScore) {
        cumulativePoints.set(match.teamBId, currentB + 3);
        return;
      }

      cumulativePoints.set(match.teamAId, currentA + 1);
      cumulativePoints.set(match.teamBId, currentB + 1);
    });

    return {
      roundNumber,
      pointsByTeam: Object.fromEntries(Array.from(cumulativePoints.entries()))
    };
  });

  const maxPoints = Math.max(
    3,
    ...roundSnapshots.flatMap((snapshot) => Object.values(snapshot.pointsByTeam))
  );

  const xForIndex = (index: number) => {
    if (roundSnapshots.length === 1) {
      return paddingLeft + innerWidth / 2;
    }
    return paddingLeft + (innerWidth * index) / (roundSnapshots.length - 1);
  };
  const yForPoints = (points: number) => paddingTop + innerHeight - (points / maxPoints) * innerHeight;

  const rounds = roundSnapshots.map((snapshot, index) => ({
    roundNumber: snapshot.roundNumber,
    label: `${snapshot.roundNumber}`,
    x: xForIndex(index)
  }));

  const palette = ['#d95d5d', '#2d9bd3', '#2f8f57', '#c08b2d', '#7b61c8', '#28727a'];
  const series = session.value.teams.map((team, teamIndex) => {
    const color = team.color || palette[teamIndex % palette.length];
    const points = roundSnapshots.map((snapshot, roundIndex) => ({
      roundNumber: snapshot.roundNumber,
      x: xForIndex(roundIndex),
      y: yForPoints(snapshot.pointsByTeam[team.id] ?? 0),
      points: snapshot.pointsByTeam[team.id] ?? 0
    }));

    return {
      teamId: team.id,
      teamName: team.name,
      color,
      path: points.map((point, index) => `${index === 0 ? 'M' : 'L'} ${point.x} ${point.y}`).join(' '),
      points
    };
  });

  const yTicks = Array.from(new Set([0, Math.ceil(maxPoints / 2), maxPoints]))
    .sort((left, right) => left - right)
    .map((value) => ({
      value,
      y: yForPoints(value)
    }));

  return {
    rounds,
    series,
    yTicks
  };
});
const roundRobinPairOptions = computed(() => {
  if (!session.value) return [];
  return buildTeamPairs(session.value.teams).map(([teamA, teamB]) => ({
    key: pairKey(teamA.id, teamB.id),
    label: `${teamA.name} - ${teamB.name}`
  }));
});

function playerInitials(player: SessionPlayer): string {
  return [player.firstName, player.lastName]
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part?.[0])
    .join('') || 'И';
}

function waitlistInitials(entry: SessionWaitlistEntry): string {
  return [entry.firstName, entry.lastName]
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part?.[0])
    .join('') || 'И';
}

function resultPlayerInitials(name: string): string {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0])
    .join('') || 'И';
}

function pointsLabel(points: number): string {
  const mod10 = points % 10;
  const mod100 = points % 100;

  if (mod10 === 1 && mod100 !== 11) {
    return 'очко';
  }
  if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) {
    return 'очка';
  }
  return 'очков';
}

function goalDifferenceLabel(goalDifference: number): string {
  return goalDifference > 0 ? `+${goalDifference}` : `${goalDifference}`;
}

function teamColorEmoji(value: string): string {
  const normalized = value.trim().toLowerCase();
  if (normalized.includes('blue') || normalized.includes('син')) return '🟦';
  if (normalized.includes('red') || normalized.includes('крас')) return '🟥';
  if (normalized.includes('green') || normalized.includes('зелен')) return '🟩';
  if (normalized.includes('yellow') || normalized.includes('желт')) return '🟨';
  if (normalized.includes('orange') || normalized.includes('оранж')) return '🟧';
  if (normalized.includes('purple') || normalized.includes('фиолет')) return '🟪';
  if (normalized.includes('black') || normalized.includes('черн')) return '⬛';
  if (normalized.includes('white') || normalized.includes('бел')) return '⬜';
  return '▪️';
}

function sessionPersonDisplayName(person: SessionPlayer | SessionWaitlistEntry): string {
  return person.displayName?.trim() || person.firstName;
}

function sessionPersonDetails(person: SessionPlayer | SessionWaitlistEntry): string {
  const fullName = [person.firstName, person.lastName].filter(Boolean).join(' ');
  const city = person.homeCity?.trim();
  return city ? `${fullName} (${city})` : fullName;
}

function playerSessionStats(playerId: number): { goals: number; assists: number } {
  return sessionPlayerStats.value[playerId] ?? { goals: 0, assists: 0 };
}

function fallbackRoundNumber(matchNumber: number): number {
  const teamCount = session.value?.teams.length ?? 0;
  const matchesPerRound = Math.max(1, (teamCount * (teamCount - 1)) / 2);
  return Math.floor((Math.max(1, matchNumber) - 1) / matchesPerRound) + 1;
}

function matchStatusClass(status: SessionMatch['status']): string {
  return `status-pill--match-${status.toLowerCase()}`;
}

function matchCardStatusClass(status: SessionMatch['status']): string {
  return `match-card--${status.toLowerCase()}`;
}

function compactPlayerPositionLabel(position: PlayerPosition | null | undefined): string {
  const labels: Record<PlayerPosition, string> = {
    GOALKEEPER: 'ВР',
    DEFENDER: 'ЗАЩ',
    MIDFIELDER: 'ПЗ',
    FORWARD: 'НАП',
    UNIVERSAL: 'УН'
  };

  return position ? labels[position] : 'Н/У';
}

function teamGroupHeaderStyle(color: string | null) {
  if (!color) {
    return {};
  }

  return {
    color,
    borderLeftColor: color,
    background: `color-mix(in srgb, ${color} 14%, transparent)`
  };
}

function ensureTeamSelection(teamId: number) {
  if (!selectedPlayersByTeam[teamId]) {
    selectedPlayersByTeam[teamId] = [];
  }
}

function isPlayerSelectedForTeam(teamId: number, playerId: number): boolean {
  ensureTeamSelection(teamId);
  return isPlayerAssignedToTeam(teamId, playerId) || selectedPlayersByTeam[teamId].includes(playerId);
}

function toggleTeamPlayer(teamId: number, playerId: number, event: Event) {
  void handleTeamPlayerToggle(teamId, playerId, event);
}

async function handleTeamPlayerToggle(teamId: number, playerId: number, event: Event) {
  if (sessionIsFinished.value) {
    (event.target as HTMLInputElement).checked = false;
    return;
  }
  ensureTeamSelection(teamId);
  const checked = (event.target as HTMLInputElement).checked;
  if (!checked && isPlayerAssignedToTeam(teamId, playerId)) {
    await api.removePlayerFromTeam(teamId, playerId);
    selectedPlayersByTeam[teamId] = selectedPlayersByTeam[teamId].filter((id) => id !== playerId);
    await loadTeamPlayers();
    return;
  }
  if (isPlayerLockedForTeam(teamId, playerId)) {
    (event.target as HTMLInputElement).checked = false;
    return;
  }
  const current = selectedPlayersByTeam[teamId];
  selectedPlayersByTeam[teamId] = checked
    ? [...new Set([...current, playerId])]
    : current.filter((id) => id !== playerId);
}

function assignedTeamForPlayer(playerId: number) {
  if (!session.value) return undefined;
  const teamId = Object.entries(teamPlayers.value).find(([, members]) => {
    return members.some((member) => member.playerId === playerId);
  })?.[0];
  return teamId ? session.value.teams.find((team) => team.id === Number(teamId)) : undefined;
}

function isPlayerAssignedToTeam(teamId: number, playerId: number): boolean {
  return Boolean(teamPlayers.value[teamId]?.some((member) => member.playerId === playerId));
}

function isPlayerLockedForTeam(teamId: number, playerId: number): boolean {
  const assignedTeam = assignedTeamForPlayer(playerId);
  return Boolean(assignedTeam && assignedTeam.id !== teamId);
}

function assignedTeamName(playerId: number): string {
  return assignedTeamForPlayer(playerId)?.name ?? '';
}

function teamChipStyle(teamId: number, playerId: number) {
  const assignedTeam = assignedTeamForPlayer(playerId);
  const color = assignedTeam?.color;
  if (!color) return {};
  return {
    borderColor: color,
    background: `color-mix(in srgb, ${color} 18%, white)`
  };
}

async function refreshAll() {
  playersViewLoading.value = true;
  try {
    await Promise.all([loadSession(), loadContributionStatuses()]);
    await Promise.all([
      loadSessionPlayers(),
      loadWaitlist(),
      loadContributionStatuses(),
      loadContributionReminders(),
      loadPlayers(),
      loadMatches(),
      loadTeamPlayers(),
      loadStandings()
    ]);
  } finally {
    playersViewLoading.value = false;
  }
}

async function loadSession() {
  session.value = await api.getSession(sessionIdNumber.value);
  fillSessionSettings();
  session.value.teams.forEach((team) => ensureTeamSelection(team.id));
  ensureRoundRobinPairSelection();
}

function fillSessionSettings() {
  if (!session.value) return;
  sessionSettings.title = session.value.title;
  sessionSettings.sessionDate = session.value.sessionDate;
  sessionSettings.sessionTime = session.value.sessionTime?.slice(0, 5) ?? '';
  sessionSettings.location = session.value.location ?? '';
  sessionSettings.locationAddress = session.value.locationAddress ?? '';
  sessionSettings.locationUrl = session.value.locationUrl ?? '';
  sessionSettings.broadcastUrl = session.value.broadcastUrl ?? '';
  sessionSettings.telegramChatId = session.value.telegramChatId ?? null;
  sessionSettings.telegramChatTitle = session.value.telegramChatTitle ?? '';
  sessionSettings.feeAmount = session.value.feeAmount ?? null;
  sessionSettings.feeRecipient = session.value.feeRecipient ?? '';
  sessionSettings.plannedMatchDurationMinutes = session.value.plannedMatchDurationMinutes ?? null;
  sessionSettings.sessionDurationMinutes = session.value.sessionDurationMinutes ?? null;
  sessionSettings.notes = session.value.notes ?? '';
  sessionSettings.maxPlayers = session.value.maxPlayers ?? null;
  sessionSettings.playerFormat = session.value.playerFormat ?? '';
}

async function loadPlayers() {
  allPlayers.value = await api.getPlayers();
}

async function loadSessionPlayers() {
  sessionPlayers.value = await api.getSessionPlayers(sessionIdNumber.value);
}

async function loadWaitlist() {
  waitlist.value = await api.getSessionWaitlist(sessionIdNumber.value);
}

async function loadContributionReminders() {
  contributionReminders.value = await api.getContributionReminders(sessionIdNumber.value);
}

async function loadContributionStatuses() {
  contributionStatuses.value = await api.getContributionStatuses(sessionIdNumber.value);
}

async function loadMatches() {
  matches.value = await api.getMatches(sessionIdNumber.value);
  const entries = await Promise.all(
    matches.value.map(async (match) => [match.id, await api.getMatchEvents(match.id)] as const)
  );
  matchEventsByMatchId.value = Object.fromEntries(entries);
  ensureRoundRobinPairSelection();
}

function matchGoalSummaries(match: SessionMatch) {
  const assistsByGoalId = new Map<number, MatchEvent>();
  const events = matchEventsByMatchId.value[match.id] ?? [];

  events
    .filter((event) => event.eventType === 'ASSIST' && event.linkedEventId)
    .forEach((event) => assistsByGoalId.set(event.linkedEventId as number, event));

  return events
    .filter((event) => (event.eventType === 'GOAL' || event.eventType === 'OWN_GOAL') && event.teamId)
    .map((goal) => {
      const assist = assistsByGoalId.get(goal.id);
      const scorer = shortMatchPlayerName(goal.playerName) ?? 'Игрок';
      const assistName = shortMatchPlayerName(assist?.playerName ?? null);
      return {
        id: goal.id,
        teamId: goal.teamId,
        playerPhotoUrl: goal.playerPhotoUrl,
        timeLabel: matchGoalTimeLabel(goal),
        label: goal.eventType === 'OWN_GOAL'
          ? `${scorer} (А)`
          : assistName ? `${scorer} (${assistName})` : scorer
      };
    });
}

function teamMatchGoals(match: SessionMatch, teamId: number) {
  return matchGoalSummaries(match).filter((goal) => goal.teamId === teamId);
}

function shortMatchPlayerName(name: string | null): string | null {
  return name?.trim().split(/\s+/)[0] || null;
}

function matchGoalTimeLabel(goal: MatchEvent): string | null {
  if (goal.minuteInMatch == null) {
    return null;
  }

  const minutes = goal.minuteInMatch.toString();
  if (goal.secondInMatch == null || goal.secondInMatch === 0) {
    return `${minutes}'`;
  }

  return `${minutes}'${goal.secondInMatch.toString().padStart(2, '0')}"`;
}

async function loadStandings() {
  const data = await api.getStandings(sessionIdNumber.value);
  standings.value = data.standings;
}

async function loadTeamPlayers() {
  if (!session.value) return;
  const entries = await Promise.all(
    session.value.teams.map(async (team) => [team.id, await api.getTeamPlayers(team.id)] as const)
  );
  teamPlayers.value = Object.fromEntries(entries);
}

async function openAdminDialog() {
  adminDialogOpen.value = true;
  if (adminUnlocked.value) {
    await loadContributionStatuses();
  }
}

function closeAdminDialog() {
  adminDialogOpen.value = false;
  adminPassword.value = '';
}

async function copyOverlayUrl() {
  try {
    await navigator.clipboard.writeText(overlayPageUrl.value);
    error.value = '';
  } catch {
    error.value = 'Не удалось скопировать ссылку';
  }
}

function closeStreamDialog() {
  if (pendingStreamStart.value) {
    return;
  }
  streamDialogOpen.value = false;
}

async function confirmStartStream() {
  if (sessionIsFinished.value || pendingStreamStart.value) {
    return;
  }

  pendingStreamStart.value = true;
  try {
    await api.startStream(sessionIdNumber.value);
    streamDialogOpen.value = false;
    error.value = '';
  } catch (streamError) {
    error.value = streamError instanceof Error ? streamError.message : 'Не удалось начать трансляцию';
  } finally {
    pendingStreamStart.value = false;
  }
}

async function applyStreamShift() {
  const seconds = Math.abs(Number(streamShiftSeconds.value));
  if (!Number.isFinite(seconds) || seconds < 1 || pendingStreamShift.value) {
    return;
  }

  pendingStreamShift.value = true;
  error.value = '';
  try {
    const stream = await getPreferredStream();
    if (!stream) {
      error.value = 'Сначала начните трансляцию';
      return;
    }

    await api.addStreamShift(sessionIdNumber.value, stream.id, streamShiftForward.value ? seconds : -seconds);
    streamShiftSeconds.value = null;
  } catch (shiftError) {
    error.value = shiftError instanceof Error ? shiftError.message : 'Не удалось применить сдвиг трансляции';
  } finally {
    pendingStreamShift.value = false;
  }
}

async function generateStreamTimeline() {
  if (pendingTimelineGeneration.value) {
    return;
  }

  pendingTimelineGeneration.value = true;
  error.value = '';
  try {
    const stream = await getPreferredStream();
    if (!stream) {
      error.value = 'Сначала начните трансляцию';
      return;
    }

    const timeline = await api.getStreamTimeline(sessionIdNumber.value, stream.id);
    generatedTimelineText.value = timeline.descriptionBlock.trim();
  } catch (timelineError) {
    error.value = timelineError instanceof Error ? timelineError.message : 'Не удалось сгенерировать тайм-коды';
  } finally {
    pendingTimelineGeneration.value = false;
  }
}

async function copyGeneratedTimeline() {
  if (!generatedTimelineText.value) {
    return;
  }

  try {
    await navigator.clipboard.writeText(generatedTimelineText.value);
    error.value = '';
  } catch {
    error.value = 'Не удалось скопировать тайм-коды';
  }
}

async function getPreferredStream() {
  const streams = await api.getStreams(sessionIdNumber.value);
  return streams.find((item) => !item.streamEndedAt) ?? streams[0] ?? null;
}

async function unlockAdminPanel() {
  if (adminPassword.value !== adminPasswordValue) {
    error.value = 'Неверный пароль администратора';
    return;
  }
  adminUnlocked.value = true;
  adminPassword.value = '';
  error.value = '';
  await loadContributionStatuses();
}

async function saveSessionSettings() {
  if (sessionIsFinished.value) {
    return false;
  }
  if (!sessionSettings.title.trim()) {
    error.value = 'Заполните название сессии';
    return false;
  }
  if (!sessionSettings.sessionDate || !sessionSettings.sessionTime) {
    error.value = 'Укажите дату и время сессии';
    return false;
  }

  pendingSessionUpdate.value = true;
  error.value = '';
  try {
    let telegramChatTitle = sessionSettings.telegramChatTitle.trim() || null;
    if (sessionSettings.telegramChatId && authState.user) {
      const chat = await api.validateTelegramChat(sessionIdNumber.value, {
        chatId: sessionSettings.telegramChatId,
        userId: authState.user.id
      });
      telegramChatTitle = chat.title;
    }
    session.value = await api.updateSession(sessionIdNumber.value, {
      title: sessionSettings.title.trim(),
      sessionDate: sessionSettings.sessionDate,
      sessionTime: sessionSettings.sessionTime,
      location: sessionSettings.location.trim() || null,
      locationAddress: sessionSettings.locationAddress.trim() || null,
      locationUrl: sessionSettings.locationUrl.trim() || null,
      broadcastUrl: sessionSettings.broadcastUrl.trim() || null,
      telegramChatId: sessionSettings.telegramChatId || null,
      telegramChatTitle,
      feeAmount: sessionSettings.feeAmount || null,
      feeRecipient: sessionSettings.feeRecipient.trim() || null,
      status: session.value?.status ?? null,
      plannedMatchDurationMinutes: sessionSettings.plannedMatchDurationMinutes || null,
      sessionDurationMinutes: sessionSettings.sessionDurationMinutes || null,
      notes: sessionSettings.notes.trim() || null,
      maxPlayers: sessionSettings.maxPlayers || null,
      playerFormat: sessionSettings.playerFormat.trim() || null
    });
    fillSessionSettings();
    await Promise.all([loadSessionPlayers(), loadWaitlist()]);
    settingsOpen.value = false;
    return true;
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Не удалось сохранить настройки сессии';
    return false;
  } finally {
    pendingSessionUpdate.value = false;
  }
}

async function startRegistration() {
  if (!authState.user) {
    error.value = 'Откройте приложение через Telegram Mini App';
    return;
  }
  if (!sessionSettings.telegramChatId) {
    error.value = 'Укажите Telegram chat ID в настройках сессии';
    return;
  }

  pendingRegistrationStart.value = true;
  error.value = '';
  try {
    const saved = await saveSessionSettings();
    if (!saved) {
      return;
    }
    const result = await api.startSessionRegistration(sessionIdNumber.value, {
      userId: authState.user.id
    });
    if (result.messageUrl) {
      window.open(result.messageUrl, '_blank', 'noreferrer');
    }
    await Promise.all([loadSession(), loadContributionStatuses()]);
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Не удалось начать регистрацию';
  } finally {
    pendingRegistrationStart.value = false;
  }
}

async function startContributionCollection() {
  if (!authState.user) {
    error.value = 'Откройте приложение через Telegram Mini App';
    return;
  }
  if (!sessionSettings.telegramChatId) {
    error.value = 'Укажите Telegram chat ID в настройках сессии';
    return;
  }

  pendingContributionStart.value = true;
  error.value = '';
  try {
    const saved = await saveSessionSettings();
    if (!saved) {
      return;
    }
    const result = await api.startContributionCollection(sessionIdNumber.value, {
      userId: authState.user.id
    });
    if (result.messageUrl) {
      window.open(result.messageUrl, '_blank', 'noreferrer');
    }
    await loadSession();
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Не удалось начать сбор взносов';
  } finally {
    pendingContributionStart.value = false;
  }
}

async function addContributionReminder(hoursBeforeValue?: number) {
  if (sessionIsFinished.value) {
    return;
  }
  const hoursBefore = Number(hoursBeforeValue ?? reminderForm.hoursBefore);
  if (!Number.isFinite(hoursBefore) || hoursBefore < 1) {
    error.value = 'Укажите, за сколько часов до игры напомнить';
    return;
  }
  if (hasContributionReminder(hoursBefore)) {
    error.value = 'Такое напоминание уже добавлено';
    return;
  }

  pendingReminderUpdate.value = true;
  error.value = '';
  try {
    await api.createContributionReminder(sessionIdNumber.value, { hoursBefore });
    reminderForm.hoursBefore = null;
    await loadContributionReminders();
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Не удалось добавить напоминание';
  } finally {
    pendingReminderUpdate.value = false;
  }
}

async function deleteContributionReminder(hoursBefore: number) {
  if (sessionIsFinished.value) {
    return;
  }

  pendingReminderUpdate.value = true;
  error.value = '';
  try {
    await api.deleteContributionReminder(sessionIdNumber.value, hoursBefore);
    await loadContributionReminders();
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Не удалось удалить напоминание';
  } finally {
    pendingReminderUpdate.value = false;
  }
}

async function toggleContributionReminder(hoursBefore: number, checked: boolean) {
  if (checked) {
    await addContributionReminder(hoursBefore);
  } else {
    await deleteContributionReminder(hoursBefore);
  }
}

async function addCustomContributionReminderOption() {
  const hoursBefore = Number(reminderForm.hoursBefore);
  if (!Number.isFinite(hoursBefore) || hoursBefore < 1) {
    error.value = 'Укажите, за сколько часов до игры напомнить';
    return;
  }
  if (!customContributionReminderHours.value.includes(hoursBefore)) {
    customContributionReminderHours.value = [...customContributionReminderHours.value, hoursBefore];
  }
  if (!hasContributionReminder(hoursBefore)) {
    await addContributionReminder(hoursBefore);
    return;
  }
  reminderForm.hoursBefore = null;
}

async function addPlayerToSession() {
  if (sessionIsFinished.value || !sessionPlayerForm.playerId) return;
  await api.addPlayerToSession(sessionIdNumber.value, {
    playerId: sessionPlayerForm.playerId,
    position: sessionPlayerForm.position
  });
  sessionPlayerForm.playerId = undefined;
  await Promise.all([loadSessionPlayers(), loadWaitlist(), loadContributionStatuses()]);
}

async function toggleCurrentUserSession() {
  if (sessionIsFinished.value) {
    return;
  }
  if (!authState.player) {
    error.value = 'Сначала заполните профиль игрока';
    return;
  }

  pendingMembership.value = true;
  error.value = '';
  try {
    if (currentUserSessionPlayer.value) {
      await api.removePlayerFromSession(sessionIdNumber.value, authState.player.playerId);
    } else if (currentUserWaitlistEntry.value) {
      await api.leaveSessionWaitlist(sessionIdNumber.value, authState.player.playerId);
    } else {
      await api.joinSession(sessionIdNumber.value, {
        playerId: authState.player.playerId,
        position: authState.player.defaultPosition
      });
    }
    await Promise.all([loadSessionPlayers(), loadWaitlist(), loadTeamPlayers(), loadContributionStatuses()]);
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Не удалось изменить участие в игре';
  } finally {
    pendingMembership.value = false;
  }
}

async function assignSelectedPlayers(teamId: number) {
  if (sessionIsFinished.value) {
    return;
  }
  const ids = (selectedPlayersByTeam[teamId] ?? []).filter((playerId) => !assignedTeamForPlayer(playerId));
  if (!ids.length) return;
  try {
    await api.bulkAssignPlayers(teamId, ids);
    selectedPlayersByTeam[teamId] = [];
    await loadTeamPlayers();
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Не удалось распределить игроков по команде';
    await loadTeamPlayers();
  }
}

async function createNextMatch() {
  if (sessionIsFinished.value) return;
  if (!session.value || session.value.teams.length < 2) return;

  const [teamA, teamB] = nextMatchTeams();
  if (!teamA || !teamB) {
    error.value = 'Выберите две команды для матча';
    return;
  }
  if (teamA.id === teamB.id) {
    error.value = 'Выберите разные команды';
    return;
  }

  await api.createMatch(sessionIdNumber.value, {
    teamAId: teamA.id,
    teamBId: teamB.id,
    plannedDurationMinutes: session.value.plannedMatchDurationMinutes ?? 6
  });
  knockoutMatchForm.teamAId = undefined;
  knockoutMatchForm.teamBId = undefined;
  await loadMatches();
}

function nextMatchTeams() {
  if (!session.value) return [undefined, undefined] as const;
  if (session.value.formatType === 'KNOCKOUT') {
    return [
      session.value.teams.find((team) => team.id === knockoutMatchForm.teamAId),
      session.value.teams.find((team) => team.id === knockoutMatchForm.teamBId)
    ] as const;
  }
  if (session.value.formatType === 'ROUND_ROBIN') {
    const pairs = orderedRoundRobinPairs();
    const pair = pairs[matches.value.length % pairs.length];
    return pair ?? [undefined, undefined] as const;
  }
  return [
    session.value.teams[matches.value.length % session.value.teams.length],
    session.value.teams[(matches.value.length + 1) % session.value.teams.length]
  ] as const;
}

function orderedRoundRobinPairs() {
  if (!session.value) return [];
  const pairs = buildTeamPairs(session.value.teams);
  const selectedIndex = pairs.findIndex(([teamA, teamB]) => pairKey(teamA.id, teamB.id) === roundRobinFirstPairKey.value);
  if (selectedIndex < 1) return pairs;
  return [...pairs.slice(selectedIndex), ...pairs.slice(0, selectedIndex)];
}

function buildTeamPairs(teams: GameSession['teams']) {
  const pairs: Array<[GameSession['teams'][number], GameSession['teams'][number]]> = [];
  teams.forEach((teamA, index) => {
    teams.slice(index + 1).forEach((teamB) => pairs.push([teamA, teamB]));
  });
  return pairs;
}

function pairKey(teamAId: number, teamBId: number): string {
  return [teamAId, teamBId].sort((left, right) => left - right).join(':');
}

function ensureRoundRobinPairSelection() {
  if (!session.value || session.value.formatType !== 'ROUND_ROBIN') return;
  const firstMatch = matches.value[0];
  if (firstMatch) {
    roundRobinFirstPairKey.value = pairKey(firstMatch.teamAId, firstMatch.teamBId);
    return;
  }
  const options = roundRobinPairOptions.value;
  if (!options.some((option) => option.key === roundRobinFirstPairKey.value)) {
    roundRobinFirstPairKey.value = options[0]?.key ?? '';
  }
}

async function startMatch(matchId: number) {
  if (sessionIsFinished.value) return;
  await api.startMatch(sessionIdNumber.value, matchId);
  await loadMatches();
}

async function finishMatch(matchId: number) {
  if (sessionIsFinished.value) return;
  await api.finishMatch(sessionIdNumber.value, matchId);
  await Promise.all([loadMatches(), loadStandings()]);
}

async function resumeMatch(matchId: number) {
  if (sessionIsFinished.value) return;
  await api.resumeMatch(sessionIdNumber.value, matchId);
  await Promise.all([loadMatches(), loadStandings()]);
}

async function finishSession() {
  if (!session.value || sessionIsFinished.value) {
    return;
  }

  pendingSessionUpdate.value = true;
  error.value = '';
  try {
    session.value = await api.updateSession(sessionIdNumber.value, {
      title: session.value.title,
      sessionDate: session.value.sessionDate,
      sessionTime: session.value.sessionTime,
      location: session.value.location,
      locationAddress: session.value.locationAddress,
      locationUrl: session.value.locationUrl,
      broadcastUrl: session.value.broadcastUrl,
      telegramChatId: session.value.telegramChatId,
      telegramChatTitle: session.value.telegramChatTitle,
      feeAmount: session.value.feeAmount,
      feeRecipient: session.value.feeRecipient,
      status: 'FINISHED',
      plannedMatchDurationMinutes: session.value.plannedMatchDurationMinutes,
      sessionDurationMinutes: session.value.sessionDurationMinutes,
      notes: session.value.notes,
      maxPlayers: session.value.maxPlayers,
      playerFormat: session.value.playerFormat
    });
    fillSessionSettings();
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Не удалось завершить сессию';
  } finally {
    pendingSessionUpdate.value = false;
  }
}

function openResumeSessionDialog() {
  if (!session.value || !sessionIsFinished.value) {
    return;
  }

  resumePassword.value = '';
  resumePasswordError.value = '';
  error.value = '';
  resumePasswordDialogOpen.value = true;
}

function closeResumeSessionDialog() {
  if (pendingSessionUpdate.value) {
    return;
  }

  resumePasswordDialogOpen.value = false;
  resumePassword.value = '';
  resumePasswordError.value = '';
}

async function resumeSession() {
  if (!session.value || !sessionIsFinished.value) {
    return;
  }

  if (resumePassword.value !== resumeSessionPassword) {
    resumePasswordError.value = 'Неверный пароль';
    error.value = 'Неверный пароль. Статус сессии не изменен';
    return;
  }

  pendingSessionUpdate.value = true;
  resumePasswordError.value = '';
  error.value = '';
  try {
    session.value = await api.updateSession(sessionIdNumber.value, {
      title: session.value.title,
      sessionDate: session.value.sessionDate,
      sessionTime: session.value.sessionTime,
      location: session.value.location,
      locationAddress: session.value.locationAddress,
      locationUrl: session.value.locationUrl,
      broadcastUrl: session.value.broadcastUrl,
      telegramChatId: session.value.telegramChatId,
      telegramChatTitle: session.value.telegramChatTitle,
      feeAmount: session.value.feeAmount,
      feeRecipient: session.value.feeRecipient,
      status: 'IN_PROGRESS',
      plannedMatchDurationMinutes: session.value.plannedMatchDurationMinutes,
      sessionDurationMinutes: session.value.sessionDurationMinutes,
      notes: session.value.notes,
      maxPlayers: session.value.maxPlayers,
      playerFormat: session.value.playerFormat
    });
    fillSessionSettings();
    resumePasswordDialogOpen.value = false;
    resumePassword.value = '';
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Не удалось возобновить сессию';
  } finally {
    pendingSessionUpdate.value = false;
  }
}

async function openMatch(matchId: number) {
  await router.push(`/sessions/${sessionIdNumber.value}/matches/${matchId}`);
}

async function openPlayerProfile(playerId: number) {
  await router.push(`/players/${playerId}`);
}

watch(
  () => activeTab.value,
  async (tab) => {
    try {
      if (tab === 'Teams') {
        await loadTeamPlayers();
      }
      if (tab === 'Standings') {
        await loadStandings();
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Не удалось загрузить вкладку';
    }
  }
);

watch(sessionVenuePhotoUrl, () => {
  sessionVenuePhotoFailed.value = false;
});

onMounted(async () => {
  try {
    const startParam = getStartParam();
    if (startParam?.startsWith('session_')) {
      activeTab.value = 'Players';
    }
    await refreshAll();
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Не удалось загрузить сессию';
  }
});
</script>
