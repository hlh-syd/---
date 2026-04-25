const state = {
  activeModule: "overview",
  data: null,
  assistantMessages: [],
  knowledge: {
    bases: [],
    activeBaseId: null,
    folders: [],
    documents: [],
    selectedDocumentIds: [],
    sessions: [],
    activeSessionId: null,
    messages: [],
    citations: [],
  },
  research: {
    history: [],
    kbOptions: [],
    summary: "",
    mindmap: "",
  },
  web: {
    history: [],
    platform: "全部",
  },
  ai: {
    tools: [],
    promptResult: "",
    bioResult: "",
  },
  plan: {
    tasks: [],
    calendar: [],
    pomodoro: [],
    checkins: [],
    timerSeconds: 0,
    timerRunning: false,
    timerHandle: null,
    floatingCards: [],
    floatingToastHandle: null,
    pomodoroSession: {
      focusMinutes: 25,
      phase: "idle",
      remainingSeconds: 25 * 60,
      running: false,
      handle: null,
      cycleStartedAt: null,
      completedCycles: 0,
    },
  },
  usage: {
    todayMinutes: 0,
    heartbeatHandle: null,
    visibleSince: null,
    visibilityBound: false,
  },
  relax: {
    bazi: "",
    eat: "",
  },
  profile: {
    detail: null,
    bindings: [],
  },
  modal: {
    open: false,
    title: "",
    subtitle: "",
    items: [],
    action: null,
  },
};

const FLOATING_CARD_LIMIT = 4;
const FLOATING_CARD_META = {
  timer: { label: "计时", kicker: "Timer" },
  pomodoro: { label: "番茄钟", kicker: "Pomodoro" },
  task: { label: "任务", kicker: "Task" },
  calendar: { label: "日历", kicker: "Calendar" },
};

const POMODORO_TICK_MS = 1000;
const USAGE_INCREMENT_MINUTES = 30;
const USAGE_HEARTBEAT_MS = 60 * 1000;

const moduleStage = document.getElementById("module-stage");
const topNav = document.getElementById("top-nav");
const moduleNav = document.getElementById("module-nav");
const toolbarSwitches = document.getElementById("toolbar-switches");
const heroPanel = document.getElementById("hero-panel");
const heroTitle = document.getElementById("hero-title");
const heroSummary = document.getElementById("hero-summary");
const heroChips = document.getElementById("hero-chips");
const heroMetrics = document.getElementById("hero-metrics");
const spotlightPanel = document.getElementById("spotlight-panel");
const activityList = document.getElementById("activity-list");
const sourceList = document.getElementById("source-list");
const sidebarUser = document.getElementById("sidebar-user");
const sidebarRole = document.getElementById("sidebar-role");
const sidebarAvatarRing = document.getElementById("sidebar-avatar-ring");
const sidebarAvatarImage = document.getElementById("sidebar-avatar-image");
const sidebarAvatarFallback = document.getElementById("sidebar-avatar-fallback");
const sidebarTags = document.getElementById("sidebar-tags");
const brandName = document.getElementById("brand-name");
const brandTagline = document.getElementById("brand-tagline");
const logoutButton = document.getElementById("logout-button");
const topbar = document.querySelector(".topbar");
const topbarSentinel = document.getElementById("topbar-sentinel");
const topbarStatusValue = document.getElementById("topbar-status-value");
const topbarUsageValue = document.getElementById("topbar-usage-value");
const topbarContextCopy = document.getElementById("topbar-context-copy");
const heroDate = document.getElementById("hero-date");
const pickerModal = document.getElementById("picker-modal");
const pickerTitle = document.getElementById("picker-title");
const pickerSubtitle = document.getElementById("picker-subtitle");
const pickerList = document.getElementById("picker-list");
const pickerClose = document.getElementById("picker-close");
const statusBanner = document.getElementById("status-banner");
const mindmapModal = document.getElementById("mindmap-modal");
const mindmapClose = document.getElementById("mindmap-close");
const mindmapModalContent = document.getElementById("mindmap-modal-content");
const ambientOrbs = Array.from(document.querySelectorAll(".ambient-orb"));

async function boot() {
  const payload = await fetchJson("/api/workbench/bootstrap");
  state.data = payload.data;
  state.assistantMessages = [...state.data.assistant.messages];
  applyShell();
  bindGlobalUi();
  await handleOAuthReturn();
  renderModule();
}

function applyShell() {
  const { brand, modules, dashboard, profile, knowledge } = state.data;
  state.usage.todayMinutes = Number(profile.todayUsageMinutes || 0);

  if (brandName) {
    brandName.textContent = brand.name;
  }
  if (brandTagline) {
    brandTagline.textContent = brand.tagline;
  }
  if (heroTitle) {
    heroTitle.textContent = dashboard.headline.title;
  }
  if (heroSummary) {
    heroSummary.textContent = dashboard.headline.summary;
  }
  if (heroDate) {
    heroDate.textContent = formatDashboardDate();
  }
  if (sidebarUser) {
    sidebarUser.textContent = profile.user.name;
  }
  if (sidebarRole) {
    sidebarRole.textContent = profile.user.role;
  }
  renderSidebarAvatar(profile.user.avatarUrl, profile.user.name);

  if (heroChips) {
    heroChips.innerHTML = dashboard.headline.chips
      .map((chip) => `<span class="chip">${chip}</span>`)
      .join("");
  }
  if (sidebarTags) {
    sidebarTags.innerHTML = buildSidebarTags(profile)
      .map((item) => `<span class="chip">${escapeHtml(item)}</span>`)
      .join("");
  }

  if (heroMetrics) {
    heroMetrics.innerHTML = dashboard.metrics.map(metricCard).join("");
  }

  const topNavMarkup = modules
    .map((module, index) => moduleButton(module, "global", index))
    .join("");
  const navMarkup = modules.map((module, index) => moduleButton(module, "sidebar", index)).join("");
  if (topNav) {
    topNav.innerHTML = topNavMarkup;
  }
  if (moduleNav) {
    moduleNav.innerHTML = navMarkup;
  }
  if (toolbarSwitches) {
    toolbarSwitches.innerHTML = topNavMarkup;
  }

  if (spotlightPanel) {
    spotlightPanel.innerHTML = state.data.research.insights.map((item) => infoCard(item.title, item.text)).join("");
  }
  if (activityList) {
    activityList.innerHTML = dashboard.activity
      .map(
        (item) => `
          <div class="timeline-item">
            <strong>${item.time}</strong>
            <span>${item.text}</span>
          </div>
        `
      )
      .join("");
  }
  if (sourceList) {
    sourceList.innerHTML = knowledge.references.map((item) => `<div class="source-pill">${item}</div>`).join("");
  }

  bindModuleButtons();
  syncTopbarStatus();
  syncTopbarUsage();

  document.querySelectorAll("[data-quick-jump]").forEach((button) => {
    button.addEventListener("click", () => {
      state.activeModule = button.dataset.quickJump;
      syncButtons();
      renderModule();
    });
  });

}

function bindGlobalUi() {
  bindTopbarCollapse();
  bindFloatingTimer();
  bindUsageHeartbeat();
  logoutButton?.addEventListener("click", performLogout);
  document.getElementById("sidebar-logout-button")?.addEventListener("click", performLogout);
  pickerClose?.addEventListener("click", closeKbPicker);
  pickerModal?.addEventListener("click", (event) => {
    if (event.target === pickerModal) {
      closeKbPicker();
    }
  });
  mindmapClose?.addEventListener("click", closeMindmapPreview);
  mindmapModal?.addEventListener("click", (event) => {
    if (event.target === mindmapModal) {
      closeMindmapPreview();
    }
  });
  bindAmbientOrbs();
}

async function performLogout() {
  try {
    await fetchJson("/api/auth/logout", { method: "POST", skipAuthRedirect: true });
  } finally {
    window.location.href = `/login.html?next=${encodeURIComponent(window.location.pathname)}`;
  }
}

function bindTopbarCollapse() {
  if (!topbar) {
    return;
  }
  const autoCollapseMedia = window.matchMedia("(hover: hover) and (pointer: fine) and (min-width: 1261px)");
  let blurTimer = null;
  let collapseTimer = null;

  const applyCollapsed = (collapsed) => {
    topbar.classList.toggle("is-collapsed", collapsed);
  };

  const clearTimers = () => {
    if (blurTimer) {
      window.clearTimeout(blurTimer);
      blurTimer = null;
    }
    if (collapseTimer) {
      window.clearTimeout(collapseTimer);
      collapseTimer = null;
    }
  };

  const isAutoCollapseEnabled = () => autoCollapseMedia.matches;

  const syncCollapseMode = () => {
    const shouldCollapse = isAutoCollapseEnabled() && !topbar.matches(":hover") && !topbar.contains(document.activeElement);
    topbar.classList.toggle("is-auto-collapse", isAutoCollapseEnabled());
    clearTimers();
    applyCollapsed(shouldCollapse);
  };

  const expandTopbar = () => {
    if (!isAutoCollapseEnabled()) {
      return;
    }
    clearTimers();
    applyCollapsed(false);
  };

  const collapseTopbar = (delay = 260) => {
    if (!isAutoCollapseEnabled()) {
      return;
    }
    clearTimers();
    collapseTimer = window.setTimeout(() => {
      if (!topbar.matches(":hover") && !topbar.contains(document.activeElement)) {
        applyCollapsed(true);
      }
    }, delay);
  };

  syncCollapseMode();

  topbar.addEventListener("mouseenter", () => {
    expandTopbar();
  });
  topbar.addEventListener("mouseleave", () => {
    collapseTopbar(220);
  });

  topbar.addEventListener("click", (e) => {
    const btn = e.target.closest("#top-nav button");
    if (btn) {
      clearTimers();
      applyCollapsed(true);
    }
  });
  topbar.addEventListener("focusin", () => {
    expandTopbar();
  });
  topbar.addEventListener("focusout", () => {
    if (!isAutoCollapseEnabled()) {
      return;
    }
    clearTimers();
    blurTimer = window.setTimeout(() => {
      if (!topbar.contains(document.activeElement)) {
        collapseTopbar(160);
      }
    }, 30);
  });

  autoCollapseMedia.addEventListener?.("change", syncCollapseMode);
  window.addEventListener("resize", syncCollapseMode);

  if (topbarSentinel && "IntersectionObserver" in window) {
    const observer = new IntersectionObserver(
      ([entry]) => {
        topbar.classList.toggle("is-scrolled", !entry.isIntersecting);
      },
      { threshold: 0, rootMargin: "-18px 0px 0px 0px" }
    );
    observer.observe(topbarSentinel);
  }
}

function bindAmbientOrbs() {
  if (!ambientOrbs.length) {
    return;
  }
  const zoneBounds = {
    "left-top": { minX: -22, maxX: 28, minY: -18, maxY: 22 },
    "right-top": { minX: -28, maxX: 22, minY: -18, maxY: 24 },
    "left-bottom": { minX: -20, maxX: 24, minY: -26, maxY: 14 },
    "right-bottom": { minX: -24, maxX: 20, minY: -24, maxY: 16 },
    "center-top": { minX: -22, maxX: 22, minY: -16, maxY: 22 },
    "center-bottom": { minX: -22, maxX: 22, minY: -22, maxY: 18 },
  };
  const orbStates = ambientOrbs.map((orb) => ({
    orb,
    currentX: 0,
    currentY: 0,
    targetX: 0,
    targetY: 0,
  }));
  let pointer = { x: window.innerWidth / 2, y: window.innerHeight / 2, active: false };
  let frameId = null;
  const updateTargets = () => {
    const { x: pointerX, y: pointerY, active } = pointer;
    orbStates.forEach((stateItem) => {
      const { orb } = stateItem;
      if (!active) {
        stateItem.targetX = 0;
        stateItem.targetY = 0;
        orb.classList.remove("is-hovered");
        return;
      }
    const rect = orb.getBoundingClientRect();
    const centerX = rect.left + rect.width / 2;
    const centerY = rect.top + rect.height / 2;
    const deltaX = pointerX - centerX;
    const deltaY = pointerY - centerY;
    const distance = Math.hypot(deltaX, deltaY);
    const interactRadius = 280;
    const influence = Math.max(0, 1 - distance / interactRadius);
    const isHovered = influence > 0.16;
    orb.classList.toggle("is-hovered", isHovered);
    const zone = zoneBounds[orb.dataset.orbZone] || zoneBounds["left-top"];
    const followRatio = 0.018 + influence * 0.11;
    const targetX = Math.max(zone.minX, Math.min(zone.maxX, deltaX * followRatio));
    const targetY = Math.max(zone.minY, Math.min(zone.maxY, deltaY * followRatio));
    stateItem.targetX = targetX;
    stateItem.targetY = targetY;
    });
  };
  const animate = () => {
    updateTargets();
    let hasMotion = false;
    orbStates.forEach((stateItem) => {
      stateItem.currentX += (stateItem.targetX - stateItem.currentX) * 0.22;
      stateItem.currentY += (stateItem.targetY - stateItem.currentY) * 0.22;
      if (Math.abs(stateItem.currentX - stateItem.targetX) > 0.08 || Math.abs(stateItem.currentY - stateItem.targetY) > 0.08) {
        hasMotion = true;
      }
      stateItem.orb.style.setProperty("--orb-x", `${stateItem.currentX.toFixed(2)}px`);
      stateItem.orb.style.setProperty("--orb-y", `${stateItem.currentY.toFixed(2)}px`);
    });
    frameId = hasMotion || pointer.active ? window.requestAnimationFrame(animate) : null;
  };
  const ensureAnimation = () => {
    if (frameId !== null) {
      return;
    }
    frameId = window.requestAnimationFrame(animate);
  };
  window.addEventListener("pointermove", (event) => {
    pointer = { x: event.clientX, y: event.clientY, active: true };
    ensureAnimation();
  });
  window.addEventListener("pointerleave", () => {
    pointer = { x: window.innerWidth / 2, y: window.innerHeight / 2, active: false };
    ensureAnimation();
  });
  window.addEventListener("pointerdown", (event) => {
    pointer = { x: event.clientX, y: event.clientY, active: true };
    ensureAnimation();
  });
  window.addEventListener("resize", () => {
    pointer = { x: window.innerWidth / 2, y: window.innerHeight / 2, active: pointer.active };
    window.requestAnimationFrame(() => {
      orbStates.forEach((stateItem) => {
        stateItem.currentX = 0;
        stateItem.currentY = 0;
        stateItem.targetX = 0;
        stateItem.targetY = 0;
      });
      ensureAnimation();
    });
  });
  ensureAnimation();
}

