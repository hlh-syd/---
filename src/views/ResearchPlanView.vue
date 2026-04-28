<template>
  <div class="page-grid">
    <div class="page-grid grid-2">
      <SectionCard eyebrow="Pomodoro" title="番茄钟" subtitle="参考任务页的控制密度与状态反馈。">
        <div class="timer-card">
          <div class="timer-card__time">{{ formatTime(pomodoroSeconds) }}</div>
          <el-progress :percentage="Math.round((pomodoroSeconds / (25 * 60)) * 100)" :show-text="false" :stroke-width="12" />
          <div class="timer-card__actions">
            <GlassButton variant="primary" @click="togglePomodoro">{{ pomodoroRunning ? '暂停' : '开始' }}</GlassButton>
            <GlassButton variant="ghost" @click="resetPomodoro">重置</GlassButton>
          </div>
        </div>
      </SectionCard>

      <SectionCard eyebrow="Timer" title="定时 / 计时" subtitle="和番茄钟拆开，保留独立计时能力。">
        <div class="timer-card">
          <div class="timer-card__time">{{ formatTime(stopwatchSeconds) }}</div>
          <el-progress :percentage="Math.min(100, Math.round((stopwatchSeconds / 5400) * 100))" :show-text="false" :stroke-width="12" color="#19b7a0" />
          <div class="timer-card__actions">
            <GlassButton variant="primary" @click="toggleStopwatch">{{ stopwatchRunning ? '暂停' : '开始' }}</GlassButton>
            <GlassButton variant="ghost" @click="resetStopwatch">清零</GlassButton>
          </div>
        </div>
      </SectionCard>
    </div>

    <SectionCard eyebrow="Gantt Task" title="工作计划 list" subtitle="使用任务稿的横向进度带与阶段划分。">
      <div class="task-list">
        <article v-for="task in store.planTasks" :key="task.id" class="task-list__item">
          <div>
            <strong>{{ task.title }}</strong>
            <p>{{ task.owner }} · {{ task.lane }} · 截止 {{ task.due }}</p>
          </div>
          <div class="task-list__bar">
            <div class="task-list__bar-fill" :style="{ width: `${task.progress}%` }"></div>
          </div>
          <span>{{ task.progress }}%</span>
        </article>
      </div>
    </SectionCard>

    <div class="page-grid grid-2">
      <SectionCard eyebrow="Calendar" title="日历" subtitle="日历页参考稿的规则网格、事件胶囊和轻边框。">
        <div class="calendar-grid">
          <div v-for="label in weekdays" :key="label" class="calendar-grid__weekday">{{ label }}</div>
          <div v-for="day in days" :key="day" class="calendar-grid__day">
            <strong>{{ day }}</strong>
            <span v-if="eventMap.get(day)" class="calendar-grid__event">{{ eventMap.get(day)?.title }}</span>
          </div>
        </div>
      </SectionCard>

      <SectionCard eyebrow="Punch" title="打卡" subtitle="将本周专注状态做成小型出勤板。">
        <div class="punch-grid">
          <article v-for="item in store.punchDays" :key="item.day" class="punch-grid__item" :class="{ 'punch-grid__item--done': item.done }">
            <strong>{{ item.day }}</strong>
            <span>{{ item.done ? '已打卡' : '未打卡' }}</span>
          </article>
        </div>
      </SectionCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue';

import GlassButton from '@/components/ui/GlassButton.vue';
import SectionCard from '@/components/ui/SectionCard.vue';
import { useWorkspaceStore } from '@/stores/workspace';

const store = useWorkspaceStore();

const pomodoroSeconds = ref(25 * 60);
const stopwatchSeconds = ref(0);
const pomodoroRunning = ref(false);
const stopwatchRunning = ref(false);

const weekdays = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
const days = Array.from({ length: 30 }, (_, index) => index + 1);

const eventMap = computed(() => new Map(store.calendarEvents.map((event) => [event.day, event])));

let pomodoroTimer: number | undefined;
let stopwatchTimer: number | undefined;

