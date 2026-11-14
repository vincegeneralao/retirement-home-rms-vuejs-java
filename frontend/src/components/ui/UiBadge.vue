<script setup lang="ts">
import { cva, type VariantProps } from "class-variance-authority";
import { computed } from "vue";
import { cn } from "@/lib/utils";

const badgeVariants = cva(
  "inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold transition-colors",
  {
    variants: {
      variant: {
        default: "border-transparent bg-primary text-primary-foreground",
        secondary: "border-transparent bg-secondary text-secondary-foreground",
        success: "border-transparent bg-green-100 text-green-800",
        warning: "border-transparent bg-amber-100 text-amber-800",
        destructive: "border-transparent bg-red-100 text-red-800",
        outline: "text-slate-700",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  },
);

const props = defineProps<{
  variant?: NonNullable<VariantProps<typeof badgeVariants>["variant"]>;
  class?: string | null;
}>();

const klass = computed(() => badgeVariants({ variant: props.variant, class: props.class }));
</script>

<template>
  <span :class="klass" data-slot="badge">
    <slot />
  </span>
</template>
