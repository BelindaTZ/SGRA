import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { AuthResponse, Role } from '../types/auth';
import { loginRequest } from '../services/authService';

const STORAGE_KEYS = {
  token: 'sgra_token',
  role: 'sgra_role',
  userId: 'sgra_user_id',
  username: 'sgra_username',
};

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(STORAGE_KEYS.token));
  const role = ref<Role | null>(
    (localStorage.getItem('sgra_active_role') || localStorage.getItem(STORAGE_KEYS.role)) as Role | null
  );
  const userId = ref<number | null>(
    localStorage.getItem(STORAGE_KEYS.userId)
      ? Number(localStorage.getItem(STORAGE_KEYS.userId))
      : null
  );
  const username = ref<string | null>(localStorage.getItem(STORAGE_KEYS.username));

  const isAuthenticated = computed(() => Boolean(token.value));

  const setSession = (data: AuthResponse) => {
    token.value = data.token;
    role.value = data.role;
    userId.value = data.userId;
    username.value = data.username;

    localStorage.setItem(STORAGE_KEYS.token, data.token);
    localStorage.setItem(STORAGE_KEYS.role, data.role);
    localStorage.setItem(STORAGE_KEYS.userId, String(data.userId));
    localStorage.setItem(STORAGE_KEYS.username, data.username);
  };

  const login = async (user: string, pass: string) => {
    const response = await loginRequest(user, pass);
    if (response?.token) {
      setSession(response);
    }
    return response;
  };

  const logout = () => {
    token.value = null;
    role.value = null;
    userId.value = null;
    username.value = null;

    localStorage.removeItem(STORAGE_KEYS.token);
    localStorage.removeItem(STORAGE_KEYS.role);
    localStorage.removeItem(STORAGE_KEYS.userId);
    localStorage.removeItem(STORAGE_KEYS.username);
  };

  return {
    token,
    role,
    userId,
    username,
    isAuthenticated,
    login,
    logout,
    setSession,
  };
});
