<template>
  <span
    class="inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold ring-1 ring-inset"
    :class="tone"
    data-slot="status-badge"
  >
    <component :is="STATUS_ICONS[status]" class="mr-1 size-3.5" />
    {{ label }}
  </span>
</template>

<script setup lang="ts">
import { computed } from "vue";
import type { AssignmentStatus } from "@/types/api";
import { STATUS_ICONS, STATUS_LABELS, STATUS_TONES } from "@/utils/statusMeta";

const props = defineProps<{ status: AssignmentStatus }>();

const label = computed(() => STATUS_LABELS[props.status]);
const tone = computed(() => STATUS_TONES[props.status]);
</script>