async function handleOAuthReturn() {
  const params = new URLSearchParams(window.location.search);
  const oauth = params.get("oauth");
  if (!oauth) {
    return;
  }
  const platform = params.get("platform") || "oauth";
  const message = params.get("message") || `${platform} 状态已更新`;
  if (oauth === "success") {
    try {
      const payload = await fetchJson("/api/profile/bindings");
      state.data.profile.bindings = payload.data.map((item) => `${item.platform}: ${item.openId}`);
      showStatusBanner(message);
    } catch (error) {
      showStatusBanner(message);
    }
  } else {
    showStatusBanner(message);
  }
  history.replaceState({}, document.title, window.location.pathname);
}

function showStatusBanner(message) {
  if (!statusBanner) return;
  statusBanner.textContent = message;
  statusBanner.classList.remove("hidden");
  window.setTimeout(() => statusBanner.classList.add("hidden"), 5000);
}

function moduleButton(module, scope, index = 0) {
  const isSidebar = scope === "sidebar";
  const buttonContent = isSidebar
    ? `
      <span class="module-button-copy">
        <strong>${module.label}</strong>
        <small>${module.description}</small>
      </span>
    `
    : `<span class="nav-label">${module.label}</span>`;
  return `
    <button
      class="${module.id === state.activeModule ? "is-active" : ""}"
      data-module="${module.id}"
      data-scope="${scope}"
      title="${module.description}"
      aria-pressed="${module.id === state.activeModule ? "true" : "false"}"
      style="--nav-index: ${index};"
    >
      ${buttonContent}
    </button>
  `;
}

function bindModuleButtons() {
  document.querySelectorAll("[data-module]").forEach((button) => {
    button.addEventListener("click", () => {
      state.activeModule = button.dataset.module;
      syncButtons();
      renderModule();
    });
  });
}

function syncButtons() {
  document.querySelectorAll("[data-module]").forEach((button) => {
    const isActive = button.dataset.module === state.activeModule;
    button.classList.toggle("is-active", isActive);
    button.setAttribute("aria-pressed", isActive ? "true" : "false");
  });
  const sidebarLogout = document.getElementById("sidebar-logout-button");
  if (sidebarLogout) {
    sidebarLogout.classList.toggle("hidden", state.activeModule !== "profile");
  }
  syncTopbarStatus();
}

function getActiveModuleMeta() {
  if (!state.data?.modules?.length) {
    return null;
  }
  return state.data.modules.find((module) => module.id === state.activeModule) || state.data.modules[0];
}

function buildSidebarTags(profile) {
  const profileTags = Array.isArray(profile.tags) ? profile.tags.filter(Boolean).slice(0, 4) : [];
  if (profileTags.length) {
    return profileTags;
  }
  return [
    profile.user?.institution,
    profile.detail?.degreeLevel,
    profile.detail?.researchDirection,
  ].filter(Boolean).slice(0, 4);
}

function formatDashboardDate() {
  return new Intl.DateTimeFormat("zh-CN", {
    month: "long",
    day: "numeric",
    weekday: "short",
  }).format(new Date());
}

function syncTopbarStatus() {
  const activeModule = getActiveModuleMeta();
  if (!topbarStatusValue || !activeModule) {
    return;
  }
  topbarStatusValue.textContent = activeModule.label;
  if (topbarContextCopy) {
    topbarContextCopy.textContent = activeModule.description || state.data?.brand?.tagline || "";
  }
}

function formatUsageMinutes(totalMinutes) {
  const hours = String(Math.floor(totalMinutes / 60)).padStart(2, "0");
  const minutes = String(totalMinutes % 60).padStart(2, "0");
  return `${hours}:${minutes}`;
}

function syncTopbarUsage() {
  if (!topbarUsageValue) {
    return;
  }
  topbarUsageValue.textContent = formatUsageMinutes(state.usage.todayMinutes);
}

function bindUsageHeartbeat() {
  if (state.usage.visibilityBound) {
    return;
  }
  state.usage.visibilityBound = true;
  state.usage.visibleSince = document.visibilityState === "visible" ? Date.now() : null;

  const syncVisibility = () => {
    state.usage.visibleSince = document.visibilityState === "visible" ? Date.now() : null;
  };

  const maybePingUsage = async () => {
    if (document.visibilityState !== "visible") {
      return;
    }
    if (state.usage.visibleSince == null) {
      state.usage.visibleSince = Date.now();
      return;
    }
    if (Date.now() - state.usage.visibleSince < USAGE_INCREMENT_MINUTES * 60 * 1000) {
      return;
    }
    const payload = await fetchJson("/api/plans/usage/ping", { method: "POST" });
    state.usage.todayMinutes = Number(payload?.data?.todayTime || state.usage.todayMinutes);
    syncTopbarUsage();
    state.usage.visibleSince = Date.now();
  };

  document.addEventListener("visibilitychange", syncVisibility);
  state.usage.heartbeatHandle = window.setInterval(() => {
    void maybePingUsage();
  }, USAGE_HEARTBEAT_MS);
}

function renderModule() {
  if (!state.data) {
    return;
  }

  const modules = {
    overview: renderOverview,
    research: renderResearch,
    knowledge: renderKnowledge,
    web: renderWeb,
    assistant: renderAssistant,
    plan: renderPlan,
    relax: renderRelax,
    profile: renderProfile,
  };

  moduleStage.innerHTML = "";
  const renderer = modules[state.activeModule] || renderOverview;
  moduleStage.appendChild(renderer());

  if (heroPanel) {
    heroPanel.classList.toggle("hidden", state.activeModule !== "overview");
  }
  const rightRail = document.querySelector(".right-rail");
  if (rightRail) {
    rightRail.classList.toggle("hidden", state.activeModule !== "overview");
  }
  const workspace = document.querySelector(".workspace");
  if (workspace) {
    workspace.classList.toggle("is-main-extended", state.activeModule !== "overview");
  }
  document.body.dataset.activeModule = state.activeModule;
}

function renderOverviewLegacy() {
  const section = createModuleSection("首页工作台", "今天该做什么、资料在哪里、最近研究推进到哪一步。");
  const { dashboard } = state.data;

  section.innerHTML += `
    <div class="card-grid">
      <div class="content-card card-span-5">
        <div class="section-header"><h3>最近搜索</h3><span>Search</span></div>
        <div class="list-stack">
          ${dashboard.recentSearches.map(listItem).join("")}
        </div>
      </div>
      <div class="content-card card-span-7">
        <div class="section-header"><h3>项目空间</h3><span>Workspace</span></div>
        <div class="list-stack">
          ${dashboard.projects
            .map(
              (item) => `
                <div class="progress-row">
                  <div>
                    <p class="title-line">${item.title}</p>
                    <p class="meta-line">${item.field} · ${item.stage}</p>
                  </div>
                  <strong>${item.progress}</strong>
                </div>
              `
            )
            .join("")}
        </div>
      </div>
      <div class="content-card card-span-6">
        <div class="section-header"><h3>今日任务</h3><span>Tasks</span></div>
        <div class="list-stack">
          ${dashboard.focusTasks
            .map(
              (item) => `
                <div class="list-item">
                  <p class="title-line">${item.title}</p>
                  <p class="meta-line">${item.priority} · ${item.deadline}</p>
                </div>
              `
            )
            .join("")}
        </div>
      </div>
      <div class="content-card card-span-6">
        <div class="section-header"><h3>研究活动</h3><span>Timeline</span></div>
        <div class="timeline-list">
          ${dashboard.activity
            .map(
              (item) => `
                <div class="timeline-item">
                  <strong>${item.time}</strong>
                  <span>${item.text}</span>
                </div>
              `
            )
            .join("")}
        </div>
      </div>
    </div>
  `;

  return section;
}

function renderOverview() {
  const section = createModuleSection("首页工作台", "今天该做什么、资料在哪里、最近研究推进到哪一步。");
  const { dashboard } = state.data;

  section.innerHTML += `
    <div class="card-grid overview-board">
      <div class="content-card card-span-7 stage-card">
        <div class="section-header"><h3>项目空间</h3><span>Workspace</span></div>
        <div class="list-stack">
          ${dashboard.projects
            .map(
              (item) => `
                <div class="progress-row project-progress">
                  <div class="progress-copy">
                    <p class="title-line">${item.title}</p>
                    <p class="meta-line">${item.field} · ${item.stage}</p>
                  </div>
                  <strong>${item.progress}</strong>
                </div>
              `
            )
            .join("")}
        </div>
      </div>
      <div class="content-card card-span-5 task-card">
        <div class="section-header"><h3>今日任务</h3><span>Tasks</span></div>
        <div class="list-stack">
          ${dashboard.focusTasks
            .map(
              (item) => `
                <div class="list-item task-snapshot">
                  <p class="title-line">${item.title}</p>
                  <p class="meta-line">${item.priority} · ${item.deadline}</p>
                </div>
              `
            )
            .join("")}
        </div>
      </div>
      <div class="content-card card-span-4 search-log-card">
        <div class="section-header"><h3>最近搜索</h3><span>Search</span></div>
        <div class="list-stack">
          ${dashboard.recentSearches.map(listItem).join("")}
        </div>
      </div>
      <div class="content-card card-span-8 timeline-card">
        <div class="section-header"><h3>研究活动</h3><span>Timeline</span></div>
        <div class="timeline-list">
          ${dashboard.activity
            .map(
              (item) => `
                <div class="timeline-item">
                  <strong>${item.time}</strong>
                  <span>${item.text}</span>
                </div>
              `
            )
            .join("")}
        </div>
      </div>
    </div>
  `;

  return section;
}

function renderResearch() {
  const section = createModuleSection("学术调研", "围绕研究问题检索、筛选、总结，并将高价值结果沉淀到知识库。");
  const { research } = state.data;
  section.innerHTML += `
    <div class="search-shell">
      <div class="search-panel">
        <div class="section-header"><h3>研究问题检索</h3><span>Discovery</span></div>
        <form id="research-form" class="search-form">
          <input name="query" value="${escapeHtml(research.featuredQuery)}" placeholder="输入研究问题，而不是零散关键词">
          <button class="primary-button" type="submit">开始检索</button>
        </form>
        <div class="badge-line">
          ${research.filters.years.map((item) => `<button class="quick-filter" type="button">${item}</button>`).join("")}
          ${research.filters.types.map((item) => `<button class="quick-filter" type="button">${item}</button>`).join("")}
        </div>
      </div>
      <div class="two-columns">
        <div class="content-card">
          <div class="section-header"><h3>检索结果</h3><span id="research-result-count">${research.results.length} 条</span></div>
          <div id="research-results" class="list-stack">
            ${research.results.map(researchCardWithActions).join("")}
          </div>
        </div>
        <div class="content-card">
          <div class="section-header"><h3>AI 洞察</h3><span>Brief</span></div>
          <div id="research-insights" class="list-stack">
            ${research.insights.map((item) => infoCard(item.title, item.text)).join("")}
          </div>
        </div>
      </div>
      <div class="three-columns">
        <div class="content-card">
          <div class="section-header"><h3>快速总结</h3><span>Summary</span></div>
          <div id="research-summary" class="list-item empty-text">点击结果卡中的“总结”。</div>
        </div>
        <div class="content-card">
          <div class="section-header"><h3>思维导图</h3><span>Mindmap</span></div>
          <div class="button-row spaced-top">
            <button id="mindmap-export-svg" class="ghost-button" type="button">导出 SVG</button>
            <button id="mindmap-export-png" class="ghost-button" type="button">导出 PNG</button>
            <button id="mindmap-copy-source" class="ghost-button" type="button">复制源码</button>
            <button id="mindmap-preview" class="ghost-button" type="button">全屏预览</button>
          </div>
          <pre id="research-mindmap" class="mindmap-panel">点击结果卡中的“导图”。</pre>
        </div>
        <div class="content-card">
          <div class="section-header"><h3>历史搜索</h3><span>History</span></div>
          <div id="research-history" class="list-stack"></div>
        </div>
      </div>
    </div>
  `;

  queueMicrotask(async () => {
    document.getElementById("research-form").addEventListener("submit", submitResearchQuery);
    document.getElementById("mindmap-export-svg").addEventListener("click", exportMindmapSvg);
    document.getElementById("mindmap-export-png").addEventListener("click", exportMindmapPng);
    document.getElementById("mindmap-copy-source").addEventListener("click", copyMindmapSource);
    document.getElementById("mindmap-preview").addEventListener("click", openMindmapPreview);
    await loadResearchHistory();
    bindResearchActionButtons();
  });

  return section;
}

function renderKnowledge() {
  const section = createModuleSection("知识库", "现在支持真实的知识库、文档新增、文件上传、解析分块、RAG 问答和引用回溯。");
  section.innerHTML += `
    <div class="split-card">
      <div class="content-card">
        <div class="section-header"><h3>知识库管理</h3><span>CRUD</span></div>
        <form id="kb-base-form" class="stack-form">
          <div class="field-grid">
            <input class="compact-input" name="name" placeholder="新建知识库名称">
            <input class="compact-input" name="description" placeholder="描述">
          </div>
          <button class="primary-button compact" type="submit">新增知识库</button>
        </form>
        <div id="kb-base-list" class="list-stack spaced-top"></div>
        <div class="section-header spaced-top"><h3>文件夹</h3><span>Folder</span></div>
        <form id="kb-folder-form" class="stack-form">
          <input class="compact-input" name="name" placeholder="新建文件夹名称">
          <button class="secondary-button compact" type="submit">新增文件夹</button>
        </form>
        <div id="kb-folder-list" class="list-stack spaced-top"></div>
      </div>
      <div class="content-card">
        <div class="section-header"><h3>文档管理</h3><span>Docs</span></div>
        <div id="kb-active-hint" class="meta-line">请选择一个知识库。</div>
        <form id="kb-search-form" class="stack-form spaced-top">
          <input class="compact-input" name="keyword" placeholder="知识库搜索">
          <div class="button-row">
            <button class="ghost-button" type="submit">搜索</button>
            <button id="kb-summary-button" class="ghost-button" type="button">知识库总结</button>
          </div>
        </form>
        <form id="kb-text-form" class="stack-form spaced-top">
          <input class="compact-input" name="title" placeholder="新增文本资料标题">
          <input class="compact-input" name="sourceType" placeholder="来源类型，例如 NOTE / WEB / PAPER">
          <textarea name="content" placeholder="输入文本内容，将直接生成知识文档"></textarea>
          <button class="primary-button compact" type="submit">新增文本资料</button>
        </form>
        <form id="kb-upload-form" class="stack-form file-form spaced-top">
          <div class="field-grid">
            <input class="compact-input" name="sourceType" placeholder="上传来源类型">
            <input type="file" name="file">
          </div>
          <button class="secondary-button compact" type="submit">上传文件</button>
        </form>
        <div class="button-row spaced-top">
          <button id="kb-batch-delete" class="danger-button" type="button">批量删除已选文档</button>
        </div>
        <div id="kb-document-list" class="list-stack spaced-top"></div>
      </div>
    </div>
    <div class="three-columns">
      <div class="content-card">
        <div class="section-header"><h3>解析操作</h3><span>Parse</span></div>
        <div class="button-row">
          <button id="kb-parse-base" class="primary-button compact" type="button">解析整个知识库</button>
        </div>
        <p class="meta-line">上传文件后可重新解析，系统会自动切块并更新引用片段。</p>
      </div>
      <div class="content-card">
        <div class="section-header"><h3>会话列表</h3><span>Session</span></div>
        <form id="kb-session-form" class="stack-form">
          <input class="compact-input" name="title" placeholder="新建问答会话标题">
          <button class="secondary-button compact" type="submit">新建会话</button>
        </form>
        <div id="kb-session-list" class="list-stack"></div>
      </div>
      <div class="content-card">
        <div class="section-header"><h3>会话式 RAG</h3><span>Chat</span></div>
        <div id="kb-chat-list" class="list-stack"></div>
        <form id="kb-qa-form" class="stack-form spaced-top">
          <textarea name="question" placeholder="输入追问，系统会结合历史上下文和知识分块继续回答"></textarea>
          <button class="primary-button compact" type="submit">发送追问</button>
        </form>
      </div>
      <div class="content-card">
        <div class="section-header"><h3>引用证据</h3><span>Citations</span></div>
        <div id="kb-citation-list" class="list-stack">
          <div class="empty-text">点击文档中的“引用”按钮，或发送会话提问后查看证据。</div>
        </div>
      </div>
    </div>
  `;

  queueMicrotask(async () => {
    bindKnowledgeEvents();
    await loadKnowledgeData();
  });

  return section;
}

