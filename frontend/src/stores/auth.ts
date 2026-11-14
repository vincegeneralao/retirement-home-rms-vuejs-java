import { defineStore } from "pinia";
import { ref, computed } from "vue";
import type { AuthResponse } from "@/types/api";
import * as api from "@/api/services";

export interface SessionUser {
  userId: number;
  username: string;
  fullName: string;
  roles: string[];
}

const TOKEN_KEY = "rms_token";
const USER_KEY = "rms_user";
const RESET_KEY = "rms_password_reset_required";

export const useAuthStore = defineStore("auth", () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY));
  const user = ref<SessionUser | null>(
    (() => {
      const raw = localStorage.getItem(USER_KEY);
      return raw ? (JSON.parse(raw) as SessionUser) : null;
    })(),
  );
  // Held separately from `user` so the flag survives a refresh without widening SessionUser.
  const mustChangePassword = ref(localStorage.getItem(RESET_KEY) === "true");

  const isAuthenticated = computed(() => !!token.value && !!user.value);
  const isAdmin = computed(() => user.value?.roles.includes("ROLE_ADMIN") ?? false);
  const isWorker = computed(() => user.value?.roles.includes("ROLE_WORKER") ?? false);

  /** Stores a freshly issued session. Password changes rotate the token, so this is reused. */
  function applySession(resp: AuthResponse): void {
    token.value = resp.token;
    user.value = {
      userId: resp.userId,
      username: resp.username,
      fullName: resp.fullName,
      roles: resp.roles,
    };
    mustChangePassword.value = resp.mustChangePassword === true;
    persist();
  }

  async function login(username: string, password: string): Promise<SessionUser> {
    applySession(await api.login(username, password));
    return user.value as SessionUser;
  }

  /** Replaces the password with the worker's own choice and adopts the rotated token. */
  async function changePassword(currentPassword: string, newPassword: string): Promise<void> {
    applySession(await api.changePassword(currentPassword, newPassword));
  }

  /** Keeps the blank password an admin reset the account to. */
  async function keepResetPassword(): Promise<void> {
    applySession(await api.keepResetPassword());
  }

  function logout(): void {
    token.value = null;
    user.value = null;
    mustChangePassword.value = false;
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    localStorage.removeItem(RESET_KEY);
  }

  function persist(): void {
    if (token.value) localStorage.setItem(TOKEN_KEY, token.value);
    if (user.value) localStorage.setItem(USER_KEY, JSON.stringify(user.value));
    localStorage.setItem(RESET_KEY, String(mustChangePassword.value));
  }

  return {
    token,
    user,
    mustChangePassword,
    isAuthenticated,
    isAdmin,
    isWorker,
    login,
    logout,
    changePassword,
    keepResetPassword,
  };
});
