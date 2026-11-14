<template>
  <div class="min-h-dvh pb-12">
    <TopBar />

    <main class="mx-auto w-full max-w-6xl px-4 pt-6 sm:px-6 sm:pt-8">
      <header class="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 class="text-2xl font-semibold text-brand-700 sm:text-3xl">Open shifts</h1>
          <p class="mt-1 text-sm text-slate-500">Publish date and time slots for workers to claim, or assign a worker yourself.</p>
        </div>
        <router-link
          to="/admin"
          class="inline-flex items-center gap-2 rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-700 shadow-sm transition-colors hover:bg-slate-50 hover:text-slate-900"
        >
          Daily roster
        </router-link>
      </header>

      <div class="grid gap-6 lg:grid-cols-[minmax(0,22rem)_minmax(0,1fr)]">
        <UiCard class="h-fit">
          <h2 class="text-base font-semibold text-slate-900">Create an open shift</h2>
          <form id="open-shift-form" class="mt-4 grid gap-3" @submit.prevent="submitCreate">
            <div class="grid gap-1.5">
              <UiLabel for="os-client">Client</UiLabel>
              <select
                id="os-client"
                v-model="form.clientId"
                class="h-9 w-full rounded-md border border-input bg-transparent px-3 text-sm shadow-sm outline-none focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/30"
                required
              >
                <option value="" disabled>Select client…</option>
                <option v-for="c in clients" :key="c.id" :value="c.id">{{ c.fullName }} ({{ c.roomNumber }})</option>
              </select>
            </div>
            <div class="grid gap-1.5">
              <UiLabel for="os-date">Date</UiLabel>
              <UiInput id="os-date" type="date" v-model="form.date" required />
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div class="grid gap-1.5">
                <UiLabel for="os-start">Start time</UiLabel>
                <UiInput id="os-start" type="time" v-model="form.startTime" required />
              </div>
              <div class="grid gap-1.5">
                <UiLabel for="os-end">End time</UiLabel>
                <UiInput id="os-end" type="time" v-model="form.endTime" required />
              </div>
            </div>
            <div class="grid gap-1.5">
              <UiLabel for="os-notes">Notes (optional)</UiLabel>
              <UiInput id="os-notes" v-model="form.notes" placeholder="e.g. Evening medication round" />
            </div>
            <UiButton type="submit" :disabled="creating">
              {{ creating ? "Creating…" : "Create open shift" }}
            </UiButton>
          </form>
          <UiAlert v-if="formMessage?.kind === 'error'" variant="destructive" class="mt-4">{{ formMessage.text }}</UiAlert>
          <UiAlert v-else-if="formMessage" variant="success" class="mt-4">{{ formMessage.text }}</UiAlert>
        </UiCard>

        <section>
          <h2 class="mb-4 text-base font-semibold text-slate-900">Open and taken shifts</h2>

          <StatePanels :loading="loading" :error="error" :empty="!loading && !error && openShifts.length === 0" />

          <UiCard v-if="!loading && !error && openShifts.length > 0" class="p-0 sm:p-0">
            <ul class="divide-y divide-slate-100">
              <OpenShiftCard
                v-for="os in openShifts"
                :key="os.id"
                :shift="os"
                mode="admin"
                :workers="workers"
                @assign="assign"
                @cancel="cancel"
              />
            </ul>
          </UiCard>

          <UiAlert v-if="actionError" variant="destructive" class="mt-4">{{ actionError }}</UiAlert>
        </section>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from "vue";
import TopBar from "@/components/TopBar.vue";
import StatePanels from "@/components/StatePanels.vue";
import OpenShiftCard from "@/components/OpenShiftCard.vue";
import UiCard from "@/components/ui/UiCard.vue";
import UiInput from "@/components/ui/UiInput.vue";
import UiLabel from "@/components/ui/UiLabel.vue";
import UiButton from "@/components/ui/UiButton.vue";
import UiAlert from "@/components/ui/UiAlert.vue";
import {
  getAdminClients,
  getAdminWorkers,
  getAdminOpenShifts,
  createOpenShift,
  assignOpenShift,
  cancelOpenShift,
} from "@/api/services";
import type { ClientView, WorkerView, OpenShiftView } from "@/types/api";
import { todayIso } from "@/utils/siteTime";

const clients = ref<ClientView[]>([]);
const workers = ref<WorkerView[]>([]);
const openShifts = ref<OpenShiftView[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);
const actionError = ref<string | null>(null);
const creating = ref(false);
const formMessage = ref<{ kind: "success" | "error"; text: string } | null>(null);

const form = reactive({
  clientId: "" as string | number,
  date: todayIso(),
  startTime: "08:00",
  endTime: "16:00",
  notes: "",
});

onMounted(async () => {
  try {
    const [c, w, os] = await Promise.all([getAdminClients(), getAdminWorkers(), getAdminOpenShifts()]);
    clients.value = c.filter((x) => x.active);
    workers.value = w.filter((wk) => wk.enabled);
    openShifts.value = os;
  } catch (err: any) {
    error.value = err.response?.data?.detail || "Failed to load open shifts";
  } finally {
    loading.value = false;
  }
});

async function refreshList() {
  try {
    openShifts.value = await getAdminOpenShifts();
  } catch {
    /* keep the last known list */
  }
}

async function submitCreate() {
  creating.value = true;
  formMessage.value = null;
  try {
    await createOpenShift({
      clientId: Number(form.clientId),
      date: form.date,
      startTime: form.startTime,
      endTime: form.endTime,
      notes: form.notes || undefined,
    });
    formMessage.value = { kind: "success", text: "Open shift created" };
    await refreshList();
  } catch (err: any) {
    formMessage.value = { kind: "error", text: err.response?.data?.detail || "Could not create open shift" };
  } finally {
    creating.value = false;
  }
}

async function assign(openShiftId: number, workerId: number) {
  actionError.value = null;
  try {
    await assignOpenShift(openShiftId, workerId);
  } catch (err: any) {
    actionError.value = err.response?.data?.detail || "Could not assign worker";
  }
  await refreshList();
}

async function cancel(openShiftId: number) {
  actionError.value = null;
  try {
    await cancelOpenShift(openShiftId);
  } catch (err: any) {
    actionError.value = err.response?.data?.detail || "Could not cancel open shift";
  }
  await refreshList();
}
</script>
