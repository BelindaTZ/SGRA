import axios from 'axios';
import { useAuthStore } from '../stores/authStore';

const apiBase = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '');

const http = axios.create({
  baseURL: apiBase || '',
  timeout: 10000,
});

http.interceptors.request.use((config) => {
  const auth = useAuthStore();
  if (auth.token) {
    config.headers = config.headers ?? {};
    config.headers.Authorization = `Bearer ${auth.token}`;
  }
  return config;
});

export default http;
