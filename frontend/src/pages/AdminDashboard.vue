<template>
  <div class="min-h-dvh pb-12">
    <TopBar />

    <main class="mx-auto w-full max-w-6xl px-4 pt-6 sm:px-6 sm:pt-8">
      <header class="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 class="text-2xl font-semibold text-brand-700 sm:text-3xl">Daily roster</h1>
          <p class="mt-1 text-sm text-slate-500">{{ dateLabel }} · all workers and clients</p>
        </div>
        <div class="flex flex-wrap gap-2">
          <router-link
            to="/admin/workers"
            class="inline-flex items-center gap-2 rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-700 shadow-sm transition-colors hover:bg-slate-50 hover:text-slate-900"
          >
            <Users class="size-4" aria-hidden="true" />
            Manage Workers
          </router-link>
          <router-link
            to="/admin/open-shifts"
            class="inline-flex items-center gap-2 rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-700 shadow-sm transition-colors hover:bg-slate-50 hover:text-slate-900"
          >
            <CalendarDays class="size-4" aria-hidden="true" />
            Open Shifts
          </router-link>
          <router-link
            to="/admin/clients"
            class="inline-flex items-center gap-2 rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-700 shadow-sm transition-colors hover:bg-slate-50 hover:text-slate-900"
          >
            <House class="size-4" aria-hidden="true" />
            Clients
          </router-link>
          <button
            type="button"
            :disabled="reportBusy"
            class="inline-flex items-center gap-2 rounded-lg bg-accent-500 px-3 py-2 text-sm font-semibold text-white shadow-lg shadow-accent-500/25 transition-colors hover:bg-accent-600 disabled:opacity-60"
            @click="downloadReport"
          >
            <Download class="size-4" aria-hidden="true" />
            {{ reportBusy ? "Preparing…" : "Download Report" }}
          </button>
        </div>
      </header>

      <div class="mb-6 grid gap-3 sm:grid-cols-3">
        <div class="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:p-5">
          <p class="text-3xl font-semibold text-brand-700">{{ shifts.length }}</p>
          <p class="mt-1 text-sm text-slate-500">Shifts today</p>
        </div>
        <div class="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:p-5">
          <p class="text-3xl font-semibold text-brand-700">{{ completedCount }}</p>
          <p class="mt-1 text-sm text-slate-500">Completed</p>
        </div>
        <div class="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:p-5">
          <p class="text-3xl font-semibold text-brand-700">{{ activeCount }}</p>
          <p class="mt-1 text-sm text-slate-500">In progress / en route</p>
        </div>
      </div>

      <UiAlert v-if="reportError" variant="destructive" class="mb-4">{{ reportError }}</UiAlert>
      <StatePanels :loading="loading" :error="error" :empty="!loading && !error && shifts.length === 0" />

      <UiCard v-if="!loading && !error && shifts.length > 0" class="p-0 sm:p-0">
        <ul class="divide-y divide-slate-100">
          <ShiftCard v-for="shift in shifts" :key="shift.id" :shift="shift" />
        </ul>
      </UiCard>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from "vue";
import { CalendarDays, Download, House, Users } from "@lucide/vue";
import TopBar from "@/components/TopBar.vue";
import StatePanels from "@/components/StatePanels.vue";
import ShiftCard from "@/components/ShiftCard.vue";
import UiCard from "@/components/ui/UiCard.vue";
import UiAlert from "@/components/ui/UiAlert.vue";
import { getAdminAssignments, downloadDailyReport } from "@/api/services";
import type { AssignmentView } from "@/types/api";
import { todayIso } from "@/utils/siteTime";

const shifts = ref<AssignmentView[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);
const dateLabel = todayIso();
const reportBusy = ref(false);
const reportError = ref("");

const completedCount = computed(() => shifts.value.filter((s) => s.status === "COMPLETED").length);
const activeCount = computed(() =>
  shifts.value.filter((s) => s.status === "IN_PROGRESS" || s.status === "EN_ROUTE").length,
);

async function readBlobText(blob: Blob): Promise<string> {
  if (typeof blob.text === "function") return blob.text();
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result ?? ""));
    reader.onerror = () => reject(reader.error);
    reader.readAsText(blob);
  });
}

async function downloadReport() {
  reportError.value = "";
  reportBusy.value = true;
  try {
    const { blob, filename } = await downloadDailyReport();
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = filename;
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    URL.revokeObjectURL(url);
  } catch (err: any) {
    let detail = err.response?.data?.detail ?? "";
    if (!detail && err.response?.data instanceof Blob) {
      try {
        detail = JSON.parse(await readBlobText(err.response.data)).detail ?? "";
      } catch {
        detail = "";
      }
    }
    reportError.value = detail || "Could not download the report";
  } finally {
    reportBusy.value = false;
  }
}

onMounted(async () => {
  try {
    shifts.value = await getAdminAssignments();
  } catch (err: any) {
    error.value = err.response?.data?.detail || "Failed to load schedule";
  } finally {
    loading.value = false;
  }
});
</script>
