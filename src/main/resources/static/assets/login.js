(function () {
  const loginForm = document.getElementById("login-form");
  const registerForm = document.getElementById("register-form");
  const tabs = Array.from(document.querySelectorAll(".tab"));
  const panelTitle = document.getElementById("panel-title");
  const panelNote = document.getElementById("panel-note");
  const authStatus = document.getElementById("auth-status");
  const switchAuthMode = document.getElementById("switch-auth-mode");
  const heroRegister = document.getElementById("hero-register");
  const nextPath = new URLSearchParams(window.location.search).get("next") || "/";

  const copy = {
    login: {
      title: "登录你的研究工作台",
      note: "已有账号后可直接进入个人工作区。"
    },
    register: {
      title: "创建新的研究工作台",
      note: "注册后系统会自动初始化你的个人资料、工作区和欢迎会话。"
    }
  };

  function setMode(mode) {
    tabs.forEach((tab) => tab.classList.toggle("is-active", tab.dataset.mode === mode));
    loginForm.classList.toggle("hidden", mode !== "login");
    registerForm.classList.toggle("hidden", mode !== "register");
    panelTitle.textContent = copy[mode].title;
    panelNote.textContent = copy[mode].note;
    hideStatus();
  }

  function showStatus(message) {
    authStatus.textContent = message;
    authStatus.classList.remove("hidden");
  }

  function hideStatus() {
    authStatus.classList.add("hidden");
  }

  async function request(url, options, redirectOn401 = false) {
    const response = await fetch(url, {
      credentials: "same-origin",
      headers: {
        "Content-Type": "application/json",
        ...(options && options.headers ? options.headers : {})
      },
      ...options
    });

    let payload = null;
    const contentType = response.headers.get("content-type") || "";
    if (contentType.includes("application/json")) {
      payload = await response.json();
    }

    if (response.status === 401 && redirectOn401) {
      return null;
    }

    if (!response.ok) {
      throw new Error(payload?.message || "请求失败");
    }

    return payload;
  }

  async function checkSession() {
    try {
      const payload = await request("/api/auth/me", { method: "GET" }, true);
      if (payload?.data) {
        window.location.href = nextPath;
      }
    } catch (_) {
      // Ignore. User is not logged in yet.
    }
  }

  loginForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    hideStatus();
    const formData = new FormData(loginForm);
    try {
      await request("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({
          identifier: String(formData.get("identifier") || "").trim(),
          password: String(formData.get("password") || "")
        })
      });
      window.location.href = nextPath;
    } catch (error) {
      showStatus(error.message);
    }
  });

  registerForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    hideStatus();
    const formData = new FormData(registerForm);
    const password = String(formData.get("password") || "");
    const confirmPassword = String(formData.get("confirmPassword") || "");
    if (password !== confirmPassword) {
      showStatus("两次输入的密码不一致");
      return;
    }
    try {
      await request("/api/auth/register", {
        method: "POST",
        body: JSON.stringify({
          username: String(formData.get("username") || "").trim(),
          nickname: String(formData.get("nickname") || "").trim(),
          email: String(formData.get("email") || "").trim(),
          password
        })
      });
      window.location.href = nextPath;
    } catch (error) {
      showStatus(error.message);
    }
  });

  tabs.forEach((tab) => {
    tab.addEventListener("click", () => setMode(tab.dataset.mode));
  });

  switchAuthMode?.addEventListener("click", () => {
    const nextMode = document.querySelector(".tab.is-active")?.dataset.mode === "login" ? "register" : "login";
    setMode(nextMode);
  });

  heroRegister?.addEventListener("click", () => {
    setMode("register");
    document.getElementById("auth-panel")?.scrollIntoView({ behavior: "smooth", block: "start" });
  });

  setMode("login");
  checkSession();
})();
