import axios, { AxiosError } from "axios";
import type { ProblemDetail } from "@/types/api";

const API_BASE = import.meta.env.VITE_API_BASE || "/api";

export const httpClient = axios.create({
  baseURL: API_BASE,
  headers: { "Content-Type": "application/json" },
});

httpClient.interceptors.request.use((config) => {
  const token = localStorage.getItem("rms_token");
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

httpClient.interceptors.response.use(
  (res) => res,
  (error: AxiosError<ProblemDetail>) => {
    if (error.response?.status === 401) {
      localStorage.removeItem("rms_token");
      localStorage.removeItem("rms_user");
      window.location.href = "/login";
    }
    return Promise.reject(error);
  },
);

export function getApiBase(): string {
  return API_BASE;
}
