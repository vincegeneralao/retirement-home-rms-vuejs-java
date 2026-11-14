<script setup lang="ts">
import { computed, ref } from "vue";
import { KeyRound, TriangleAlert } from "@lucide/vue";
import UiButton from "@/components/ui/UiButton.vue";
import UiInput from "@/components/ui/UiInput.vue";
import UiLabel from "@/components/ui/UiLabel.vue";
import UiAlert from "@/components/ui/UiAlert.vue";
import { useAuthStore } from "@/stores/auth";

/**
 * Shown instead of the worker dashboard when an admin has reset their password to blank.
 * The worker either picks a real password or explicitly keeps the blank one — keeping it is
 * allowed but discouraged, so that path asks for a second confirmation.
 */
const emit = defineEmits<{ resolved: [] }>();

const auth = useAuthStore();

const currentPassword = ref("");
const newPassword = ref("");
const confirmPassword = ref("");
const busy = ref(false);
const error = ref<string | null>(null);
const confirmKeep = ref(false);

const canSubmit = computed(
  () => !busy.value && newPassword.value.length >= 6 && newPassword.value === confirmPassword.value,
);

const mismatch = computed(
  () => confirmPassword.value.length > 0 && newPassword.value !== confirmPassword.value,
);

function message(err: any, fallback: string): string {
  return err?.response?.data?.detail || err?.response?.data?.errors?.newPassword || fallback;
}

async function handleChange() {
  if (!canSubmit.value) return;
  busy.value = true;
  error.value = null;
  try {
    await auth.changePassword(currentPassword.value, newPassword.value);
    emit("resolved");
  } catch (err: any) {
    error.value = message(err, "Could not change your password");
  } finally {
    busy.value = false;
  }
}

async function handleKeep() {
  if (!confirmKeep.value) {
    confirmKeep.value = true;
    error.value = null;
    return;
  }
  busy.value = true;
  error.value = null;
  try {
    await auth.keepResetPassword();
    emit("resolved");
  } catch (err: any) {
    // A stale local flag means the reset was already dealt with elsewhere, so let the worker
    // through instead of trapping them behind the modal.
    if (err?.response?.data?.code === "PASSWORD_RESET_NOT_PENDING") {
      auth.clearPasswordResetFlag();
      emit("resolved");
      return;
    }
    error.value = message(err, "Could not confirm your password choice");
    confirmKeep.value = false;
  } finally {
    busy.value = false;
  }
}
</script>

<template>
  <div
    class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4"
    role="dialog"
    aria-modal="true"
    aria-labelledby="reset-title"
  >
    <div class="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl">
      <div class="flex items-start gap-3">
        <span class="rounded-full bg-brand-50 p-2 text-brand-700" aria-hidden="true">
          <KeyRound class="size-5" />
        </span>
        <div>
          <h2 id="reset-title" class="text-lg font-semibold text-slate-900">Your password was reset</h2>
          <p class="mt-1 text-sm text-slate-500">
            An administrator set your password to blank. Choose a new one, or keep the blank password.
          </p>
        </div>
      </div>

      <form class="mt-5 grid gap-3" @submit.prevent="handleChange" novalidate>
        <div>
          <UiLabel for="cp-current" class="mb-1.5 block">Current password</UiLabel>
          <UiInput
            id="cp-current"
            v-model="currentPassword"
            type="password"
            autocomplete="current-password"
            placeholder="Leave blank if you have not changed it"
          />
        </div>
        <div>
          <UiLabel for="cp-new" class="mb-1.5 block">New password</UiLabel>
          <UiInput
            id="cp-new"
            v-model="newPassword"
            type="password"
            autocomplete="new-password"
            placeholder="At least 6 characters"
          />
        </div>
        <div>
          <UiLabel for="cp-confirm" class="mb-1.5 block">Confirm new password</UiLabel>
          <UiInput
            id="cp-confirm"
            v-model="confirmPassword"
            type="password"
            autocomplete="new-password"
            placeholder="Repeat the new password"
          />
          <p v-if="mismatch" class="mt-1 text-sm text-red-600">The passwords do not match.</p>
        </div>

        <UiAlert v-if="error" variant="destructive">{{ error }}</UiAlert>
        <UiAlert v-if="confirmKeep" variant="destructive">
          <span class="flex items-start gap-2">
            <TriangleAlert class="mt-0.5 size-4 shrink-0" aria-hidden="true" />
            <span>
              Keeping a blank password is not best practice. Anyone who knows your username could sign in
              as you. Click "Keep blank password" again to confirm.
            </span>
          </span>
        </UiAlert>

        <div class="mt-1 flex flex-wrap items-center justify-end gap-2">
          <UiButton type="button" variant="outline" :disabled="busy" @click="handleKeep">
            {{ confirmKeep ? "Keep blank password" : "Keep" }}
          </UiButton>
          <UiButton type="submit" :disabled="!canSubmit">
            {{ busy ? "Working…" : "Change password" }}
          </UiButton>
        </div>
      </form>
    </div>
  </div>
</template>
