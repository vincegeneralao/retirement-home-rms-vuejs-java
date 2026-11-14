<script setup lang="ts">
import { computed } from "vue";
import { cn } from "@/lib/utils";

const props = defineProps<{ class?: string | null }>();

const klass = computed(() =>
  cn("rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:p-5", props.class),
);
</script>

<template>
  <div :class="klass" data-slot="card">
    <slot />
  </div>
</template>
