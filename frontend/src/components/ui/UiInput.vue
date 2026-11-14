<script setup lang="ts">
import { computed } from "vue";
import { cn } from "@/lib/utils";

const props = defineProps<{
  type?: string;
  placeholder?: string;
  modelValue?: string;
  disabled?: boolean;
  class?: string | null;
}>();

const klass = computed(() =>
  cn(
    "flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors placeholder:text-muted-foreground focus-visible:outline-none focus-visible:border-brand-500 focus-visible:ring-2 focus-visible:ring-brand-200 disabled:cursor-not-allowed disabled:opacity-50",
    props.class,
  ),
);
</script>

<template>
  <input
    :type="type ?? 'text'"
    :placeholder="placeholder"
    :value="modelValue"
    :disabled="disabled"
    :class="klass"
    data-slot="input"
    @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
  />
</template>