function renderWeb() {
  const section = createModuleSection("网页搜索", "聚合网页、社区和短视频线索，并生成可保存的 Markdown 摘要。");
  const { webSearch } = state.data;
  section.innerHTML += `
    <div class="search-shell">
      <div class="search-panel">
        <div class="section-header"><h3>网页趋势检索</h3><span>MetaSo</span></div>
        <form id="web-form" class="search-form">
          <input name="query" placeholder="搜索网页、社区或工具站内容">
          <button class="primary-button" type="submit">聚合结果</button>
        </form>
        <div class="badge-line">
          ${webSearch.tabs
            .map(
              (item) =>
                `<button class="${state.web.platform === item ? "quick-filter is-active" : "quick-filter"}" type="button" data-action="web-platform" data-platform="${item}">${item}</button>`
            )
            .join("")}
        </div>
      </div>
      <div class="two-columns">
        <div class="content-card">
          <div class="section-header"><h3>聚合结果</h3><span id="web-result-count">${webSearch.results.length} 条</span></div>
          <div id="web-results" class="list-stack">
            ${webSearch.results.map(webCardWithActions).join("")}
          </div>
        </div>
        <div class="markdown-panel">
          <div class="section-header"><h3>Markdown 摘要</h3><span>Output</span></div>
          <pre id="web-markdown">${escapeHtml(webSearch.markdown)}</pre>
        </div>
      </div>
      <div class="content-card">
        <div class="section-header"><h3>历史搜索</h3><span>History</span></div>
        <div id="web-history" class="list-stack"></div>
      </div>
    </div>
  `;

  queueMicrotask(async () => {
    document.getElementById("web-form").addEventListener("submit", submitWebQuery);
    document.querySelectorAll("[data-action='web-platform']").forEach((button) => {
      button.addEventListener("click", () => {
        state.web.platform = button.dataset.platform;
        renderModule();
      });
    });
    await loadWebHistory();
    bindWebActionButtons();
  });

  return section;
}

function renderAssistant() {
  const section = createModuleSection("AI 服务", "工具列表、提示词优化和 Bio 生物医学助手已经拆成可直接使用的工作区。");
  const { assistant } = state.data;
  section.innerHTML += `
    <div class="three-columns">
      <div class="content-card">
        <div class="section-header"><h3>工具列表</h3><span>Tools</span></div>
        <div id="ai-tool-list" class="list-stack"></div>
      </div>
      <div class="content-card">
        <div class="section-header"><h3>提示词优化</h3><span>Prompt</span></div>
        <form id="prompt-optimize-form" class="stack-form">
          <textarea name="prompt" placeholder="输入一个原始提示词"></textarea>
          <button class="primary-button compact" type="submit">优化提示词</button>
        </form>
        <div id="prompt-optimize-result" class="list-item empty-text">这里展示优化后的提示词。</div>
      </div>
      <div class="content-card">
        <div class="section-header"><h3>Bio 生物医学助手</h3><span>Bio</span></div>
        <form id="bio-assistant-form" class="stack-form">
          <textarea name="prompt" placeholder="输入生物医学问题"></textarea>
          <button class="secondary-button compact" type="submit">获取建议</button>
        </form>
        <div id="bio-assistant-result" class="list-item empty-text">这里展示 Bio 助手回答。</div>
      </div>
    </div>
    <div class="chat-panel spaced-top">
      <div class="section-header"><h3>通用助手会话流</h3><span>Assistant</span></div>
      <div id="assistant-messages" class="list-stack">
        ${state.assistantMessages.map(messageCard).join("")}
      </div>
      <form id="assistant-form" class="chat-form spaced-top">
        <textarea name="prompt" placeholder="例如：请把本周研究结果拆分成可执行计划，并附引用来源"></textarea>
        <button class="primary-button" type="submit">生成建议</button>
      </form>
    </div>
  `;

  queueMicrotask(async () => {
    document.getElementById("assistant-form").addEventListener("submit", submitAssistantPrompt);
    document.getElementById("prompt-optimize-form").addEventListener("submit", submitPromptOptimize);
    document.getElementById("bio-assistant-form").addEventListener("submit", submitBioAssistant);
    await loadAiTools();
  });

  return section;
}

function renderPlan() {
  const section = createModuleSection("研究计划", "任务、日历、番茄钟和打卡都已开放真实增删改接口。");
  section.innerHTML += `
    <div class="split-card">
      <div class="content-card">
        <div class="section-header"><h3>新增任务</h3><span>Task</span></div>
        <form id="plan-task-form" class="stack-form">
          <input class="compact-input" name="title" placeholder="任务标题">
          <input class="compact-input" name="description" placeholder="任务描述">
          <div class="field-grid">
            <input class="compact-input" type="number" min="1" max="3" name="priorityLevel" placeholder="优先级 1-3">
            <input class="compact-input" type="datetime-local" name="dueTime">
          </div>
          <button class="primary-button compact" type="submit">新增任务</button>
        </form>
        <div class="section-header spaced-top"><h3>任务清单</h3><span id="task-count">0 条</span></div>
        <div id="plan-task-list" class="list-stack"></div>
      </div>
      <div class="content-card">
        <div class="section-header"><h3>新增日历</h3><span>Calendar</span></div>
        <form id="plan-calendar-form" class="stack-form">
          <input class="compact-input" name="title" placeholder="日历标题">
          <input class="compact-input" name="description" placeholder="日历描述">
          <div class="field-grid">
            <input class="compact-input" type="datetime-local" name="startTime">
            <input class="compact-input" type="datetime-local" name="endTime">
          </div>
          <button class="secondary-button compact" type="submit">新增日历</button>
        </form>
        <div class="section-header spaced-top"><h3>日历事件</h3><span id="calendar-count">0 条</span></div>
        <div id="plan-calendar-list" class="list-stack"></div>
      </div>
    </div>
    <div class="three-columns">
      <div class="content-card">
        <div class="section-header"><h3>番茄钟</h3><span>Pomodoro</span></div>
        <form id="pomodoro-form" class="stack-form">
          <input class="compact-input" type="number" min="1" max="180" name="focusMinutes" placeholder="专注分钟（1-180）" value="25">
          <div class="metric-card">
            <span class="metric-label">倒计时</span>
            <strong id="pomodoro-display" class="metric-value">00:25:00</strong>
            <div class="button-row">
              <button id="pomodoro-start" class="primary-button compact" type="button">开始</button>
              <button id="pomodoro-stop" class="danger-button compact" type="button">提前结束</button>
            </div>
            <p id="pomodoro-status" class="meta-line">设置专注时长后点击开始。</p>
          </div>
        </form>
        <div id="pomodoro-list" class="list-stack spaced-top"></div>
      </div>
      <div class="content-card">
        <div class="section-header"><h3>定时 / 计时</h3><span>Timer</span></div>
        <div class="metric-card">
          <span class="metric-label">当前计时</span>
          <strong id="local-timer-display" class="metric-value">00:00:00</strong>
          <div class="button-row">
            <button id="timer-start" class="primary-button compact" type="button">开始</button>
            <button id="timer-stop" class="ghost-button" type="button">暂停</button>
            <button id="timer-reset" class="danger-button" type="button">重置</button>
          </div>
        </div>
      </div>
      <div class="content-card">
        <div class="section-header"><h3>打卡</h3><span>Checkin</span></div>
        <form id="checkin-form" class="stack-form">
          <div class="field-grid">
            <input class="compact-input" type="date" name="date">
            <input class="compact-input" type="number" name="focusMinutes" placeholder="专注分钟">
          </div>
          <div class="field-grid">
            <input class="compact-input" type="number" name="completedTaskCount" placeholder="完成任务数">
            <input class="compact-input" name="summary" placeholder="简短总结">
          </div>
          <button class="secondary-button compact" type="submit">提交打卡</button>
        </form>
        <div id="checkin-list" class="list-stack"></div>
      </div>
      <div class="content-card">
        <div class="section-header"><h3>专注趋势</h3><span>Focus</span></div>
        <div id="focus-trend" class="trend-bars"></div>
      </div>
    </div>
  `;

  queueMicrotask(async () => {
    bindPlanEvents();
    await loadPlanData();
    renderLocalTimer();
  });

  return section;
}

function renderProfile() {
  const section = createModuleSection("个人中心", "");
  const { profile } = state.data;
  const detail = profile.detail || {};
  section.innerHTML += `
    <div class="three-columns">
      <div class="content-card">
        <div class="section-header"><h3>个人资料</h3><span>Identity</span></div>
        <form id="profile-form" class="stack-form profile-form">
          <div class="profile-avatar-shell">
            <div class="profile-avatar-header">
              <img id="avatar-file-preview" class="avatar-file-preview hidden" src="" alt="头像预览">
              <div>
                <p class="title-line">头像</p>
                <p id="avatar-file-name" class="meta-line">上传图片更新头像</p>
              </div>
            </div>
            <div class="avatar-upload-row">
              <label class="secondary-button compact avatar-upload-button" for="profile-avatar-file">选择图片</label>
              <input id="profile-avatar-file" class="visually-hidden" name="avatarFile" type="file" accept="image/*">
              <input id="profile-avatar-url" name="avatarUrl" type="hidden" value="${escapeHtml(detail.avatarUrl || profile.user.avatarUrl || "")}">
            </div>
          </div>
          <div class="field-group">
            <label for="profile-real-name">姓名</label>
            <input id="profile-real-name" class="compact-input" name="realName" value="${escapeHtml(detail.realName ?? "")}" placeholder="输入姓名">
          </div>
          <div class="field-group">
            <label for="profile-gender">性别</label>
            <select id="profile-gender" class="compact-input" name="gender">
              <option value="0">保密</option>
              <option value="1">男</option>
              <option value="2">女</option>
            </select>
          </div>
          <div class="field-group">
            <label for="profile-institution">机构</label>
            <input id="profile-institution" class="compact-input" name="institution" value="${escapeHtml(profile.user.institution)}" placeholder="学校 / 机构">
          </div>
          <div class="field-group">
            <label for="profile-department">院系 / 部门</label>
            <input id="profile-department" class="compact-input" name="department" value="${escapeHtml(detail.department || "")}" placeholder="例如：计算生物学实验室">
          </div>
          <div class="field-grid">
            <div class="field-group">
              <label for="profile-degree">学历层级</label>
              <input id="profile-degree" class="compact-input" name="degreeLevel" value="${escapeHtml(detail.degreeLevel || "")}" placeholder="硕士 / 博士">
            </div>
            <div class="field-group">
              <label for="profile-direction">研究方向</label>
              <input id="profile-direction" class="compact-input" name="researchDirection" value="${escapeHtml(detail.researchDirection || "")}" placeholder="因果学习 / 生信">
            </div>
          </div>
          <div class="field-group">
            <label for="profile-bio">简介</label>
            <textarea id="profile-bio" name="bio" placeholder="介绍你的研究主题、方法和近期目标">${escapeHtml(profile.user.bio)}</textarea>
          </div>
          <button class="primary-button compact" type="submit">保存资料</button>
        </form>
      </div>
      <div class="content-card">
        <div class="section-header"><h3>绑定状态</h3><span>Binding</span></div>
        <div class="button-row">
          <a class="ghost-button external-link" href="/api/profile/oauth/wechat/authorize">微信 OAuth</a>
          <a class="ghost-button external-link" href="/api/profile/oauth/feishu/authorize">飞书 OAuth</a>
        </div>
        <form id="profile-binding-form" class="stack-form spaced-top">
          <input class="compact-input" name="platform" placeholder="平台，例如 wechat / feishu">
          <input class="compact-input" name="openId" placeholder="关联 ID">
          <button class="secondary-button compact" type="submit">保存绑定</button>
        </form>
        <div id="profile-bindings" class="list-stack spaced-top">${profile.bindings.map((item) => `<div class="source-pill">${item}</div>`).join("")}</div>
      </div>
      <div class="content-card">
        <div class="section-header"><h3>我的产出</h3><span>Outputs</span></div>
        <div class="list-stack">
          ${profile.outputs.map((item) => `<div class="source-pill">${item}</div>`).join("")}
        </div>
      </div>
    </div>
  `;

  queueMicrotask(() => {
    const profileForm = document.getElementById("profile-form");
    if (profileForm) {
      const realNameField = profileForm.elements.namedItem("realName");
      const genderField = profileForm.elements.namedItem("gender");
      const institutionField = profileForm.elements.namedItem("institution");
      const departmentField = profileForm.elements.namedItem("department");
      const degreeLevelField = profileForm.elements.namedItem("degreeLevel");
      const researchDirectionField = profileForm.elements.namedItem("researchDirection");
      const avatarUrlField = profileForm.elements.namedItem("avatarUrl");
      const bioTextArea = profileForm.elements.namedItem("bio");

      if (realNameField) realNameField.value = detail.realName ?? "";
      if (genderField) genderField.value = String(normalizeGenderValue(detail.gender ?? profile.user.gender));
      if (institutionField) institutionField.value = detail.institution || profile.user.institution || "";
      if (departmentField) departmentField.value = detail.department || "";
      if (degreeLevelField) degreeLevelField.value = detail.degreeLevel || "";
      if (researchDirectionField) researchDirectionField.value = detail.researchDirection || "";
      if (avatarUrlField) avatarUrlField.value = detail.avatarUrl || profile.user.avatarUrl || "";
      if (bioTextArea) bioTextArea.value = detail.bio || profile.user.bio || "";
      renderAvatarUploadPreview(detail.avatarUrl || profile.user.avatarUrl || "", "已保存头像");
    }

    bindAvatarFileUpload(profileForm);

    document.getElementById("profile-form").addEventListener("submit", saveProfile);
    document.getElementById("profile-binding-form").addEventListener("submit", saveBinding);
  });

  return section;
}

