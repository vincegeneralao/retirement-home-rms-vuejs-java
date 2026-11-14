<template>
  <div class="relative" @keydown.esc="open = false">
    <button
      type="button"
      :disabled="busy"
      aria-label="Shift status"
      aria-haspopup="menu"
      :aria-expanded="open"
      class="flex w-36 items-center justify-between gap-2 rounded-lg border border-slate-300 bg-white px-2.5 py-1.5 text-xs font-medium text-slate-700 shadow-sm transition-colors hover:border-brand-400 focus:border-brand-500 focus:outline-none disabled:opacity-60"
      @click="open = !open"
    >
      <span class="flex min-w-0 items-center gap-1.5">
        <component :is="STATUS_ICONS[status]" class="size-3.5 shrink-0" :class="STATUS_ICON_TONES[status]" />
        <span class="truncate">{{ STATUS_LABELS[status] }}</span>
      </span>
      <ChevronDown class="size-3.5 shrink-0 text-slate-400 transition-transform" :class="{ 'rotate-180': open }" />
    </button>

    <template v-if="open">
      <div class="fixed inset-0 z-10" aria-hidden="true" @click="open = false"></div>
      <div
        class="absolute right-0 z-20 mt-1 w-40 rounded-xl border border-slate-200 bg-white p-1 shadow-lg"
        role="menu"
      >
        <button
          v-for="option in WORKER_STATUS_OPTIONS"
          :key="option"
          type="button"
          role="menuitem"
          :disabled="option === status"
          class="flex w-full items-center gap-2 rounded-lg px-2 py-1.5 text-left text-xs font-medium text-slate-700 transition-colors hover:bg-slate-100 disabled:cursor-default disabled:opacity-40"
          @click="pick(option)"
        >
          <component :is="STATUS_ICONS[option]" class="size-3.5 shrink-0" :class="STATUS_ICON_TONES[option]" />
          {{ STATUS_LABELS[option] }}
        </button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { ChevronDown } from "@lucide/vue";
import type { AssignmentStatus } from "@/types/api";
import { STATUS_ICONS, STATUS_ICON_TONES, STATUS_LABELS, WORKER_STATUS_OPTIONS } from "@/utils/statusMeta";

const props = defineProps<{ status: AssignmentStatus; busy?: boolean }>();
const emit = defineEmits<{ change: [status: AssignmentStatus] }>();

const open = ref(false);

function pick(status: AssignmentStatus) {
  open.value = false;
  if (status !== props.status) emit("change", status);
}
</script>
