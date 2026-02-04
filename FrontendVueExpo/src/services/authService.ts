import axios from 'axios';
import type { AuthResponse } from '../types/auth';

export const loginRequest = async (username: string, password: string) => {
  const apiBase = (import.meta.env.VITE_API_URL ?? 'http://localhost:8080').replace(/\/$/, '');
  const loginUrl = `${apiBase}/api/auth/login`;
  const { data } = await axios.post<AuthResponse>(loginUrl, {
    username,
    password,
  }, {
    timeout: 10000,
  });
  return data;
};
