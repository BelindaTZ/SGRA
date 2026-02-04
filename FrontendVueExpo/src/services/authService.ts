import axios from 'axios';
import type { AuthResponse } from '../types/auth';

export const loginRequest = async (username: string, password: string) => {
  const apiBase = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '');
  const loginUrl = apiBase ? `${apiBase}/api/auth/login` : '/api/auth/login';
  const { data } = await axios.post<AuthResponse>(loginUrl, {
    username,
    password,
  }, {
    timeout: 10000,
  });
  return data;
};
