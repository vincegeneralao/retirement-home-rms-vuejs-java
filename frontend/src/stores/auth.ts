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

export const useAuthStore = defineStore("auth", () => {
  const token = ref<string | null>(localStorage.getItem("rms_token"));
  const user = ref<SessionUser | null>(
    (() => {
      const raw = localStorage.getItem("rms_user");
      return raw ? (JSON.parse(raw) as SessionUser) : null;
    })(),
  );

  const isAuthenticated = computed(() => !!token.value && !!user.value);
  const isAdmin = computed(() => user.value?.roles.includes("ROLE_ADMIN") ?? false);
  const isWorker = computed(() => user.value?.roles.includes("ROLE_WORKER") ?? false);

  async function login(username: string, password: string): Promise<SessionUser> {
    const resp: AuthResponse = await api.login(username, password);
    token.value = resp.token;
    user.value = {
      userId: resp.userId,
      username: resp.username,
      fullName: resp.fullName,
      roles: resp.roles,
    };
    persist();
    return user.value;
  }

  function logout(): void {
    token.value = null;
    user.value = null;
    localStorage.removeItem("rms_token");
    localStorage.removeItem("rms_user");
  }

  function persist(): void {
    if (token.value) localStorage.setItem("rms_token", token.value);
    if (user.value) localStorage.setItem("rms_user", JSON.stringify(user.value));
  }

  return { token, user, isAuthenticated, isAdmin, isWorker, login, logout };
});
