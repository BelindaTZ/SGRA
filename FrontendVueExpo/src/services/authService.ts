import axios from 'axios';
import type { AuthResponse } from '../types/auth';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? '',
  timeout: 10000,
});

export const loginRequest = async (username: string, password: string) => {
  const { data } = await api.post<AuthResponse>('/api/auth/login', {
    username,
    password,
  });
  return data;
};
