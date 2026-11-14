<script setup lang="ts">
import { computed } from "vue";
import { cn } from "@/lib/utils";

// The `for` attribute is applied via attribute fall-through to the label element.
const props = defineProps<{ class?: string | null }>();

const klass = computed(() =>
  cn("text-sm font-medium leading-none text-slate-700", props.class),
);
</script>

<template>
  <label :class="klass" data-slot="label">
    <slot />
  </label>
</template>
