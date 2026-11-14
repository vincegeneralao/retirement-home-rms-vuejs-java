<template>
  <li class="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-3 shadow-sm sm:flex-nowrap">
    <div class="flex min-w-0 flex-1 items-center gap-4">
      <span class="w-28 shrink-0 text-sm font-semibold tabular-nums text-brand-700">
        <span class="block text-xs font-medium text-slate-400">{{ dateLabel(shift.start) }}</span>
        {{ timeLabel(shift.start) }}–{{ timeLabel(shift.end) }}
      </span>
      <div class="min-w-0 flex-1">
        <p class="truncate text-sm font-semibold text-slate-900">{{ shift.clientName }}</p>
        <p class="truncate text-sm text-slate-500">{{ shift.location }}</p>
        <p v-if="shift.notes" class="mt-0.5 truncate text-xs text-slate-400 italic">{{ shift.notes }}</p>
      </div>
    </div>

    <div class="flex flex-wrap items-center gap-2">
      <UiBadge :variant="shift.status === 'OPEN' ? 'success' : 'secondary'">
        {{ shift.status === "OPEN" ? "Open" : "Taken" }}
      </UiBadge>

      <template v-if="mode === 'worker' && shift.status === 'OPEN'">
        <UiButton size="sm" :disabled="busy" @click="$emit('take', shift.id)">
          {{ busy ? "Taking…" : "Take shift" }}
        </UiButton>
      </template>

      <template v-else-if="mode === 'admin' && shift.status === 'OPEN'">
        <select
          v-model="selectedWorkerId"
          class="h-8 rounded-md border border-input bg-transparent px-2 text-sm text-slate-700"
          aria-label="Worker to assign"
        >
          <option value="" disabled>Assign worker…</option>
          <option v-for="wk in workers" :key="wk.id" :value="wk.id">{{ wk.fullName }}</option>
        </select>
        <UiButton size="sm" variant="outline" :disabled="!selectedWorkerId" @click="onAssign">Assign</UiButton>
        <UiButton size="sm" variant="ghost" @click="$emit('cancel', shift.id)">Cancel</UiButton>
      </template>

      <span v-if="shift.status === 'TAKEN'" class="text-xs text-slate-500">by {{ shift.takenByName }}</span>
    </div>
  </li>
</template>

<script setup lang="ts">
import { ref } from "vue";
import type { OpenShiftView, WorkerView } from "@/types/api";
import { dateLabel, timeLabel } from "@/utils/siteTime";
import UiBadge from "./ui/UiBadge.vue";
import UiButton from "./ui/UiButton.vue";

const props = defineProps<{
  shift: OpenShiftView;
  mode: "worker" | "admin";
  workers?: WorkerView[];
  busy?: boolean;
}>();

const emit = defineEmits<{
  (e: "take", openShiftId: number): void;
  (e: "assign", openShiftId: number, workerId: number): void;
  (e: "cancel", openShiftId: number): void;
}>();

const selectedWorkerId = ref<string>("");

function onAssign() {
  if (!selectedWorkerId.value) return;
  emit("assign", props.shift.id, Number(selectedWorkerId.value));
  selectedWorkerId.value = "";
}
</script>
