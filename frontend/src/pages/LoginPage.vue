<template>
  <div class="min-h-dvh">
    <main class="mx-auto grid w-full max-w-md place-items-center px-4 py-12">
      <section class="w-full">
        <div class="mb-6 text-center">
          <span aria-hidden="true" class="mx-auto grid size-12 place-items-center rounded-2xl bg-brand-700 text-2xl text-white">⚑</span>
          <h1 class="mt-3 text-2xl font-semibold text-brand-700">Sign In</h1>
          <p class="mt-1 text-sm text-slate-500">
            Sign in to see your roster of shifts for today.
          </p>
        </div>

        <UiCard>
          <form @submit.prevent="handleSubmit" novalidate class="space-y-4">
            <div>
              <UiLabel for="username" class="mb-1.5 block">Username</UiLabel>
              <UiInput id="username" v-model="username" type="text" autocomplete="username" placeholder="grace.loke" required />
            </div>
            <div>
              <UiLabel for="password" class="mb-1.5 block">Password</UiLabel>
              <UiInput id="password" v-model="password" type="password" autocomplete="current-password" placeholder="••••••••" required />
            </div>

            <UiAlert v-if="errorMsg" variant="destructive" role="alert">
              {{ errorMsg }}
            </UiAlert>

            <UiButton type="submit" :disabled="loading" class="w-full">
              <Loader2 v-if="loading" class="animate-spin" aria-hidden="true" />
              {{ loading ? "Signing in…" : "Sign In" }}
            </UiButton>
          </form>
        </UiCard>

        <details class="mt-4 rounded-2xl border border-slate-200 bg-white p-4 text-sm shadow-sm">
          <summary class="cursor-pointer font-medium text-slate-700">Demo accounts</summary>
          <ul class="mt-2 space-y-1 text-slate-500">
            <li><code class="rounded bg-slate-100 px-1">admin</code> / <code class="rounded bg-slate-100 px-1">Admin#2026</code> — administrator</li>
            <li><code class="rounded bg-slate-100 px-1">grace.loke</code> / <code class="rounded bg-slate-100 px-1">Worker#2026</code> — support worker</li>
            <li><code class="rounded bg-slate-100 px-1">priya.raman</code> / <code class="rounded bg-slate-100 px-1">Worker#2026</code> — support worker</li>
          </ul>
        </details>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import { Loader2 } from "@lucide/vue";
import { useAuthStore } from "@/stores/auth";
import UiCard from "@/components/ui/UiCard.vue";
import UiInput from "@/components/ui/UiInput.vue";
import UiLabel from "@/components/ui/UiLabel.vue";
import UiButton from "@/components/ui/UiButton.vue";
import UiAlert from "@/components/ui/UiAlert.vue";

const router = useRouter();
const auth = useAuthStore();
const username = ref("");
const password = ref("");
const loading = ref(false);
const errorMsg = ref<string | null>(null);

async function handleSubmit() {
  loading.value = true;
  errorMsg.value = null;
  try {
    const user = await auth.login(username.value, password.value);
    const target = user.roles.includes("ROLE_ADMIN") ? "/admin" : "/worker";
    router.push(target);
  } catch (err: any) {
    errorMsg.value = err.response?.data?.detail || "Invalid username or password";
  } finally {
    loading.value = false;
  }
}
</script>