function renderRelax() {
  const section = createModuleSection("放松一下", "轻量娱乐功能，切换节奏，不进入严肃科研模式。");
  section.innerHTML += `
    <div class="two-columns">
      <div class="content-card">
        <div class="section-header"><h3>看你八字</h3><span>Fun</span></div>
        <form id="relax-bazi-form" class="stack-form">
          <input class="compact-input" name="input" placeholder="随便输入一句你的状态或生日（娱乐）">
          <button class="primary-button compact" type="submit">开始解读</button>
        </form>
        <div id="relax-bazi-result" class="list-item empty-text">这里展示娱乐化解读。</div>
      </div>
      <div class="content-card">
        <div class="section-header"><h3>今天吃什么？</h3><span>Food</span></div>
        <form id="relax-eat-form" class="stack-form">
          <input class="compact-input" name="input" placeholder="口味偏好，例如：辣一点、清淡、想吃面">
          <button class="secondary-button compact" type="submit">帮我决定</button>
        </form>
        <div id="relax-eat-result" class="list-item empty-text">这里展示推荐结果。</div>
      </div>
    </div>
  `;

  queueMicrotask(() => {
    document.getElementById("relax-bazi-form").addEventListener("submit", submitBazi);
    document.getElementById("relax-eat-form").addEventListener("submit", submitEat);
  });

  return section;
}

async function loadKnowledgeData() {
  const payload = await fetchJson("/api/knowledge/bases");
  state.knowledge.bases = payload.data;
  if (!state.knowledge.activeBaseId && payload.data.length > 0) {
    state.knowledge.activeBaseId = payload.data[0].id;
  }
  await loadKnowledgeFolders();
  renderKnowledgeBases();
  await loadKnowledgeDocuments();
  await loadKnowledgeSessions();
}

async function loadKnowledgeFolders() {
  if (!state.knowledge.activeBaseId) {
    state.knowledge.folders = [];
    return;
  }
  const payload = await fetchJson(`/api/knowledge/bases/${state.knowledge.activeBaseId}/folders`);
  state.knowledge.folders = payload.data;
}

async function loadKnowledgeDocuments() {
  if (!state.knowledge.activeBaseId) {
    state.knowledge.documents = [];
    renderKnowledgeDocuments();
    return;
  }
  const payload = await fetchJson(`/api/knowledge/bases/${state.knowledge.activeBaseId}/documents`);
  state.knowledge.documents = payload.data;
  renderKnowledgeDocuments();
}

function bindKnowledgeEvents() {
  document.getElementById("kb-base-form").addEventListener("submit", createKnowledgeBase);
  document.getElementById("kb-folder-form").addEventListener("submit", createKnowledgeFolder);
  document.getElementById("kb-search-form").addEventListener("submit", searchKnowledgeDocuments);
  document.getElementById("kb-text-form").addEventListener("submit", createKnowledgeTextDocument);
  document.getElementById("kb-upload-form").addEventListener("submit", uploadKnowledgeDocument);
  document.getElementById("kb-session-form").addEventListener("submit", createKnowledgeSession);
  document.getElementById("kb-qa-form").addEventListener("submit", askKnowledgeQuestion);
  document.getElementById("kb-parse-base").addEventListener("click", parseKnowledgeBase);
  document.getElementById("kb-batch-delete").addEventListener("click", batchDeleteKnowledgeDocuments);
  document.getElementById("kb-summary-button").addEventListener("click", summarizeKnowledgeBase);
}

