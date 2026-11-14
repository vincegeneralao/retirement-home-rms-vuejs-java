<script setup lang="ts">
import { cva, type VariantProps } from "class-variance-authority";
import { computed } from "vue";
import { cn } from "@/lib/utils";

const alertVariants = cva(
  "relative w-full rounded-xl border px-4 py-3 text-sm [&>svg]:absolute [&>svg]:left-4 [&>svg]:top-4 [&>svg~*]:pl-7",
  {
    variants: {
      variant: {
        default: "bg-card text-card-foreground",
        destructive: "border-red-300 bg-red-50 text-red-700",
        success: "border-green-300 bg-green-50 text-green-700",
        info: "border-brand-200 bg-brand-50 text-brand-700",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  },
);

const props = defineProps<{
  variant?: NonNullable<VariantProps<typeof alertVariants>["variant"]>;
  class?: string | null;
}>();

const klass = computed(() => alertVariants({ variant: props.variant, class: props.class }));
</script>

<template>
  <div role="alert" :class="klass" data-slot="alert">
    <slot />
  </div>
</template>
