<template>
  <div class="min-h-dvh pb-12">
    <TopBar />

    <main class="mx-auto w-full max-w-5xl px-4 pt-6 sm:px-6 sm:pt-8">
      <header class="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 class="text-2xl font-semibold text-brand-700 sm:text-3xl">Clients</h1>
          <p class="mt-1 text-sm text-slate-500">Add residents so open shifts and assignments can reference them.</p>
        </div>
        <router-link
          to="/admin"
          class="inline-flex items-center gap-2 rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-700 shadow-sm transition-colors hover:bg-slate-50 hover:text-slate-900"
        >
          <ArrowLeft class="size-4" aria-hidden="true" />
          Back to Dashboard
        </router-link>
      </header>

      <!-- Add client form -->
      <UiCard class="mb-6">
        <h2 class="text-lg font-semibold text-slate-900">Add a client</h2>
        <p class="mt-1 text-sm text-slate-500">Room and address are shown to workers as the shift location.</p>

        <form @submit.prevent="handleAdd" novalidate class="mt-4 grid gap-3 sm:grid-cols-2">
          <div class="sm:col-span-2">
            <UiLabel for="c-fullName" class="mb-1.5 block">Full name</UiLabel>
            <UiInput id="c-fullName" v-model="form.fullName" autocomplete="off" placeholder="Margaret Smith" required />
          </div>
          <div>
            <UiLabel for="c-roomNumber" class="mb-1.5 block">Room number</UiLabel>
            <UiInput id="c-roomNumber" v-model="form.roomNumber" autocomplete="off" placeholder="201" required />
          </div>
          <div>
            <UiLabel for="c-address" class="mb-1.5 block">Address</UiLabel>
            <UiInput id="c-address" v-model="form.address" autocomplete="off" placeholder="Elm St" required />
          </div>

          <UiAlert v-if="addMsg && !addOk" variant="destructive">{{ addMsg }}</UiAlert>
          <UiAlert v-else-if="addMsg" variant="success">{{ addMsg }}</UiAlert>

          <div class="sm:col-span-2">
            <UiButton type="submit">Add client</UiButton>
          </div>
        </form>
      </UiCard>

      <!-- Clients list -->
      <StatePanels :loading="loading" :error="error" :empty="!loading && !error && clients.length === 0" />

      <section v-if="!loading && !error && clients.length > 0" class="grid gap-3">
        <UiCard v-for="c in clients" :key="c.id">
          <div class="flex flex-wrap items-center justify-between gap-3">
            <div class="min-w-0">
              <div class="flex flex-wrap items-center gap-2">
                <p class="truncate text-base font-semibold text-slate-900">{{ c.fullName }}</p>
                <UiBadge variant="outline" class="border-brand-200 bg-brand-50 text-brand-700">Room {{ c.roomNumber }}</UiBadge>
              </div>
              <p class="truncate text-sm text-slate-500">{{ c.address }}</p>
            </div>
          </div>
        </UiCard>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ArrowLeft } from "@lucide/vue";
import TopBar from "@/components/TopBar.vue";
import StatePanels from "@/components/StatePanels.vue";
import UiButton from "@/components/ui/UiButton.vue";
import UiCard from "@/components/ui/UiCard.vue";
import UiInput from "@/components/ui/UiInput.vue";
import UiLabel from "@/components/ui/UiLabel.vue";
import UiBadge from "@/components/ui/UiBadge.vue";
import UiAlert from "@/components/ui/UiAlert.vue";
import { getAdminClients, createClient } from "@/api/services";
import type { ClientView } from "@/types/api";

const clients = ref<ClientView[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);
const form = reactive({ fullName: "", roomNumber: "", address: "" });
const addMsg = ref("");
const addOk = ref(false);

async function loadClients() {
  loading.value = true;
  error.value = null;
  try {
    clients.value = await getAdminClients();
  } catch (err: any) {
    error.value = err.response?.data?.detail || "Failed to load clients";
  } finally {
    loading.value = false;
  }
}

async function handleAdd() {
  addMsg.value = "";
  addOk.value = false;
  try {
    await createClient({ ...form });
    addOk.value = true;
    addMsg.value = "Client added";
    form.fullName = ""; form.roomNumber = ""; form.address = "";
    await loadClients();
  } catch (err: any) {
    addOk.value = false;
    addMsg.value = err.response?.data?.detail || "Failed to add client";
  }
}

onMounted(loadClients);
</script>