function renderKnowledgeBases() {
  const container = document.getElementById("kb-base-list");
  const folderContainer = document.getElementById("kb-folder-list");
  if (!container) return;
  container.innerHTML = state.knowledge.bases.length
    ? state.knowledge.bases
        .map(
          (base) => `
            <div class="list-item ${base.id === state.knowledge.activeBaseId ? "selected-item" : ""}">
              <div class="action-row">
                <div>
                  <p class="title-line">${base.name}</p>
                  <p class="meta-line">${base.meta}</p>
                </div>
                <div class="button-row">
                  <button class="ghost-button" data-action="select-base" data-id="${base.id}">打开</button>
                  <button class="ghost-button" data-action="edit-base" data-id="${base.id}">编辑</button>
                  <button class="danger-button" data-action="delete-base" data-id="${base.id}">删除</button>
                </div>
              </div>
            </div>
          `
        )
        .join("")
    : `<div class="empty-text">还没有知识库，请先创建。</div>`;

  container.querySelectorAll("[data-action='select-base']").forEach((button) => {
    button.addEventListener("click", async () => {
      state.knowledge.activeBaseId = Number(button.dataset.id);
      state.knowledge.citations = [];
      state.knowledge.activeSessionId = null;
      state.knowledge.messages = [];
      renderKnowledgeBases();
      await loadKnowledgeDocuments();
      await loadKnowledgeSessions();
    });
  });

  container.querySelectorAll("[data-action='edit-base']").forEach((button) => {
    button.addEventListener("click", async () => {
      const base = state.knowledge.bases.find((item) => item.id === Number(button.dataset.id));
      const name = window.prompt("知识库名称", base.name);
      if (!name) return;
      const description = window.prompt("知识库描述", base.description || "");
      await fetchJson(`/api/knowledge/bases/${base.id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name, description }),
      });
      await loadKnowledgeData();
    });
  });

  container.querySelectorAll("[data-action='delete-base']").forEach((button) => {
    button.addEventListener("click", async () => {
      if (!window.confirm("确认删除该知识库及其文档？")) return;
      await fetchJson(`/api/knowledge/bases/${button.dataset.id}`, { method: "DELETE" });
      if (Number(button.dataset.id) === state.knowledge.activeBaseId) {
        state.knowledge.activeBaseId = null;
        state.knowledge.citations = [];
        state.knowledge.activeSessionId = null;
        state.knowledge.messages = [];
      }
      await loadKnowledgeData();
    });
  });

  if (folderContainer) {
    folderContainer.innerHTML = state.knowledge.folders.length
      ? state.knowledge.folders
          .map(
            (folder) => `
              <div class="list-item">
                <div class="action-row">
                  <div>
                    <p class="title-line">${folder.name}</p>
                    <p class="meta-line">排序 ${folder.sortNo}</p>
                  </div>
                  <div class="button-row">
                    <button class="ghost-button" data-action="rename-folder" data-id="${folder.id}">命名</button>
                    <button class="danger-button" data-action="delete-folder" data-id="${folder.id}">删除</button>
                  </div>
                </div>
              </div>
            `
          )
          .join("")
      : `<div class="empty-text">当前知识库暂无文件夹。</div>`;

    folderContainer.querySelectorAll("[data-action='rename-folder']").forEach((button) => {
      button.addEventListener("click", async () => {
        const folder = state.knowledge.folders.find((item) => item.id === Number(button.dataset.id));
        const name = window.prompt("文件夹名称", folder.name);
        if (!name) return;
        await fetchJson(`/api/knowledge/folders/${folder.id}`, {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ name }),
        });
        await loadKnowledgeFolders();
        renderKnowledgeBases();
      });
    });

    folderContainer.querySelectorAll("[data-action='delete-folder']").forEach((button) => {
      button.addEventListener("click", async () => {
        await fetchJson(`/api/knowledge/folders/${button.dataset.id}`, { method: "DELETE" });
        await loadKnowledgeFolders();
        renderKnowledgeBases();
      });
    });
  }
}

function renderKnowledgeDocuments() {
  const hint = document.getElementById("kb-active-hint");
  const container = document.getElementById("kb-document-list");
  const sessionList = document.getElementById("kb-session-list");
  const chatList = document.getElementById("kb-chat-list");
  const citations = document.getElementById("kb-citation-list");
  if (!hint || !container) return;

  const activeBase = state.knowledge.bases.find((item) => item.id === state.knowledge.activeBaseId);
  hint.textContent = activeBase ? `当前知识库：${activeBase.name}` : "请选择一个知识库。";
  if (sessionList) {
    sessionList.innerHTML = state.knowledge.sessions.length
      ? state.knowledge.sessions
          .map(
            (item) => `
              <div class="list-item ${item.id === state.knowledge.activeSessionId ? "selected-item" : ""}">
                <div class="action-row">
                  <div>
                    <p class="title-line">${item.title}</p>
                    <p class="meta-line">${item.updatedAt}</p>
                    <p class="meta-line">${item.preview || "暂无消息"}</p>
                  </div>
                  <div class="button-row">
                    <button class="ghost-button" data-action="select-session" data-id="${item.id}">打开</button>
                    <button class="ghost-button" data-action="rename-session" data-id="${item.id}">重命名</button>
                    <button class="ghost-button" data-action="auto-title-session" data-id="${item.id}">自动标题</button>
                    <button class="danger-button" data-action="delete-session" data-id="${item.id}">删除</button>
                  </div>
                </div>
              </div>
            `
          )
          .join("")
      : `<div class="empty-text">请先新建一个知识库会话。</div>`;
  }
  if (chatList) {
    chatList.innerHTML = state.knowledge.messages.length
      ? state.knowledge.messages
          .map(
            (item) => `
              <div class="message-item" data-role="${item.role}">
                <p class="title-line">${item.role === "assistant" ? "助手" : "你"}</p>
                <p class="meta-line">${escapeHtml(item.content).replace(/\n/g, "<br>")}</p>
                ${
                  item.role === "assistant"
                    ? `<div class="button-row spaced-top">
                        <button class="ghost-button" data-action="show-message-citations" data-id="${item.id}">查看引用</button>
                        <button class="secondary-button compact" data-action="save-message" data-id="${item.id}">回答入库</button>
                      </div>`
                    : ""
                }
              </div>
            `
          )
          .join("")
      : `<div class="empty-text">请选择一个会话，然后开始提问。</div>`;
  }
  if (citations) {
    citations.innerHTML = state.knowledge.citations.length
      ? state.knowledge.citations
          .map(
            (item) => `
              <div class="list-item">
                <p class="title-line">${item.documentTitle || `文档 ${item.documentId}`}</p>
                <p class="meta-line">片段 ${item.chunkNo || item.chunkId} · 分数 ${item.score ?? "-"}</p>
                <p class="meta-line">${escapeHtml(item.excerpt || item.content || "")}</p>
              </div>
            `
          )
          .join("")
      : `<div class="empty-text">点击文档中的“引用”按钮查看切片证据。</div>`;
  }

  container.innerHTML = state.knowledge.documents.length
    ? state.knowledge.documents
        .map(
          (doc) => `
            <div class="list-item">
              <div class="action-row">
                <div>
                  <label class="checkbox-row">
                    <input type="checkbox" data-action="select-doc" data-id="${doc.id}" ${state.knowledge.selectedDocumentIds.includes(doc.id) ? "checked" : ""}>
                    <span class="title-line">${doc.title}</span>
                  </label>
                  <p class="meta-line">${doc.source} · ${doc.status} · ${doc.date} · ${doc.chunkCount || 0} 段</p>
                  <p class="meta-line">${doc.summary || ""}</p>
                </div>
                <div class="button-row">
                  <button class="ghost-button" data-action="parse-doc" data-id="${doc.id}">解析</button>
                  <button class="ghost-button" data-action="show-citations" data-id="${doc.id}">引用</button>
                  <button class="ghost-button" data-action="edit-doc" data-id="${doc.id}">编辑</button>
                  <button class="danger-button" data-action="delete-doc" data-id="${doc.id}">删除</button>
                </div>
              </div>
            </div>
          `
        )
        .join("")
    : `<div class="empty-text">该知识库暂无文档。</div>`;

  container.querySelectorAll("[data-action='edit-doc']").forEach((button) => {
    button.addEventListener("click", async () => {
      const doc = state.knowledge.documents.find((item) => item.id === Number(button.dataset.id));
      const title = window.prompt("文档标题", doc.title);
      if (!title) return;
      const summary = window.prompt("文档摘要", doc.summary || "");
      const sourceType = window.prompt("来源类型", doc.source || "");
      await fetchJson(`/api/knowledge/documents/${doc.id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ title, summary, sourceType }),
      });
      await loadKnowledgeDocuments();
    });
  });

  container.querySelectorAll("[data-action='parse-doc']").forEach((button) => {
    button.addEventListener("click", async () => {
      await fetchJson(`/api/knowledge/documents/${button.dataset.id}/parse`, {
        method: "POST",
      });
      await loadKnowledgeDocuments();
    });
  });

  container.querySelectorAll("[data-action='select-doc']").forEach((checkbox) => {
    checkbox.addEventListener("change", () => {
      const id = Number(checkbox.dataset.id);
      if (checkbox.checked) {
        state.knowledge.selectedDocumentIds = [...new Set([...state.knowledge.selectedDocumentIds, id])];
      } else {
        state.knowledge.selectedDocumentIds = state.knowledge.selectedDocumentIds.filter((item) => item !== id);
      }
    });
  });

  container.querySelectorAll("[data-action='show-citations']").forEach((button) => {
    button.addEventListener("click", async () => {
      const payload = await fetchJson(`/api/knowledge/documents/${button.dataset.id}/citations`);
      state.knowledge.citations = payload.data;
      renderKnowledgeDocuments();
    });
  });

  container.querySelectorAll("[data-action='delete-doc']").forEach((button) => {
    button.addEventListener("click", async () => {
      if (!window.confirm("确认删除该文档？")) return;
      await fetchJson(`/api/knowledge/documents/${button.dataset.id}`, { method: "DELETE" });
      await loadKnowledgeDocuments();
    });
  });

  sessionList?.querySelectorAll("[data-action='select-session']").forEach((button) => {
    button.addEventListener("click", async () => {
      state.knowledge.activeSessionId = Number(button.dataset.id);
      await loadKnowledgeMessages();
    });
  });

  sessionList?.querySelectorAll("[data-action='rename-session']").forEach((button) => {
    button.addEventListener("click", async () => {
      const session = state.knowledge.sessions.find((item) => item.id === Number(button.dataset.id));
      const title = window.prompt("会话标题", session.title);
      if (!title) return;
      await fetchJson(`/api/knowledge/sessions/${session.id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ title }),
      });
      await loadKnowledgeSessions();
    });
  });

  sessionList?.querySelectorAll("[data-action='delete-session']").forEach((button) => {
    button.addEventListener("click", async () => {
      await fetchJson(`/api/knowledge/sessions/${button.dataset.id}`, { method: "DELETE" });
      if (Number(button.dataset.id) === state.knowledge.activeSessionId) {
        state.knowledge.activeSessionId = null;
        state.knowledge.messages = [];
      }
      await loadKnowledgeSessions();
    });
  });

  sessionList?.querySelectorAll("[data-action='auto-title-session']").forEach((button) => {
    button.addEventListener("click", async () => {
      await fetchJson(`/api/knowledge/sessions/${button.dataset.id}/auto-title`, { method: "POST" });
      await loadKnowledgeSessions();
    });
  });

  chatList?.querySelectorAll("[data-action='show-message-citations']").forEach((button) => {
    button.addEventListener("click", async () => {
      const message = state.knowledge.messages.find((item) => item.id === Number(button.dataset.id));
      state.knowledge.citations = message?.sourceRefs || [];
      renderKnowledgeDocuments();
    });
  });

  chatList?.querySelectorAll("[data-action='save-message']").forEach((button) => {
    button.addEventListener("click", async () => {
      const message = state.knowledge.messages.find((item) => item.id === Number(button.dataset.id));
      const title = window.prompt("保存为知识条目的标题", `会话回答-${message.id}`);
      if (!title) return;
      await fetchJson(`/api/knowledge/sessions/${state.knowledge.activeSessionId}/messages/${message.id}/save`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ title }),
      });
      await loadKnowledgeDocuments();
    });
  });
}

async function createKnowledgeBase(event) {
  event.preventDefault();
  const formData = new FormData(event.currentTarget);
  const name = String(formData.get("name") || "").trim();
  const description = String(formData.get("description") || "").trim();
  if (!name) return;
  await fetchJson("/api/knowledge/bases", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ name, description }),
  });
  event.currentTarget.reset();
  await loadKnowledgeData();
}

async function createKnowledgeFolder(event) {
  event.preventDefault();
  if (!state.knowledge.activeBaseId) {
    window.alert("请先选择知识库。");
    return;
  }
  const formData = new FormData(event.currentTarget);
  const name = String(formData.get("name") || "").trim();
  if (!name) return;
  await fetchJson(`/api/knowledge/bases/${state.knowledge.activeBaseId}/folders`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ name }),
  });
  event.currentTarget.reset();
  await loadKnowledgeFolders();
  renderKnowledgeBases();
}

async function searchKnowledgeDocuments(event) {
  event.preventDefault();
  if (!state.knowledge.activeBaseId) return;
  const keyword = String(new FormData(event.currentTarget).get("keyword") || "").trim();
  const payload = await fetchJson(`/api/knowledge/bases/${state.knowledge.activeBaseId}/documents/search?q=${encodeURIComponent(keyword)}`);
  state.knowledge.documents = payload.data;
  renderKnowledgeDocuments();
}

async function createKnowledgeTextDocument(event) {
  event.preventDefault();
  if (!state.knowledge.activeBaseId) {
    window.alert("请先选择知识库。");
    return;
  }
  const formData = new FormData(event.currentTarget);
  const title = String(formData.get("title") || "").trim();
  const content = String(formData.get("content") || "").trim();
  const sourceType = String(formData.get("sourceType") || "").trim();
  if (!title) return;
  await fetchJson(`/api/knowledge/bases/${state.knowledge.activeBaseId}/documents`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ title, content, sourceType }),
  });
  event.currentTarget.reset();
  await loadKnowledgeDocuments();
}

async function uploadKnowledgeDocument(event) {
  event.preventDefault();
  if (!state.knowledge.activeBaseId) {
    window.alert("请先选择知识库。");
    return;
  }
  const formData = new FormData(event.currentTarget);
  if (!formData.get("file") || formData.get("file").size === 0) {
    window.alert("请选择文件。");
    return;
  }
  await fetchJson(`/api/knowledge/bases/${state.knowledge.activeBaseId}/documents/upload`, {
    method: "POST",
    body: formData,
  });
  event.currentTarget.reset();
  await loadKnowledgeDocuments();
}

async function batchDeleteKnowledgeDocuments() {
  if (!state.knowledge.selectedDocumentIds.length) {
    window.alert("请先勾选文档。");
    return;
  }
  await fetchJson("/api/knowledge/documents/batch-delete", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ documentIds: state.knowledge.selectedDocumentIds }),
  });
  state.knowledge.selectedDocumentIds = [];
  await loadKnowledgeDocuments();
}

async function summarizeKnowledgeBase() {
  if (!state.knowledge.activeBaseId) return;
  const payload = await fetchJson(`/api/knowledge/bases/${state.knowledge.activeBaseId}/summary`);
  state.knowledge.citations = [];
  state.knowledge.messages = [
    {
      id: `summary-${Date.now()}`,
      role: "assistant",
      content: payload.data.summary,
      sourceRefs: [],
    },
  ];
  renderKnowledgeDocuments();
}

async function parseKnowledgeBase() {
  if (!state.knowledge.activeBaseId) {
    window.alert("请先选择知识库。");
    return;
  }
  await fetchJson(`/api/knowledge/bases/${state.knowledge.activeBaseId}/parse`, {
    method: "POST",
  });
  await loadKnowledgeDocuments();
}

async function loadKnowledgeSessions() {
  if (!state.knowledge.activeBaseId) {
    state.knowledge.sessions = [];
    state.knowledge.activeSessionId = null;
    state.knowledge.messages = [];
    renderKnowledgeDocuments();
    return;
  }
  const payload = await fetchJson(`/api/knowledge/bases/${state.knowledge.activeBaseId}/sessions`);
  state.knowledge.sessions = payload.data;
  if (!state.knowledge.activeSessionId && payload.data.length > 0) {
    state.knowledge.activeSessionId = payload.data[0].id;
  }
  await loadKnowledgeMessages();
}

async function loadKnowledgeMessages() {
  if (!state.knowledge.activeSessionId) {
    state.knowledge.messages = [];
    renderKnowledgeDocuments();
    return;
  }
  const payload = await fetchJson(`/api/knowledge/sessions/${state.knowledge.activeSessionId}/messages`);
  state.knowledge.messages = payload.data;
  renderKnowledgeDocuments();
}

async function createKnowledgeSession(event) {
  event.preventDefault();
  if (!state.knowledge.activeBaseId) {
    window.alert("请先选择知识库。");
    return;
  }
  const formData = new FormData(event.currentTarget);
  const title = String(formData.get("title") || "").trim();
  const payload = await fetchJson(`/api/knowledge/bases/${state.knowledge.activeBaseId}/sessions`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ title }),
  });
  state.knowledge.activeSessionId = payload.data.id;
  event.currentTarget.reset();
  await loadKnowledgeSessions();
}

async function askKnowledgeQuestion(event) {
  event.preventDefault();
  if (!state.knowledge.activeBaseId) {
    window.alert("请先选择知识库。");
    return;
  }
  if (!state.knowledge.activeSessionId) {
    window.alert("请先新建或选择一个会话。");
    return;
  }
  const formData = new FormData(event.currentTarget);
  const question = String(formData.get("question") || "").trim();
  if (!question) {
    return;
  }
  const payload = await fetchJson(`/api/knowledge/sessions/${state.knowledge.activeSessionId}/messages`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ question }),
  });
  state.knowledge.citations = payload.data.citations || [];
  await loadKnowledgeMessages();
  await loadKnowledgeSessions();
  event.currentTarget.reset();
}

async function loadResearchHistory() {
  const history = await fetchJson("/api/research/history");
  const kbOptions = await fetchJson("/api/research/knowledge-bases");
  state.research.history = history.data;
  state.research.kbOptions = kbOptions.data;
  const container = document.getElementById("research-history");
  if (container) {
    container.innerHTML = state.research.history.length
      ? state.research.history
          .map(
            (item) => `
              <div class="list-item">
                <div class="action-row">
                  <div>
                    <p class="title-line">${item.query}</p>
                    <p class="meta-line">${item.createdAt} · ${item.resultCount} 条</p>
                  </div>
                  <button class="secondary-button compact" data-action="research-history-save" data-id="${item.id}">保存到知识库</button>
                </div>
              </div>
            `
          )
          .join("")
      : `<div class="empty-text">暂无历史搜索。</div>`;
    container.querySelectorAll("[data-action='research-history-save']").forEach((button) => {
      button.addEventListener("click", async () => {
        await ensureKnowledgeBaseOptions();
        openKbPicker({
          title: "保存学术搜索历史到知识库",
          subtitle: "将本次历史搜索的结果集合保存为一条知识文档。",
          items: state.research.kbOptions,
          action: async (kbId) => {
            await fetchJson(`/api/research/tasks/${button.dataset.id}/save`, {
              method: "POST",
              headers: { "Content-Type": "application/json" },
              body: JSON.stringify({ kbId: Number(kbId) }),
            });
            window.alert("学术历史已保存到知识库");
          },
        });
      });
    });
  }
}

function bindResearchActionButtons() {
  document.querySelectorAll("[data-action='paper-summary']").forEach((button) => {
    button.addEventListener("click", async () => {
      const payload = await fetchJson(`/api/research/results/${button.dataset.id}/summary`, { method: "POST" });
      const panel = document.getElementById("research-summary");
      panel.innerHTML = `<p class="meta-line">${escapeHtml(payload.data.summary).replace(/\n/g, "<br>")}</p>`;
      panel.classList.remove("empty-text");
    });
  });

  document.querySelectorAll("[data-action='paper-mindmap']").forEach((button) => {
    button.addEventListener("click", async () => {
      const payload = await fetchJson(`/api/research/results/${button.dataset.id}/mindmap`, { method: "POST" });
      renderMindmap("research-mindmap", payload.data.mindmap);
    });
  });

  document.querySelectorAll("[data-action='paper-save']").forEach((button) => {
    button.addEventListener("click", async () => {
      await ensureKnowledgeBaseOptions();
      openKbPicker({
        title: "保存论文结果到知识库",
        subtitle: "请选择目标知识库，系统会把论文标题与摘要保存进去。",
        items: state.research.kbOptions,
        action: async (kbId) => {
          await fetchJson(`/api/research/results/${button.dataset.id}/save`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ kbId: Number(kbId) }),
          });
          window.alert("已加入知识库");
        },
      });
    });
  });
}

async function loadWebHistory() {
  const payload = await fetchJson("/api/websearch/history");
  state.web.history = payload.data;
  const container = document.getElementById("web-history");
  if (container) {
    container.innerHTML = state.web.history.length
      ? state.web.history
          .map(
            (item) => `
              <div class="list-item">
                <div class="action-row">
                  <div>
                    <p class="title-line">${item.query}</p>
                    <p class="meta-line">${item.createdAt} · ${item.resultCount} 条</p>
                  </div>
                  <button class="secondary-button compact" data-action="web-history-save" data-id="${item.id}">保存到知识库</button>
                </div>
              </div>
            `
          )
          .join("")
      : `<div class="empty-text">暂无网页搜索历史。</div>`;
    container.querySelectorAll("[data-action='web-history-save']").forEach((button) => {
      button.addEventListener("click", async () => {
        await ensureKnowledgeBaseOptions();
        openKbPicker({
          title: "保存网页搜索历史到知识库",
          subtitle: "将这次网页搜索的 Markdown 简报保存为知识文档。",
          items: state.research.kbOptions,
          action: async (kbId) => {
            await fetchJson(`/api/websearch/tasks/${button.dataset.id}/save`, {
              method: "POST",
              headers: { "Content-Type": "application/json" },
              body: JSON.stringify({ kbId: Number(kbId) }),
            });
            window.alert("网页历史已保存到知识库");
          },
        });
      });
    });
  }
}

function bindWebActionButtons() {
  document.querySelectorAll("[data-action='web-save']").forEach((button) => {
    button.addEventListener("click", async () => {
      await ensureKnowledgeBaseOptions();
      openKbPicker({
        title: "保存网页结果到知识库",
        subtitle: "请选择目标知识库，系统会把网页摘要和来源信息一起保存。",
        items: state.research.kbOptions,
        action: async (kbId) => {
          await fetchJson(`/api/websearch/results/${button.dataset.id}/save`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ kbId: Number(kbId) }),
          });
          window.alert("网页结果已保存到知识库");
        },
      });
    });
  });
}

async function loadAiTools() {
  const payload = await fetchJson("/api/ai/tools");
  state.ai.tools = payload.data;
  const container = document.getElementById("ai-tool-list");
  if (container) {
    container.innerHTML = state.ai.tools
      .map((tool) => `<a class="list-item external-link" target="_blank" href="${tool.url}"><p class="title-line">${tool.name}</p><p class="meta-line">${tool.category}</p></a>`)
      .join("");
  }
}

async function ensureKnowledgeBaseOptions() {
  if (state.research.kbOptions.length > 0) {
    return;
  }
  const kbOptions = await fetchJson("/api/research/knowledge-bases");
  state.research.kbOptions = kbOptions.data;
}

function openKbPicker({ title, subtitle, items, action }) {
  state.modal.open = true;
  state.modal.title = title;
  state.modal.subtitle = subtitle;
  state.modal.items = items;
  state.modal.action = action;
  renderKbPicker();
}

function closeKbPicker() {
  state.modal.open = false;
  state.modal.title = "";
  state.modal.subtitle = "";
  state.modal.items = [];
  state.modal.action = null;
  renderKbPicker();
}

function renderKbPicker() {
  if (!pickerModal || !pickerTitle || !pickerSubtitle || !pickerList) {
    return;
  }
  pickerModal.classList.toggle("hidden", !state.modal.open);
  pickerTitle.textContent = state.modal.title || "选择知识库";
  pickerSubtitle.textContent = state.modal.subtitle || "请选择目标知识库。";
  pickerList.innerHTML = state.modal.items.length
    ? state.modal.items
        .map(
          (item) => `
            <div class="list-item picker-item">
              <div>
                <p class="title-line">${item.name}</p>
                <p class="meta-line">ID: ${item.id}</p>
              </div>
              <button class="primary-button compact" data-action="picker-select" data-id="${item.id}">选择</button>
            </div>
          `
        )
        .join("")
    : `<div class="empty-text">暂无可选知识库。</div>`;

  pickerList.querySelectorAll("[data-action='picker-select']").forEach((button) => {
    button.addEventListener("click", async () => {
      const action = state.modal.action;
      closeKbPicker();
      if (action) {
        await action(button.dataset.id);
      }
    });
  });
}

async function submitPromptOptimize(event) {
  event.preventDefault();
  const prompt = String(new FormData(event.currentTarget).get("prompt") || "").trim();
  if (!prompt) return;
  const payload = await fetchJson("/api/ai/prompt-optimize", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ prompt }),
  });
  const panel = document.getElementById("prompt-optimize-result");
  panel.innerHTML = `<p class="meta-line">${escapeHtml(payload.data.result).replace(/\n/g, "<br>")}</p>`;
  panel.classList.remove("empty-text");
}

async function submitBioAssistant(event) {
  event.preventDefault();
  const prompt = String(new FormData(event.currentTarget).get("prompt") || "").trim();
  if (!prompt) return;
  const payload = await fetchJson("/api/ai/bio-assistant", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ prompt }),
  });
  const panel = document.getElementById("bio-assistant-result");
  panel.innerHTML = `<p class="meta-line">${escapeHtml(payload.data.result).replace(/\n/g, "<br>")}</p>`;
  panel.classList.remove("empty-text");
}

async function submitBazi(event) {
  event.preventDefault();
  const input = String(new FormData(event.currentTarget).get("input") || "").trim();
  const payload = await fetchJson("/api/relax/bazi", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ input }),
  });
  const panel = document.getElementById("relax-bazi-result");
  panel.innerHTML = `<p class="meta-line">${escapeHtml(payload.data.result).replace(/\n/g, "<br>")}</p>`;
  panel.classList.remove("empty-text");
}

async function submitEat(event) {
  event.preventDefault();
  const input = String(new FormData(event.currentTarget).get("input") || "").trim();
  const payload = await fetchJson("/api/relax/eat", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ input }),
  });
  const panel = document.getElementById("relax-eat-result");
  panel.innerHTML = `<p class="meta-line">${escapeHtml(payload.data.result).replace(/\n/g, "<br>")}</p>`;
  panel.classList.remove("empty-text");
}

async function saveProfile(event) {
  event.preventDefault();
  const formData = new FormData(event.currentTarget);
  const gender = normalizeGenderValue(formData.get("gender"));
  const currentAvatarUrl = String(
    formData.get("avatarUrl")
      || state.data?.profile?.detail?.avatarUrl
      || state.data?.profile?.user?.avatarUrl
      || "",
  ).trim();
  const payload = await fetchJson("/api/profile", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      realName: String(formData.get("realName") || ""),
      avatarUrl: currentAvatarUrl,
      gender,
      institution: String(formData.get("institution") || ""),
      department: String(formData.get("department") || ""),
      degreeLevel: String(formData.get("degreeLevel") || ""),
      researchDirection: String(formData.get("researchDirection") || ""),
      bio: String(formData.get("bio") || ""),
    }),
  });
  const profile = payload?.data || {};
  const detail = {
    realName: profile.realName ?? String(formData.get("realName") || ""),
    avatarUrl: profile.avatarUrl ?? currentAvatarUrl,
    gender: normalizeGenderValue(profile.gender ?? gender),
    institution: profile.institution ?? String(formData.get("institution") || ""),
    department: profile.department ?? String(formData.get("department") || ""),
    degreeLevel: profile.degreeLevel ?? String(formData.get("degreeLevel") || ""),
    researchDirection: profile.researchDirection ?? String(formData.get("researchDirection") || ""),
    bio: profile.bio ?? String(formData.get("bio") || ""),
  };
  const realName = String(detail.realName || "");
  const displayName = String(profile.displayName || "").trim() || realName || "研究者";
  const institution = detail.institution;
  const researchDirection = detail.researchDirection;
  const degreeLevel = detail.degreeLevel;
  const role = [degreeLevel, researchDirection].filter((item) => item && item.trim()).join(" / ") || "研究者";
  if (state.data?.profile) {
    state.data.profile.detail = detail;
    if (state.data.profile.user) {
      state.data.profile.user.name = displayName;
      state.data.profile.user.avatarUrl = detail.avatarUrl;
      state.data.profile.user.gender = detail.gender;
      state.data.profile.user.institution = institution;
      state.data.profile.user.role = role && role.trim() ? role : "研究者";
      state.data.profile.user.bio = detail.bio;
    }
  }
  renderSidebarAvatar(detail.avatarUrl, displayName);
  if (sidebarUser) sidebarUser.textContent = displayName;
  if (sidebarRole) sidebarRole.textContent = role && role.trim() ? role : "研究者";
  window.alert("资料已保存");
}

function bindAvatarFileUpload(profileForm) {
  if (!profileForm) {
    return;
  }
  const avatarFileInput = profileForm.elements.namedItem("avatarFile");
  const avatarUrlInput = profileForm.elements.namedItem("avatarUrl");
  if (!avatarFileInput || !avatarUrlInput) {
    return;
  }

  avatarFileInput.addEventListener("change", async () => {
    const file = avatarFileInput.files?.[0];
    if (!file) {
      return;
    }
    if (!file.type.startsWith("image/")) {
      window.alert("请选择图片文件");
      avatarFileInput.value = "";
      return;
    }

    const localPreviewUrl = URL.createObjectURL(file);
    renderAvatarUploadPreview(localPreviewUrl, `正在上传：${file.name}`);
    const uploadData = new FormData();
    uploadData.append("file", file);

    try {
      const payload = await fetchJson("/api/profile/avatar", {
        method: "POST",
        body: uploadData,
      });
      const avatarUrl = String(payload?.data?.avatarUrl || "").trim();
      if (!avatarUrl) {
        throw new Error("头像上传未返回地址");
      }
      avatarUrlInput.value = avatarUrl;
      updateProfileAvatarState(profileForm, avatarUrl);
      renderAvatarUploadPreview(avatarUrl, `已上传：${file.name}`);
    } catch (error) {
      const fallbackAvatarUrl = String(
        state.data?.profile?.detail?.avatarUrl || state.data?.profile?.user?.avatarUrl || "",
      ).trim();
      renderAvatarUploadPreview(fallbackAvatarUrl, "头像上传失败");
    } finally {
      URL.revokeObjectURL(localPreviewUrl);
      avatarFileInput.value = "";
    }
  });
}

function updateProfileAvatarState(profileForm, avatarUrl) {
  const displayName = String(profileForm.elements.namedItem("realName")?.value || state.data?.profile?.user?.name || "");
  const avatarUrlInput = profileForm.elements.namedItem("avatarUrl");
  if (avatarUrlInput) {
    avatarUrlInput.value = avatarUrl;
  }
  renderSidebarAvatar(avatarUrl, displayName);
  if (state.data?.profile) {
    state.data.profile.detail = { ...(state.data.profile.detail || {}), avatarUrl };
    if (state.data.profile.user) {
      state.data.profile.user.avatarUrl = avatarUrl;
    }
  }
}

function renderAvatarUploadPreview(avatarUrl, labelText) {
  const fileNameLabel = document.getElementById("avatar-file-name");
  const preview = document.getElementById("avatar-file-preview");
  if (!fileNameLabel || !preview) {
    return;
  }
  if (labelText) {
    fileNameLabel.textContent = labelText;
  }
  const safeUrl = String(avatarUrl || "").trim();
  if (!safeUrl) {
    preview.classList.add("hidden");
    preview.removeAttribute("src");
    return;
  }
  preview.src = buildAvatarImageUrl(safeUrl);
  preview.classList.remove("hidden");
  preview.onerror = () => {
    preview.classList.add("hidden");
    preview.removeAttribute("src");
  };
}

function normalizeGenderValue(value) {
  const numeric = Number(value);
  if (!Number.isFinite(numeric)) {
    return 0;
  }
  if (numeric < 0 || numeric > 2) {
    return 0;
  }
  return Math.trunc(numeric);
}

function renderSidebarAvatar(avatarUrl, displayName) {
  if (!sidebarAvatarRing || !sidebarAvatarImage || !sidebarAvatarFallback) {
    return;
  }
  const safeName = String(displayName || "").trim();
  const fallbackText = safeName ? safeName.slice(0, 1).toUpperCase() : "LAB";
  const safeAvatarUrl = String(avatarUrl || "").trim();
  sidebarAvatarFallback.textContent = fallbackText;
  if (safeAvatarUrl) {
    sidebarAvatarImage.src = buildAvatarImageUrl(safeAvatarUrl);
    sidebarAvatarImage.alt = `${safeName || "用户"}头像`;
    sidebarAvatarRing.classList.add("has-image");
    sidebarAvatarImage.onerror = () => {
      sidebarAvatarRing.classList.remove("has-image");
      sidebarAvatarImage.removeAttribute("src");
    };
    return;
  }
  sidebarAvatarRing.classList.remove("has-image");
  sidebarAvatarImage.removeAttribute("src");
}

function buildAvatarImageUrl(avatarUrl) {
  const safeUrl = String(avatarUrl || "").trim();
  if (!safeUrl || !safeUrl.startsWith("/api/profile/avatar/")) {
    return safeUrl;
  }
  const separator = safeUrl.includes("?") ? "&" : "?";
  return `${safeUrl}${separator}v=${Date.now()}`;
}

async function saveBinding(event) {
  event.preventDefault();
  const formData = new FormData(event.currentTarget);
  const platform = String(formData.get("platform") || "").trim();
  const openId = String(formData.get("openId") || "").trim();
  if (!platform || !openId) return;
  await fetchJson("/api/profile/bindings", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ platform, openId }),
  });
  const payload = await fetchJson("/api/profile/bindings");
  const container = document.getElementById("profile-bindings");
  if (container) {
    container.innerHTML = payload.data.map((item) => `<div class="source-pill">${item.platform}: ${item.openId}</div>`).join("");
  }
  event.currentTarget.reset();
}

async function loadPlanData() {
  const [tasksPayload, calendarPayload, pomodoroPayload, checkinPayload] = await Promise.all([
    fetchJson("/api/plans/tasks"),
    fetchJson("/api/plans/calendar"),
    fetchJson("/api/plans/pomodoro"),
    fetchJson("/api/plans/checkins"),
  ]);
  state.plan.tasks = tasksPayload.data;
  state.plan.calendar = calendarPayload.data;
  state.plan.pomodoro = pomodoroPayload.data;
  state.plan.checkins = checkinPayload.data;
  renderPlanData();
  renderFloatingCardWall();
}

function bindPlanEvents() {
  document.getElementById("plan-task-form").addEventListener("submit", createTask);
  document.getElementById("plan-calendar-form").addEventListener("submit", createCalendarEvent);
  document.getElementById("pomodoro-start").addEventListener("click", startPomodoroSession);
  document.getElementById("pomodoro-stop").addEventListener("click", stopPomodoroSessionEarly);
  document.getElementById("pomodoro-form").addEventListener("submit", (event) => event.preventDefault());
  document.getElementById("pomodoro-form").focusMinutes.addEventListener("change", syncPomodoroSessionPreset);
  document.getElementById("checkin-form").addEventListener("submit", createCheckin);
  document.getElementById("timer-start").addEventListener("click", startLocalTimer);
  document.getElementById("timer-stop").addEventListener("click", stopLocalTimer);
  document.getElementById("timer-reset").addEventListener("click", resetLocalTimer);
  renderPomodoroSessionState();
}

function renderPlanData() {
  const taskList = document.getElementById("plan-task-list");
  const calendarList = document.getElementById("plan-calendar-list");
  const pomodoroList = document.getElementById("pomodoro-list");
  const checkinList = document.getElementById("checkin-list");
  const focusTrend = document.getElementById("focus-trend");
  const taskCount = document.getElementById("task-count");
  const calendarCount = document.getElementById("calendar-count");

  if (!taskList) return;

  taskCount.textContent = `${state.plan.tasks.length} 条`;
  calendarCount.textContent = `${state.plan.calendar.length} 条`;

  taskList.innerHTML = state.plan.tasks.length
    ? state.plan.tasks
        .map(
          (item) => `
            <div class="list-item ${item.done ? "done-item" : ""}">
              <div class="action-row">
                <div>
                  <p class="title-line">${item.title}</p>
                  <p class="meta-line">${item.priority} · ${item.deadline || "待安排"}</p>
                  <p class="meta-line">${item.description || ""}</p>
                </div>
                <div class="button-row">
                  <button class="ghost-button" data-action="toggle-task" data-id="${item.id}">${item.done ? "重开" : "完成"}</button>
                  <button class="ghost-button" data-action="edit-task" data-id="${item.id}">编辑</button>
                  <button class="danger-button" data-action="delete-task" data-id="${item.id}">删除</button>
                </div>
              </div>
            </div>
          `
        )
        .join("")
    : `<div class="empty-text">暂无任务。</div>`;

  calendarList.innerHTML = state.plan.calendar.length
    ? state.plan.calendar
        .map(
          (item) => `
            <div class="list-item">
              <div class="action-row">
                <div>
                  <p class="title-line">${item.title}</p>
                  <p class="meta-line">${item.startTime} → ${item.endTime}</p>
                  <p class="meta-line">${item.description || ""}</p>
                </div>
                <div class="button-row">
                  <button class="ghost-button" data-action="edit-calendar" data-id="${item.id}">编辑</button>
                  <button class="danger-button" data-action="delete-calendar" data-id="${item.id}">删除</button>
                </div>
              </div>
            </div>
          `
        )
        .join("")
    : `<div class="empty-text">暂无日历事件。</div>`;

  pomodoroList.innerHTML = state.plan.pomodoro.length
    ? state.plan.pomodoro
        .map(
          (item) => `
            <div class="list-item">
              <div class="action-row">
                <div>
                  <p class="title-line">第 ${item.sessionCount} 次（今日第 ${item.dailySequence} 次）</p>
                  <p class="meta-line">时间：${item.sessionTime}</p>
                  <p class="meta-line">专注时长：${item.focusMinutes == null ? "—" : `${item.focusMinutes} 分钟`}</p>
                  <p class="meta-line">备注：${escapeHtml(item.desc || "")}</p>
                </div>
                <button class="danger-button" data-action="delete-pomodoro" data-id="${item.id}">删除</button>
              </div>
            </div>
          `
        )
        .join("")
    : `<div class="empty-text">暂无番茄钟记录。</div>`;

  checkinList.innerHTML = state.plan.checkins.length
    ? state.plan.checkins
        .map(
          (item) => `
            <div class="list-item">
              <div class="action-row">
                <div>
                  <p class="title-line">${item.date}</p>
                  <p class="meta-line">专注 ${item.focusMinutes} 分钟 · 完成 ${item.completedTaskCount} 项</p>
                  <p class="meta-line">${item.summary || ""}</p>
                </div>
                <button class="danger-button" data-action="delete-checkin" data-id="${item.id}">删除</button>
              </div>
            </div>
          `
        )
        .join("")
    : `<div class="empty-text">暂无打卡记录。</div>`;

  focusTrend.innerHTML = state.plan.pomodoro
    .slice(0, 7)
    .reverse()
    .map(
      (item, index) => `
        <div class="trend-bar">
          <span style="--height: ${Math.max(Number(item.focusMinutes || 0), 18)}%;"></span>
          <small>P${index + 1}</small>
        </div>
      `
    )
    .join("");

  bindPlanActionButtons();
}

function bindPlanActionButtons() {
  document.querySelectorAll("[data-action='toggle-task']").forEach((button) => {
    button.addEventListener("click", async () => {
      const item = state.plan.tasks.find((task) => task.id === Number(button.dataset.id));
      await fetchJson(`/api/plans/tasks/${item.id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          title: item.title,
          description: item.description,
          priorityLevel: item.priorityLevel,
          dueTime: normalizeDateTimeLocal(item.deadline),
          taskStatus: item.done ? "TODO" : "DONE",
        }),
      });
      await loadPlanData();
      ensureFloatingCard("task");
    });
  });

  document.querySelectorAll("[data-action='edit-task']").forEach((button) => {
    button.addEventListener("click", async () => {
      const item = state.plan.tasks.find((task) => task.id === Number(button.dataset.id));
      const title = window.prompt("任务标题", item.title);
      if (!title) return;
      const description = window.prompt("任务描述", item.description || "");
      const priorityLevel = Number(window.prompt("优先级 1-3", item.priorityLevel || 2));
      await fetchJson(`/api/plans/tasks/${item.id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          title,
          description,
          priorityLevel,
          dueTime: normalizeDateTimeLocal(item.deadline),
          taskStatus: item.taskStatus || (item.done ? "DONE" : "TODO"),
        }),
      });
      await loadPlanData();
      ensureFloatingCard("task");
    });
  });

  document.querySelectorAll("[data-action='delete-task']").forEach((button) => {
    button.addEventListener("click", async () => {
      await fetchJson(`/api/plans/tasks/${button.dataset.id}`, { method: "DELETE" });
      await loadPlanData();
      ensureFloatingCard("task");
    });
  });

  document.querySelectorAll("[data-action='edit-calendar']").forEach((button) => {
    button.addEventListener("click", async () => {
      const item = state.plan.calendar.find((calendar) => calendar.id === Number(button.dataset.id));
      const title = window.prompt("日历标题", item.title);
      if (!title) return;
      const description = window.prompt("日历描述", item.description || "");
      await fetchJson(`/api/plans/calendar/${item.id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          title,
          description,
          startTime: normalizeDateTimeLocal(item.startTime),
          endTime: normalizeDateTimeLocal(item.endTime),
        }),
      });
      await loadPlanData();
      ensureFloatingCard("calendar");
    });
  });

  document.querySelectorAll("[data-action='delete-calendar']").forEach((button) => {
    button.addEventListener("click", async () => {
      await fetchJson(`/api/plans/calendar/${button.dataset.id}`, { method: "DELETE" });
      await loadPlanData();
      ensureFloatingCard("calendar");
    });
  });

  document.querySelectorAll("[data-action='delete-pomodoro']").forEach((button) => {
    button.addEventListener("click", async () => {
      await fetchJson(`/api/plans/pomodoro/${button.dataset.id}`, { method: "DELETE" });
      await loadPlanData();
      ensureFloatingCard("pomodoro");
    });
  });

  document.querySelectorAll("[data-action='delete-checkin']").forEach((button) => {
    button.addEventListener("click", async () => {
      await fetchJson(`/api/plans/checkins/${button.dataset.id}`, { method: "DELETE" });
      await loadPlanData();
    });
  });
}

