<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';
import { useAuthStore } from '../../stores/authStore';
import type { Role } from '../../types/auth';

const router = useRouter();
const auth = useAuthStore();

const user = ref('');
const pass = ref('');
const message = ref('');
const error = ref('');
const loading = ref(false);

const redirectByRole: Record<Role, string> = {
  ADMIN: '/dashboard/en-construccion',
  COORDINATOR: '/dashboard/en-construccion',
  TEACHER: '/dashboard/docente',
  STUDENT: '/dashboard/estudiante',
};

const onSubmit = async () => {
  message.value = '';
  error.value = '';

  if (!user.value.trim() || !pass.value.trim()) {
    error.value = 'Por favor, completa todos los campos';
    return;
  }

  loading.value = true;
  try {
    const response = await auth.login(user.value.trim(), pass.value);
    message.value = 'Ingreso exitoso. Redirigiendo...';
    await router.push(redirectByRole[response.role] ?? '/dashboard/en-construccion');
  } catch (err) {
    if (axios.isAxiosError(err)) {
      if (err.response?.status === 401) {
        error.value = 'Usuario o contraseña incorrectos';
      } else if (err.response?.status === 403) {
        error.value = (err.response?.data as { message?: string })?.message ??
          'Cuenta inactiva. Contacta al administrador.';
      } else {
        error.value = 'No se pudo conectar con el servidor. Inténtalo más tarde.';
      }
    } else {
      error.value = 'No se pudo conectar con el servidor. Inténtalo más tarde.';
    }
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <div class="login-page d-flex align-items-center justify-content-center min-vh-100 bg-light">
    <div class="login-card card shadow-sm p-4">
      <div class="text-center mb-3">
        <img src="/SGRAicono.ico" alt="SGRA" width="64" height="64" class="mb-2" />
        <h1 class="h4 mb-1">Sistema de Gestión de Refuerzo Académico</h1>
        <p class="text-muted">UTEQ</p>
      </div>

      <form @submit.prevent="onSubmit">
        <div class="mb-3">
          <label class="form-label" for="username">Usuario</label>
          <input
            id="username"
            v-model="user"
            type="text"
            class="form-control"
            autocomplete="username"
            placeholder="Ingresa tu usuario"
          />
        </div>
        <div class="mb-3">
          <label class="form-label" for="password">Contraseña</label>
          <input
            id="password"
            v-model="pass"
            type="password"
            class="form-control"
            autocomplete="current-password"
            placeholder="Ingresa tu contraseña"
          />
        </div>

        <div v-if="error" class="alert alert-danger" role="alert">
          {{ error }}
        </div>
        <div v-if="message" class="alert alert-success" role="alert">
          {{ message }}
        </div>

        <button class="btn btn-primary w-100" type="submit" :disabled="loading">
          <span v-if="loading" class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>
          Ingresar
        </button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.login-card {
  max-width: 420px;
  width: 100%;
  border-radius: 16px;
}
</style>
