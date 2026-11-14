import { createRouter, createWebHistory, RouteRecordRaw } from "vue-router";
import { useAuthStore } from "@/stores/auth";

const routes: RouteRecordRaw[] = [
  { path: "/login", name: "Login", component: () => import("@/pages/LoginPage.vue") },
  {
    path: "/worker",
    name: "WorkerDashboard",
    component: () => import("@/pages/WorkerDashboard.vue"),
    meta: { role: "ROLE_WORKER" },
  },
  {
    path: "/admin",
    name: "AdminDashboard",
    component: () => import("@/pages/AdminDashboard.vue"),
    meta: { role: "ROLE_ADMIN" },
  },
  {
    path: "/admin/workers",
    name: "AdminWorkers",
    component: () => import("@/pages/AdminWorkers.vue"),
    meta: { role: "ROLE_ADMIN" },
  },
  {
    path: "/admin/open-shifts",
    name: "AdminOpenShifts",
    component: () => import("@/pages/AdminOpenShifts.vue"),
    meta: { role: "ROLE_ADMIN" },
  },
  {
    path: "/admin/clients",
    name: "AdminClients",
    component: () => import("@/pages/AdminClients.vue"),
    meta: { role: "ROLE_ADMIN" },
  },
  { path: "/", redirect: "/login" },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to, _from, next) => {
  const auth = useAuthStore();
  if (to.path === "/login") {
    next();
    return;
  }
  if (!auth.isAuthenticated) {
    next("/login");
    return;
  }
  const requiredRole = to.meta.role as string | undefined;
  if (requiredRole && !auth.user?.roles.includes(requiredRole)) {
    const fallback = auth.isAdmin ? "/admin" : "/worker";
    next(fallback);
    return;
  }
  next();
});

export default router;