async function createTask(event) {
  event.preventDefault();
  const formData = new FormData(event.currentTarget);
  const title = String(formData.get("title") || "").trim();
  if (!title) return;
  await fetchJson("/api/plans/tasks", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      title,
      description: String(formData.get("description") || "").trim(),
      priorityLevel: Number(formData.get("priorityLevel") || 2),
      dueTime: formData.get("dueTime") ? `${formData.get("dueTime")}:00` : null,
      taskStatus: "TODO",
    }),
  });
  event.currentTarget.reset();
  await loadPlanData();
  ensureFloatingCard("task");
}

async function createCalendarEvent(event) {
  event.preventDefault();
  const formData = new FormData(event.currentTarget);
  const title = String(formData.get("title") || "").trim();
  const startTime = String(formData.get("startTime") || "").trim();
  const endTime = String(formData.get("endTime") || "").trim();
  if (!title) return;
  if (!startTime || !endTime) {
    window.alert("请填写开始时间和结束时间。");
    return;
  }
  await fetchJson("/api/plans/calendar", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      title,
      description: String(formData.get("description") || "").trim(),
      startTime: `${startTime}:00`,
      endTime: `${endTime}:00`,
    }),
  });
  event.currentTarget.reset();
  await loadPlanData();
  ensureFloatingCard("calendar");
}