const stopPomodoroTimer = () => {
  if (pomodoroTimer !== undefined) {
    window.clearInterval(pomodoroTimer);
    pomodoroTimer = undefined;
  }
};

const stopStopwatchTimer = () => {
  if (stopwatchTimer !== undefined) {
    window.clearInterval(stopwatchTimer);
    stopwatchTimer = undefined;
  }
};

const togglePomodoro = () => {
  pomodoroRunning.value = !pomodoroRunning.value;
  if (!pomodoroRunning.value) {
    stopPomodoroTimer();
    return;
  }

  pomodoroTimer = window.setInterval(() => {
    pomodoroSeconds.value = Math.max(0, pomodoroSeconds.value - 1);
    if (pomodoroSeconds.value === 0) {
      pomodoroRunning.value = false;
      stopPomodoroTimer();
    }
  }, 1000);
};

const toggleStopwatch = () => {
  stopwatchRunning.value = !stopwatchRunning.value;
  if (!stopwatchRunning.value) {
    stopStopwatchTimer();
    return;
  }

  stopwatchTimer = window.setInterval(() => {
    stopwatchSeconds.value += 1;
  }, 1000);
};

const resetPomodoro = () => {
  pomodoroRunning.value = false;
  stopPomodoroTimer();
  pomodoroSeconds.value = 25 * 60;
};

const resetStopwatch = () => {
  stopwatchRunning.value = false;
  stopStopwatchTimer();
  stopwatchSeconds.value = 0;
};

const formatTime = (value: number) => {
  const minutes = Math.floor(value / 60)
    .toString()
    .padStart(2, '0');
  const seconds = (value % 60).toString().padStart(2, '0');
  return `${minutes}:${seconds}`;
};

onUnmounted(() => {
  stopPomodoroTimer();
  stopStopwatchTimer();
});
</script>

<style scoped>
.timer-card {
  display: grid;
  gap: 20px;
}

.timer-card__time {
  font-family: 'Manrope', 'Inter', sans-serif;
  font-size: 64px;
  font-weight: 800;
  letter-spacing: -0.08em;
}

.timer-card__actions {
  display: flex;
  gap: 12px;
}

.task-list {
  display: grid;
  gap: 14px;
}

.task-list__item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(220px, 0.8fr) 48px;
  gap: 18px;
  align-items: center;
  padding: 18px;
  border-radius: 24px;
  border: 1px solid rgba(203, 212, 228, 0.72);
  background: rgba(248, 250, 255, 0.84);
}

.task-list__item p {
  margin: 8px 0 0;
  color: var(--text-faint);
}

.task-list__bar {
  height: 14px;
  border-radius: 999px;
  background: rgba(201, 210, 225, 0.46);
  overflow: hidden;
}

.task-list__bar-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, rgba(49, 109, 255, 0.94), rgba(25, 183, 160, 0.9));
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 10px;
}

.calendar-grid__weekday {
  color: var(--text-faint);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.calendar-grid__day {
  min-height: 96px;
  padding: 14px;
  border-radius: 22px;
  border: 1px solid rgba(205, 213, 228, 0.72);
  background: rgba(248, 250, 255, 0.84);
}

.calendar-grid__event {
  display: block;
  margin-top: 12px;
  padding: 8px 10px;
  border-radius: 14px;
  background: rgba(66, 123, 255, 0.12);
  color: var(--brand);
  font-size: 12px;
  font-weight: 700;
}

.punch-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 12px;
}

.punch-grid__item {
  display: grid;
  gap: 10px;
  padding: 16px;
  border-radius: 22px;
  border: 1px solid rgba(205, 213, 228, 0.72);
  background: rgba(248, 250, 255, 0.84);
  text-align: center;
}

.punch-grid__item--done {
  background: linear-gradient(180deg, rgba(25, 183, 160, 0.14), rgba(255, 255, 255, 0.88));
  border-color: rgba(79, 194, 176, 0.36);
}
</style>
