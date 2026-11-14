<template>
  <div class="min-h-dvh pb-12">
    <TopBar />

    <main class="mx-auto w-full max-w-5xl px-4 pt-6 sm:px-6 sm:pt-8">
      <header class="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 class="text-2xl font-semibold text-brand-700 sm:text-3xl">My shifts</h1>
          <p class="mt-1 text-sm text-slate-500">{{ dateLabel }} · your roster for today</p>
        </div>
      </header>

      <UiAlert v-if="statusError" variant="destructive" class="mb-4">{{ statusError }}</UiAlert>

      <StatePanels :loading="loading" :error="error" :empty="!loading && !error && shifts.length === 0" />

      <UiCard v-if="!loading && !error && shifts.length > 0" class="p-0 sm:p-0">
        <ul class="divide-y divide-slate-100">
          <ShiftCard
            v-for="shift in shifts"
            :key="shift.id"
            :shift="shift"
            editable
            :busy="statusBusyId === shift.id"
            @status-change="setStatus"
          />
        </ul>
      </UiCard>

      <section class="mt-8">
        <header class="mb-4">
          <h2 class="text-xl font-semibold text-brand-700">Upcoming shifts</h2>
          <p class="mt-1 text-sm text-slate-500">Future shifts on your roster, including shifts you have claimed.</p>
        </header>

        <div v-if="loading" class="rounded-2xl border border-slate-200 bg-white p-4 text-sm text-slate-500 shadow-sm sm:p-5">
          Loading upcoming shifts…
        </div>
        <p v-else-if="upcoming.length === 0" class="rounded-2xl border border-slate-200 bg-white p-4 text-sm text-slate-500 shadow-sm sm:p-5">
          No upcoming shifts.
        </p>
        <UiCard v-else class="p-0 sm:p-0">
          <ul class="divide-y divide-slate-100">
            <ShiftCard
              v-for="shift in upcoming"
              :key="`up-${shift.id}`"
              :shift="shift"
              show-date
              editable
              :busy="statusBusyId === shift.id"
              @status-change="setStatus"
            />
          </ul>
        </UiCard>
      </section>

      <section class="mt-8">
        <header class="mb-4">
          <h2 class="text-xl font-semibold text-brand-700">Open shifts you can take</h2>
          <p class="mt-1 text-sm text-slate-500">Claim an open shift and it is added to your roster. First come, first served.</p>
        </header>

        <UiAlert v-if="claimError" variant="destructive" class="mb-4">{{ claimError }}</UiAlert>

        <div v-if="loading" class="rounded-2xl border border-slate-200 bg-white p-4 text-sm text-slate-500 shadow-sm sm:p-5">
          Loading open shifts…
        </div>
        <p v-else-if="openShifts.length === 0" class="rounded-2xl border border-slate-200 bg-white p-4 text-sm text-slate-500 shadow-sm sm:p-5">
          No open shifts right now.
        </p>
        <UiCard v-else class="p-0 sm:p-0">
          <ul class="divide-y divide-slate-100">
            <OpenShiftCard
              v-for="os in openShifts"
              :key="os.id"
              :shift="os"
              mode="worker"
              :busy="claimingId === os.id"
              @take="claim"
            />
          </ul>
        </UiCard>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import TopBar from "@/components/TopBar.vue";
import StatePanels from "@/components/StatePanels.vue";
import ShiftCard from "@/components/ShiftCard.vue";
import OpenShiftCard from "@/components/OpenShiftCard.vue";
import UiCard from "@/components/ui/UiCard.vue";
import UiAlert from "@/components/ui/UiAlert.vue";
import { getWorkerAssignments, getWorkerUpcoming, getOpenShifts, claimOpenShift, updateWorkerAssignmentStatus } from "@/api/services";
import type { AssignmentView, OpenShiftView } from "@/types/api";
import { todayIso } from "@/utils/siteTime";

const shifts = ref<AssignmentView[]>([]);
const upcoming = ref<AssignmentView[]>([]);
const openShifts = ref<OpenShiftView[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);
const claimError = ref<string | null>(null);
const claimingId = ref<number | null>(null);
const statusError = ref<string | null>(null);
const statusBusyId = ref<number | null>(null);
const dateLabel = todayIso();

async function loadAll() {
  const [mine, future, open] = await Promise.all([getWorkerAssignments(), getWorkerUpcoming(), getOpenShifts()]);
  shifts.value = mine;
  upcoming.value = future;
  openShifts.value = open;
}

onMounted(async () => {
  try {
    await loadAll();
  } catch (err: any) {
    error.value = err.response?.data?.detail || "Failed to load shifts";
  } finally {
    loading.value = false;
  }
});

async function claim(openShiftId: number) {
  claimingId.value = openShiftId;
  claimError.value = null;
  try {
    await claimOpenShift(openShiftId);
  } catch (err: any) {
    claimError.value = err.response?.data?.detail || "This shift is no longer available";
  } finally {
    claimingId.value = null;
  }
  try {
    await loadAll();
  } catch {
    /* keep the last known lists if refresh fails */
  }
}

async function setStatus(shift: AssignmentView, status: string) {
  statusBusyId.value = shift.id;
  statusError.value = null;
  try {
    await updateWorkerAssignmentStatus(shift.id, status);
  } catch (err: any) {
    statusError.value = err.response?.data?.detail || "Could not update the shift status";
  } finally {
    statusBusyId.value = null;
  }
  try {
    await loadAll();
  } catch {
    /* keep the last known lists if refresh fails */
  }
}
</script>