function syncPomodoroSessionPreset() {
  const form = document.getElementById("pomodoro-form");
  if (!form) return;
  const focusMinutes = Number(form.focusMinutes.value || 25);
  const normalized = Number.isFinite(focusMinutes) && focusMinutes > 0 ? Math.min(180, Math.max(1, focusMinutes)) : 25;
  state.plan.pomodoroSession.focusMinutes = normalized;
  if (!state.plan.pomodoroSession.running) {
    state.plan.pomodoroSession.remainingSeconds = normalized * 60;
    renderPomodoroSessionState();
  }
}

function renderPomodoroSessionState() {
  const display = document.getElementById("pomodoro-display");
  const status = document.getElementById("pomodoro-status");
  const startButton = document.getElementById("pomodoro-start");
  const stopButton = document.getElementById("pomodoro-stop");
  if (!display || !status || !startButton || !stopButton) return;
  display.textContent = formatTimerText(state.plan.pomodoroSession.remainingSeconds);
  if (state.plan.pomodoroSession.running) {
    status.textContent = `专注进行中，目标 ${state.plan.pomodoroSession.focusMinutes} 分钟。`;
  } else if (state.plan.pomodoroSession.phase === "done") {
    status.textContent = "本次番茄钟已完成并入表。";
  } else if (state.plan.pomodoroSession.phase === "aborted") {
    status.textContent = "本次提前结束，已记录为未完成。";
  } else {
    status.textContent = "设置专注时长后点击开始。";
  }
  startButton.disabled = state.plan.pomodoroSession.running;
  stopButton.disabled = !state.plan.pomodoroSession.running;
}

function startPomodoroSession() {
  syncPomodoroSessionPreset();
  if (state.plan.pomodoroSession.running) return;
  state.plan.pomodoroSession.phase = "focus";
  state.plan.pomodoroSession.running = true;
  state.plan.pomodoroSession.remainingSeconds = state.plan.pomodoroSession.focusMinutes * 60;
  state.plan.pomodoroSession.cycleStartedAt = new Date();
  state.plan.pomodoroSession.handle = window.setInterval(() => {
    state.plan.pomodoroSession.remainingSeconds = Math.max(0, state.plan.pomodoroSession.remainingSeconds - 1);
    renderPomodoroSessionState();
    if (state.plan.pomodoroSession.remainingSeconds <= 0) {
      void finalizePomodoroSession(true);
    }
  }, POMODORO_TICK_MS);
  renderPomodoroSessionState();
}

function stopPomodoroSessionEarly() {
  if (!state.plan.pomodoroSession.running) return;
  void finalizePomodoroSession(false);
}

async function finalizePomodoroSession(completed) {
  if (!state.plan.pomodoroSession.cycleStartedAt) return;
  const startedAt = state.plan.pomodoroSession.cycleStartedAt;
  const endedAt = new Date();
  state.plan.pomodoroSession.running = false;
  if (state.plan.pomodoroSession.handle) {
    window.clearInterval(state.plan.pomodoroSession.handle);
    state.plan.pomodoroSession.handle = null;
  }
  await fetchJson("/api/plans/pomodoro", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      focusMinutes: state.plan.pomodoroSession.focusMinutes,
      completed,
      sessionTime: startedAt.toISOString().slice(0, 19),
      finishedAt: endedAt.toISOString().slice(0, 19),
      desc: completed ? "已完成" : "未完成",
    }),
  });
  state.plan.pomodoroSession.phase = completed ? "done" : "aborted";
  if (completed) {
    state.plan.pomodoroSession.completedCycles += 1;
  }
  state.plan.pomodoroSession.remainingSeconds = state.plan.pomodoroSession.focusMinutes * 60;
  await loadPlanData();
  ensureFloatingCard("pomodoro");
  renderPomodoroSessionState();
}

async function createCheckin(event) {
  event.preventDefault();
  const formData = new FormData(event.currentTarget);
  await fetchJson("/api/plans/checkins", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      date: formData.get("date") || null,
      focusMinutes: Number(formData.get("focusMinutes") || 0),
      completedTaskCount: Number(formData.get("completedTaskCount") || 0),
      summary: String(formData.get("summary") || "").trim(),
    }),
  });
  event.currentTarget.reset();
  await loadPlanData();
}

function formatTimerText(totalSeconds) {
  const hours = String(Math.floor(totalSeconds / 3600)).padStart(2, "0");
  const minutes = String(Math.floor((totalSeconds % 3600) / 60)).padStart(2, "0");
  const seconds = String(totalSeconds % 60).padStart(2, "0");
  return `${hours}:${minutes}:${seconds}`;
}

function getDateTimestamp(value) {
  const timestamp = new Date(value || "").getTime();
  return Number.isNaN(timestamp) ? 0 : timestamp;
}

function formatFloatingDateTime(value) {
  if (!value) {
    return "未设置";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return String(value);
  }
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  const hours = String(date.getHours()).padStart(2, "0");
  const minutes = String(date.getMinutes()).padStart(2, "0");
  return `${month}/${day} ${hours}:${minutes}`;
}

function getActiveTaskItem() {
  const openTasks = state.plan.tasks.filter((item) => !item.done);
  const sorted = [...(openTasks.length ? openTasks : state.plan.tasks)].sort(
    (left, right) => Number(right.id || 0) - Number(left.id || 0)
  );
  return sorted[0] || null;
}

function getUpcomingCalendarItem() {
  const sorted = [...state.plan.calendar].sort(
    (left, right) => getDateTimestamp(left.startTime) - getDateTimestamp(right.startTime)
  );
  return sorted.find((item) => getDateTimestamp(item.startTime) >= Date.now()) || sorted[0] || null;
}

function getLatestPomodoroItem() {
  const sorted = [...state.plan.pomodoro].sort((left, right) => {
    const timeDelta = getDateTimestamp(right.sessionTime) - getDateTimestamp(left.sessionTime);
    return timeDelta || Number(right.id || 0) - Number(left.id || 0);
  });
  return sorted[0] || null;
}

function floatingMetaRow(label, value) {
  return `
    <div class="floating-feature-meta-row">
      <span>${escapeHtml(label)}</span>
      <strong>${escapeHtml(value)}</strong>
    </div>
  `;
}

function buildFloatingTimerCard() {
  return `
    <article class="floating-feature-card is-timer" data-floating-card="timer">
      <div class="floating-feature-head">
        <div class="floating-feature-heading">
          <span class="floating-feature-kicker">${FLOATING_CARD_META.timer.kicker}</span>
          <strong>${FLOATING_CARD_META.timer.label}</strong>
        </div>
        <button class="floating-feature-close" type="button" data-floating-close="timer" aria-label="关闭计时卡片">×</button>
      </div>
      <strong class="floating-feature-value">${formatTimerText(state.plan.timerSeconds)}</strong>
      <p class="floating-feature-copy">${state.plan.timerRunning ? "计时进行中" : "计时已暂停"}</p>
      <div class="floating-feature-actions">
        <button
          class="${state.plan.timerRunning ? "primary-button compact" : "ghost-button compact"}"
          type="button"
          data-floating-action="toggle-timer"
        >
          ${state.plan.timerRunning ? "暂停" : "继续"}
        </button>
        <button class="danger-button compact" type="button" data-floating-action="reset-timer">重置</button>
      </div>
    </article>
  `;
}

function buildFloatingTaskCard() {
  const currentTask = getActiveTaskItem();
  const pendingCount = state.plan.tasks.filter((item) => !item.done).length;
  const title = currentTask ? currentTask.title : "暂无任务";
  const description = currentTask?.description || "新建或操作任务后，会在这里显示摘要。";

  return `
    <article class="floating-feature-card is-task" data-floating-card="task">
      <div class="floating-feature-head">
        <div class="floating-feature-heading">
          <span class="floating-feature-kicker">${FLOATING_CARD_META.task.kicker}</span>
          <strong>${FLOATING_CARD_META.task.label}</strong>
        </div>
        <button class="floating-feature-close" type="button" data-floating-close="task" aria-label="关闭任务卡片">×</button>
      </div>
      <strong class="floating-feature-title">${escapeHtml(title)}</strong>
      <p class="floating-feature-copy">${escapeHtml(description)}</p>
      <div class="floating-feature-meta">
        ${floatingMetaRow("待办", `${pendingCount} / ${state.plan.tasks.length}`)}
        ${floatingMetaRow("截止", currentTask ? formatFloatingDateTime(currentTask.deadline) : "未设置")}
      </div>
    </article>
  `;
}

function buildFloatingCalendarCard() {
  const currentEvent = getUpcomingCalendarItem();
  const title = currentEvent ? currentEvent.title : "暂无日程";
  const description = currentEvent?.description || "新增或调整日历事件后，会在这里显示摘要。";

  return `
    <article class="floating-feature-card is-calendar" data-floating-card="calendar">
      <div class="floating-feature-head">
        <div class="floating-feature-heading">
          <span class="floating-feature-kicker">${FLOATING_CARD_META.calendar.kicker}</span>
          <strong>${FLOATING_CARD_META.calendar.label}</strong>
        </div>
        <button class="floating-feature-close" type="button" data-floating-close="calendar" aria-label="关闭日历卡片">×</button>
      </div>
      <strong class="floating-feature-title">${escapeHtml(title)}</strong>
      <p class="floating-feature-copy">${escapeHtml(description)}</p>
      <div class="floating-feature-meta">
        ${floatingMetaRow("事件", `${state.plan.calendar.length} 条`)}
        ${floatingMetaRow("时间", currentEvent ? formatFloatingDateTime(currentEvent.startTime) : "待安排")}
      </div>
    </article>
  `;
}

function buildFloatingPomodoroCard() {
  const currentRecord = getLatestPomodoroItem();
  const title = currentRecord
    ? `第 ${currentRecord.sessionCount} 次 · 今日第 ${currentRecord.dailySequence} 次`
    : "暂无记录";

  return `
    <article class="floating-feature-card is-pomodoro" data-floating-card="pomodoro">
      <div class="floating-feature-head">
        <div class="floating-feature-heading">
          <span class="floating-feature-kicker">${FLOATING_CARD_META.pomodoro.kicker}</span>
          <strong>${FLOATING_CARD_META.pomodoro.label}</strong>
        </div>
        <button class="floating-feature-close" type="button" data-floating-close="pomodoro" aria-label="关闭番茄钟卡片">×</button>
      </div>
      <strong class="floating-feature-title">${escapeHtml(title)}</strong>
      <p class="floating-feature-copy">${currentRecord ? `状态：${escapeHtml(currentRecord.desc || "")}` : "开始番茄钟后，会在这里显示最新状态。"}</p>
      <div class="floating-feature-meta">
        ${floatingMetaRow("记录", `${state.plan.pomodoro.length} 条`)}
        ${floatingMetaRow("时间", currentRecord ? formatFloatingDateTime(currentRecord.sessionTime) : "待开始")}
      </div>
    </article>
  `;
}

function buildFloatingCardMarkup(type) {
  if (type === "timer") return buildFloatingTimerCard();
  if (type === "task") return buildFloatingTaskCard();
  if (type === "calendar") return buildFloatingCalendarCard();
  if (type === "pomodoro") return buildFloatingPomodoroCard();
  return "";
}

