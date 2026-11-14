import { describe, it, expect, vi, beforeEach } from "vitest";
import { createPinia, setActivePinia } from "pinia";
import { createRouter, createMemoryHistory } from "vue-router";
import { useAuthStore } from "../stores/auth";

function makeRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: "/login", name: "Login", component: { template: "<div>Login</div>" } },
      { path: "/worker", name: "Worker", component: { template: "<div>W</div>" }, meta: { role: "ROLE_WORKER" } },
      { path: "/admin", name: "Admin", component: { template: "<div>A</div>" }, meta: { role: "ROLE_ADMIN" } },
      { path: "/", redirect: "/login" },
    ],
  });
}

function addGuard(router: ReturnType<typeof makeRouter>) {
  router.beforeEach((to, _from, next) => {
    const auth = useAuthStore();
    if (to.path === "/login") { next(); return; }
    if (!auth.isAuthenticated) { next("/login"); return; }
    const requiredRole = to.meta.role as string | undefined;
    if (requiredRole && !auth.user?.roles.includes(requiredRole)) {
      const fallback = auth.isAdmin ? "/admin" : "/worker";
      next(fallback);
      return;
    }
    next();
  });
}

describe("Router guard", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    localStorage.clear();
  });

  it("redirects unauthenticated to /login", async () => {
    const router = makeRouter();
    addGuard(router);
    await router.push("/worker");
    expect(router.currentRoute.value.path).toBe("/login");
  });

  it("allows authenticated user with correct role", async () => {
    const pinia = createPinia();
    setActivePinia(pinia);
    const auth = useAuthStore();
    auth.token = "tok";
    auth.user = { userId: 1, username: "admin", fullName: "A", roles: ["ROLE_ADMIN"] };

    const router = makeRouter();
    addGuard(router);
    await router.push("/admin");
    expect(router.currentRoute.value.path).toBe("/admin");
  });

  it("redirects admin trying to access worker route", async () => {
    const pinia = createPinia();
    setActivePinia(pinia);
    const auth = useAuthStore();
    auth.token = "tok";
    auth.user = { userId: 1, username: "admin", fullName: "A", roles: ["ROLE_ADMIN"] };

    const router = makeRouter();
    addGuard(router);
    await router.push("/worker");
    expect(router.currentRoute.value.path).toBe("/admin");
  });

  it("allows /login without auth", async () => {
    const router = makeRouter();
    addGuard(router);
    await router.push("/login");
    expect(router.currentRoute.value.path).toBe("/login");
  });
});
