<template>
  <header class="sticky top-0 z-20 border-b border-brand-900/20 bg-brand-700 text-white shadow-sm">
    <div class="mx-auto flex w-full max-w-6xl items-center justify-between gap-3 px-4 py-3 sm:px-6">
      <div class="flex min-w-0 items-center gap-3">
        <span aria-hidden="true" class="grid size-9 shrink-0 place-items-center rounded-xl bg-white/15 text-lg">⚑</span>
        <span class="min-w-0">
          <span class="block truncate text-sm font-semibold sm:text-base">Retirement Home RMS</span>
          <span class="block truncate text-xs text-brand-100">Care Roster · Retirement Home</span>
        </span>
      </div>

      <div class="flex shrink-0 items-center gap-2 sm:gap-4">
        <div v-if="auth.user" class="hidden text-right sm:block">
          <p class="text-sm font-medium">{{ auth.user.fullName }}</p>
          <p class="text-xs text-brand-100">{{ roleLabel }}</p>
        </div>
        <button
          v-if="auth.isAuthenticated"
          type="button"
          @click="handleLogout"
          class="inline-flex items-center gap-2 rounded-lg bg-white/15 px-3 py-1.5 text-sm font-medium text-white transition-colors hover:bg-white/25"
        >
          <LogOut class="size-4" aria-hidden="true" />
          <span>Logout</span>
        </button>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRouter } from "vue-router";
import { LogOut } from "@lucide/vue";
import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore();
const router = useRouter();

const ROLE_LABEL: Record<string, string> = {
  ROLE_ADMIN: "Administrator",
  ROLE_WORKER: "Support worker",
};

const roleLabel = computed(() => ROLE_LABEL[auth.user?.roles[0] ?? ""] ?? "Signed in");

function handleLogout() {
  auth.logout();
  router.push("/login");
}
</script>