function syncFloatingWallVisibility() {
  const wall = document.getElementById("floating-timer");
  const toast = document.getElementById("floating-card-toast");
  if (!wall) return;
  const hasToast = Boolean(toast && !toast.classList.contains("hidden"));
  const hasFloatingCards = state.plan.floatingCards.length > 0;
  wall.classList.toggle("hidden", !hasFloatingCards && !hasToast);
  const workspace = document.querySelector(".workspace");
  if (workspace) {
    const hadFloatingCards = workspace.classList.contains("has-floating-cards");
    workspace.classList.toggle("has-floating-cards", hasFloatingCards);
    if (hasFloatingCards && !hadFloatingCards) {
      workspace.classList.remove("is-floating-shrink-anim");
      void workspace.offsetWidth;
      workspace.classList.add("is-floating-shrink-anim");
      window.setTimeout(() => {
        workspace.classList.remove("is-floating-shrink-anim");
      }, 460);
    }
  }
}

function renderFloatingCardWall() {
  const stack = document.getElementById("floating-card-stack");
  if (!stack) return;
  const total = state.plan.floatingCards.length;
  stack.innerHTML = state.plan.floatingCards
    .map((type, index) =>
      buildFloatingCardMarkup(type).replace(
        /<article class="([^"]*floating-feature-card[^"]*)"/,
        `<article class="$1" style="--stack-index:${index};--stack-depth:${total};"`
      )
    )
    .join("");
  syncFloatingWallVisibility();
  syncFloatingTimerCardState();
}

function showFloatingCardToast(message) {
  const toast = document.getElementById("floating-card-toast");
  if (!toast) return;
  if (state.plan.floatingToastHandle) {
    window.clearTimeout(state.plan.floatingToastHandle);
  }
  toast.textContent = message;
  toast.classList.remove("hidden");
  syncFloatingWallVisibility();
  state.plan.floatingToastHandle = window.setTimeout(() => {
    toast.classList.add("hidden");
    state.plan.floatingToastHandle = null;
    syncFloatingWallVisibility();
  }, 500);
}

function ensureFloatingCard(type) {
  if (state.plan.floatingCards.includes(type)) {
    renderFloatingCardWall();
    return true;
  }
  if (state.plan.floatingCards.length >= FLOATING_CARD_LIMIT) {
    showFloatingCardToast("超过限制，无法摆放");
    return false;
  }
  state.plan.floatingCards = [...state.plan.floatingCards, type];
  renderFloatingCardWall();
  return true;
}

function closeFloatingCard(type) {
  state.plan.floatingCards = state.plan.floatingCards.filter((item) => item !== type);
  renderFloatingCardWall();
}

function syncFloatingTimerCardState(formattedText = formatTimerText(state.plan.timerSeconds)) {
  const timerCard = document.querySelector("[data-floating-card='timer']");
  if (!timerCard) return;
  const value = timerCard.querySelector(".floating-feature-value");
  const description = timerCard.querySelector(".floating-feature-copy");
  const toggleButton = timerCard.querySelector("[data-floating-action='toggle-timer']");
  if (value) {
    value.textContent = formattedText;
  }
  if (description) {
    description.textContent = state.plan.timerRunning ? "计时进行中" : "计时已暂停";
  }
  if (toggleButton) {
    toggleButton.textContent = state.plan.timerRunning ? "暂停" : "继续";
    toggleButton.className = state.plan.timerRunning ? "primary-button compact" : "ghost-button compact";
  }
}

function renderLocalTimer() {
  const text = formatTimerText(state.plan.timerSeconds);
  const moduleDisplay = document.getElementById("local-timer-display");
  if (moduleDisplay) moduleDisplay.textContent = text;
  syncFloatingTimerCardState(text);
}

function syncFloatingTimerButton() {
  const btn = document.getElementById("floating-timer-toggle");
  if (!btn) return;
  btn.textContent = state.plan.timerRunning ? "暂停" : "继续";
  btn.className = state.plan.timerRunning ? "primary-button compact" : "ghost-button compact";
}

function showFloatingTimer() {
  const widget = document.getElementById("floating-timer");
  if (widget) widget.classList.remove("hidden");
}

function startLocalTimer() {
  if (state.plan.timerRunning) return;
  state.plan.timerRunning = true;
  showFloatingTimer();
  syncFloatingTimerButton();
  state.plan.timerHandle = window.setInterval(() => {
    state.plan.timerSeconds += 1;
    renderLocalTimer();
  }, 1000);
}

function stopLocalTimer() {
  state.plan.timerRunning = false;
  if (state.plan.timerHandle) {
    window.clearInterval(state.plan.timerHandle);
    state.plan.timerHandle = null;
  }
  syncFloatingTimerButton();
}

function resetLocalTimer() {
  stopLocalTimer();
  state.plan.timerSeconds = 0;
  renderLocalTimer();
}

function bindFloatingTimer() {
  document.getElementById("floating-timer-close")?.addEventListener("click", () => {
    document.getElementById("floating-timer")?.classList.add("hidden");
  });
  document.getElementById("floating-timer-toggle")?.addEventListener("click", () => {
    if (state.plan.timerRunning) {
      stopLocalTimer();
    } else {
      startLocalTimer();
    }
  });
  document.getElementById("floating-timer-reset")?.addEventListener("click", () => {
    resetLocalTimer();
  });
}

async function submitResearchQuery(event) {
  event.preventDefault();
  const query = new FormData(event.currentTarget).get("query");
  const payload = await fetchJson("/api/workbench/research/query", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ query }),
  });
  const results = payload.data.results;
  document.getElementById("research-result-count").textContent = `${results.length} 条`;
  document.getElementById("research-insights").innerHTML = payload.data.insights.map((item) => infoCard(item.title, item.text)).join("");
  document.getElementById("research-results").innerHTML = results.map(researchCardWithActions).join("");
  bindResearchActionButtons();
}

async function submitWebQuery(event) {
  event.preventDefault();
  const query = new FormData(event.currentTarget).get("query");
  const payload = await fetchJson("/api/workbench/web/query", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ query, platform: state.web.platform }),
  });
  document.getElementById("web-results").innerHTML = payload.data.results.map(webCardWithActions).join("");
  document.getElementById("web-result-count").textContent = `${payload.data.results.length} 条`;
  document.getElementById("web-markdown").textContent = payload.data.markdown;
  bindWebActionButtons();
  await loadWebHistory();
}

async function submitAssistantPrompt(event) {
  event.preventDefault();
  const formData = new FormData(event.currentTarget);
  const prompt = String(formData.get("prompt") || "").trim();
  if (!prompt) return;

  state.assistantMessages.push({ role: "user", content: prompt });
  syncAssistantMessages();

  const payload = await fetchJson("/api/workbench/assistant/chat", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      prompt,
      sources: state.data.assistant.sources.slice(0, 3),
    }),
  });
  state.assistantMessages.push({ role: "assistant", content: payload.data.answer });
  syncAssistantMessages();
  event.currentTarget.reset();
}

function syncAssistantMessages() {
  const container = document.getElementById("assistant-messages");
  if (container) {
    container.innerHTML = state.assistantMessages.map(messageCard).join("");
  }
}

function createModuleSectionLegacy(title, subtitle) {
  const section = document.createElement("section");
  section.className = "module-section glass-panel";
  section.innerHTML = `
    <div class="section-header">
      <div>
        <h2 class="module-title">${title}</h2>
        <p class="section-subtext">${subtitle}</p>
      </div>
    </div>
  `;
  return section;
}

function createModuleSection(title, subtitle) {
  const section = document.createElement("section");
  const activeModule = getActiveModuleMeta();
  section.className = "module-section glass-panel";
  section.innerHTML = `
    <div class="section-header module-shell-header">
      <div class="module-heading">
        <span class="section-eyebrow">${activeModule?.label || "Workspace"}</span>
        <h2 class="module-title">${title}</h2>
        ${subtitle ? `<p class="section-subtext">${subtitle}</p>` : ""}
      </div>
    </div>
  `;
  return section;
}

async function fetchJson(url, options = {}) {
  const { skipAuthRedirect = false, headers, ...fetchOptions } = options;
  const response = await fetch(url, {
    credentials: "same-origin",
    ...fetchOptions,
    headers,
  });
  let payload = null;
  const contentType = response.headers.get("content-type") || "";
  if (contentType.includes("application/json")) {
    payload = await response.json();
  }
  if (response.status === 401 && !skipAuthRedirect) {
    const next = `${window.location.pathname}${window.location.search}${window.location.hash}`;
    window.location.href = `/login.html?next=${encodeURIComponent(next)}`;
    throw new Error("请先登录");
  }
  if (!response.ok) {
    const message = payload?.message || `请求失败: ${response.status}`;
    window.alert(message);
    throw new Error(message);
  }
  return payload;
}

function metricCard(item) {
  return `
    <article class="metric-card">
      <span class="metric-label">${item.label}</span>
      <strong class="metric-value">${item.value}</strong>
      <small class="metric-detail">${item.detail}</small>
    </article>
  `;
}

function listItem(item) {
  return `
    <div class="list-item">
      <p class="title-line">${item.title}</p>
      <p class="meta-line">${item.time} · ${item.meta}</p>
    </div>
  `;
}

function infoCard(title, text) {
  return `
    <div class="list-item">
      <p class="title-line">${title}</p>
      <p class="meta-line">${text}</p>
    </div>
  `;
}

function researchCard(item) {
  return `
    <article class="list-item">
      <p class="title-line">${item.title}</p>
      <p class="meta-line">${item.authors}</p>
      <p class="meta-line">${item.source} · ${item.year} · 引用 ${item.citations}</p>
      <p class="meta-line">${item.summary}</p>
      <div class="badge-line">
        <span class="pill">${item.badge}</span>
        <span class="pill">${item.tag}</span>
      </div>
    </article>
  `;
}

function researchCardWithActions(item) {
  return `
    <article class="list-item">
      <p class="title-line">${item.title}</p>
      <p class="meta-line">${item.authors}</p>
      <p class="meta-line">${item.source} · ${item.year} · 引用 ${item.citations}</p>
      <p class="meta-line">${item.summary}</p>
      <div class="badge-line">
        <span class="pill">${item.badge}</span>
        <span class="pill">${item.tag}</span>
      </div>
      <div class="button-row spaced-top">
        <button class="ghost-button" data-action="paper-summary" data-id="${item.id || ""}">总结</button>
        <button class="ghost-button" data-action="paper-mindmap" data-id="${item.id || ""}">导图</button>
        <button class="secondary-button compact" data-action="paper-save" data-id="${item.id || ""}">加入知识库</button>
      </div>
    </article>
  `;
}

function webCard(item) {
  return `
    <article class="list-item">
      <p class="title-line">${item.title}</p>
      <p class="meta-line">${item.platform} · ${item.author}</p>
      <p class="meta-line">${item.summary}</p>
      <div class="badge-line"><span class="pill">${item.badge}</span></div>
    </article>
  `;
}

function webCardWithActions(item) {
  return `
    <article class="list-item">
      <p class="title-line">${item.title}</p>
      <p class="meta-line">${item.platform} · ${item.author}</p>
      <p class="meta-line">${item.summary}</p>
      <div class="badge-line"><span class="pill">${item.badge}</span></div>
      <div class="button-row spaced-top">
        <button class="secondary-button compact" data-action="web-save" data-id="${item.id || ""}">保存</button>
      </div>
    </article>
  `;
}

function messageCard(item) {
  return `
    <div class="message-item" data-role="${item.role}">
      <p class="title-line">${item.role === "assistant" ? "助手" : "你"}</p>
      <p class="meta-line">${escapeHtml(item.content).replace(/\n/g, "<br>")}</p>
    </div>
  `;
}

function normalizeDateTimeLocal(text) {
  if (!text) {
    return null;
  }
  return text.replace(" ", "T") + ":00";
}

async function renderMindmap(targetId, source) {
  const target = document.getElementById(targetId);
  if (!target) return;
  if (!source) {
    target.textContent = "暂无思维导图内容。";
    target.dataset.source = "";
    return;
  }
  target.dataset.source = source;
  if (window.mermaid) {
    try {
      const graph = source.includes("mindmap") ? source : `mindmap\n  root((${source}))`;
      const renderId = `mermaid-${Date.now()}`;
      const { svg } = await window.mermaid.render(renderId, graph);
      target.innerHTML = svg;
      return;
    } catch (error) {
      target.textContent = source;
      return;
    }
  }
  target.textContent = source;
}

function exportMindmapSvg() {
  const svg = document.querySelector("#research-mindmap svg");
  if (!svg) {
    window.alert("当前没有可导出的思维导图。");
    return;
  }
  const blob = new Blob([svg.outerHTML], { type: "image/svg+xml;charset=utf-8" });
  downloadBlob(blob, "research-mindmap.svg");
}

async function copyMindmapSource() {
  const container = document.getElementById("research-mindmap");
  const source = container?.dataset.source || "";
  if (!source) {
    window.alert("当前没有可复制的源码。");
    return;
  }
  await navigator.clipboard.writeText(source);
  showStatusBanner("Mermaid 源码已复制");
}

function openMindmapPreview() {
  const svg = document.querySelector("#research-mindmap svg");
  if (!mindmapModal || !mindmapModalContent) {
    return;
  }
  if (svg) {
    mindmapModalContent.innerHTML = svg.outerHTML;
  } else {
    mindmapModalContent.textContent = document.getElementById("research-mindmap")?.dataset.source || "暂无思维导图";
  }
  mindmapModal.classList.remove("hidden");
}

function closeMindmapPreview() {
  mindmapModal?.classList.add("hidden");
}

function exportMindmapPng() {
  const svg = document.querySelector("#research-mindmap svg");
  if (!svg) {
    window.alert("当前没有可导出的思维导图。");
    return;
  }
  const blob = new Blob([svg.outerHTML], { type: "image/svg+xml;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const image = new Image();
  image.onload = () => {
    const canvas = document.createElement("canvas");
    canvas.width = image.width || 1600;
    canvas.height = image.height || 900;
    const context = canvas.getContext("2d");
    context.fillStyle = "#ffffff";
    context.fillRect(0, 0, canvas.width, canvas.height);
    context.drawImage(image, 0, 0);
    canvas.toBlob((pngBlob) => {
      if (pngBlob) {
        downloadBlob(pngBlob, "research-mindmap.png");
      }
      URL.revokeObjectURL(url);
    });
  };
  image.onerror = () => {
    URL.revokeObjectURL(url);
    window.alert("PNG 导出失败。");
  };
  image.src = url;
}

function downloadBlob(blob, fileName) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  link.click();
  URL.revokeObjectURL(url);
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

boot().catch((error) => {
  moduleStage.innerHTML = `
    <section class="loading-state glass-panel">
      <div>
        <h3>工作台加载失败</h3>
        <p class="empty-text">${escapeHtml(error.message || "未知错误")}</p>
      </div>
    </section>
  `;
});
