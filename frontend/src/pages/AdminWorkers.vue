<template>
  <div class="min-h-dvh pb-12">
    <TopBar />

    <main class="mx-auto w-full max-w-5xl px-4 pt-6 sm:px-6 sm:pt-8">
      <header class="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 class="text-2xl font-semibold text-brand-700 sm:text-3xl">Worker roster</h1>
          <p class="mt-1 text-sm text-slate-500">Add support workers or deactivate their accounts.</p>
        </div>
        <router-link
          to="/admin"
          class="inline-flex items-center gap-2 rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-700 shadow-sm transition-colors hover:bg-slate-50 hover:text-slate-900"
        >
          <ArrowLeft class="size-4" aria-hidden="true" />
          Back to Dashboard
        </router-link>
      </header>

      <!-- Add worker form -->
      <UiCard class="mb-6">
        <h2 class="text-lg font-semibold text-slate-900">Add a support worker</h2>
        <p class="mt-1 text-sm text-slate-500">The account is always created with the support worker role.</p>

        <form @submit.prevent="handleAdd" novalidate class="mt-4 grid gap-3 sm:grid-cols-2">
          <div>
            <UiLabel for="w-username" class="mb-1.5 block">Username</UiLabel>
            <UiInput id="w-username" v-model="form.username" autocomplete="off" placeholder="priya.raman" required />
          </div>
          <div>
            <UiLabel for="w-fullName" class="mb-1.5 block">Full name</UiLabel>
            <UiInput id="w-fullName" v-model="form.fullName" autocomplete="off" placeholder="Priya Raman" required />
          </div>
          <div class="sm:col-span-2">
            <UiLabel for="w-password" class="mb-1.5 block">Initial password</UiLabel>
            <UiInput id="w-password" v-model="form.password" type="password" autocomplete="new-password" placeholder="At least 8 characters" required />
          </div>

          <UiAlert v-if="addMsg && !addOk" variant="destructive">{{ addMsg }}</UiAlert>
          <UiAlert v-else-if="addMsg" variant="success">{{ addMsg }}</UiAlert>

          <div class="sm:col-span-2">
            <UiButton type="submit">Add worker</UiButton>
          </div>
        </form>
      </UiCard>

      <!-- Workers list -->
      <StatePanels :loading="loading" :error="error" :empty="!loading && !error && workers.length === 0" />

      <section v-if="!loading && !error && workers.length > 0" class="grid gap-3">
        <UiCard v-for="w in workers" :key="w.id" :class="w.enabled ? '' : 'opacity-70'">
          <div class="flex flex-wrap items-center justify-between gap-3">
            <div class="min-w-0">
              <div class="flex flex-wrap items-center gap-2">
                <p class="truncate text-base font-semibold text-slate-900">{{ w.fullName }}</p>
                <UiBadge v-if="!w.enabled" variant="secondary">deactivated</UiBadge>
                <UiBadge v-if="w.shiftsToday > 0" variant="outline" class="border-brand-200 bg-brand-50 text-brand-700">
                  {{ w.shiftsToday }} shift{{ w.shiftsToday === 1 ? "" : "s" }} today
                </UiBadge>
              </div>
              <p class="truncate text-sm text-slate-500">@{{ w.username }}</p>
            </div>
            <UiButton v-if="w.enabled" variant="destructive" size="sm" @click="handleDeactivate(w.id)">
              Deactivate
            </UiButton>
          </div>

          <p class="mt-3 text-sm text-slate-600">
            <template v-if="w.availability.length === 0">No weekly availability recorded.</template>
          </p>
          <ul v-if="w.availability.length > 0" class="mt-3 flex flex-wrap gap-2" :aria-label="`${w.fullName} availability windows`">
            <li
              v-for="a in w.availability"
              :key="a.id"
              class="inline-flex items-center gap-2 rounded-full bg-brand-50 px-3 py-1 text-sm text-brand-700 ring-1 ring-inset ring-brand-100"
            >
              <span class="font-medium">{{ a.dayOfWeek }}</span>
              <span class="tabular-nums">{{ a.startTime }}–{{ a.endTime }}</span>
            </li>
          </ul>
        </UiCard>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ArrowLeft } from "@lucide/vue";
import TopBar from "@/components/TopBar.vue";
import StatePanels from "@/components/StatePanels.vue";
import UiButton from "@/components/ui/UiButton.vue";
import UiCard from "@/components/ui/UiCard.vue";
import UiInput from "@/components/ui/UiInput.vue";
import UiLabel from "@/components/ui/UiLabel.vue";
import UiBadge from "@/components/ui/UiBadge.vue";
import UiAlert from "@/components/ui/UiAlert.vue";
import { getAdminWorkers, createWorker, deactivateWorker } from "@/api/services";
import type { WorkerView } from "@/types/api";

const workers = ref<WorkerView[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);
const form = reactive({ username: "", fullName: "", password: "" });
const addMsg = ref("");
const addOk = ref(false);

async function loadWorkers() {
  loading.value = true;
  error.value = null;
  try {
    workers.value = await getAdminWorkers();
  } catch (err: any) {
    error.value = err.response?.data?.detail || "Failed to load workers";
  } finally {
    loading.value = false;
  }
}

async function handleAdd() {
  addMsg.value = "";
  addOk.value = false;
  try {
    await createWorker({ ...form });
    addOk.value = true;
    addMsg.value = "Worker added";
    form.username = ""; form.fullName = ""; form.password = "";
    await loadWorkers();
  } catch (err: any) {
    addOk.value = false;
    addMsg.value = err.response?.data?.detail || "Failed to add worker";
  }
}

async function handleDeactivate(id: number) {
  try {
    await deactivateWorker(id);
    await loadWorkers();
  } catch (err: any) {
    error.value = err.response?.data?.detail || "Failed to deactivate";
  }
}

onMounted(loadWorkers);
</script>
