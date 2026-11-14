<template>
  <li class="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-3 shadow-sm sm:flex-nowrap">
    <div class="flex min-w-0 flex-1 items-center gap-4">
      <span class="w-24 shrink-0 text-sm font-semibold tabular-nums text-brand-700" :class="{ 'w-28': showDate }">
        <span v-if="showDate" class="block text-xs font-medium text-slate-400">{{ dateLabel(shift.start) }}</span>
        {{ timeLabel(shift.start) }}–{{ timeLabel(shift.end) }}
      </span>
      <div class="min-w-0 flex-1">
        <p class="truncate text-sm font-semibold text-slate-900">{{ shift.clientName }}</p>
        <p class="truncate text-sm text-slate-500">{{ shift.location }} · {{ shift.workerName }}</p>
        <p v-if="shift.notes" class="mt-0.5 truncate text-xs text-slate-400 italic">{{ shift.notes }}</p>
      </div>
    </div>
    <StatusSelect
      v-if="editable && editableStatus"
      :status="shift.status"
      :busy="busy"
      @change="onStatusChange"
    />
    <StatusBadge v-else :status="shift.status" />
  </li>
</template>

<script setup lang="ts">
import { computed } from "vue";
import type { AssignmentView } from "@/types/api";
import { dateLabel, timeLabel } from "@/utils/siteTime";
import { WORKER_STATUS_OPTIONS } from "@/utils/statusMeta";
import StatusBadge from "./StatusBadge.vue";
import StatusSelect from "./StatusSelect.vue";

const props = defineProps<{ shift: AssignmentView; showDate?: boolean; editable?: boolean; busy?: boolean }>();
const emit = defineEmits<{ "status-change": [shift: AssignmentView, status: string] }>();

const editableStatus = computed(() => WORKER_STATUS_OPTIONS.includes(props.shift.status));

function onStatusChange(status: string) {
  emit("status-change", props.shift, status);
}
</script>
