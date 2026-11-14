<template>
  <div v-if="loading" role="status" aria-live="polite" class="flex items-center gap-3 rounded-2xl border border-slate-200 bg-white p-4 text-slate-600 shadow-sm sm:p-5">
    <span class="size-4 animate-spin rounded-full border-2 border-slate-300 border-t-brand-600" aria-hidden="true" />
    <span class="text-sm">Loading…</span>
  </div>
  <div v-else-if="error" role="alert" class="bg-red-50 rounded-2xl border border-rose-300 p-4 shadow-sm sm:p-5">
    <p class="text-base font-semibold text-rose-800">Something went wrong</p>
    <p class="mt-1 text-sm text-rose-700">{{ error }}</p>
  </div>
  <div v-else-if="empty" class="rounded-2xl border border-slate-200 bg-white p-4 text-center shadow-sm sm:p-5">
    <p class="text-base font-semibold text-slate-800">No records found.</p>
    <p class="mt-1 text-sm text-slate-500">There is nothing to show for this view yet.</p>
  </div>
</template>

<script setup lang="ts">
defineProps<{ loading?: boolean; error?: string | null; empty?: boolean }>();
</script>
