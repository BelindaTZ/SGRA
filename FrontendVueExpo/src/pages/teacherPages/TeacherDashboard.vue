<script setup lang="ts">
import { useRouter } from 'vue-router';
import { useAuthStore } from '../../stores/authStore';

const router = useRouter();
const auth = useAuthStore();

const handleLogout = async () => {
  auth.logout();
  await router.push('/login');
};
</script>

<template>
  <div class="container py-5">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <div>
        <h1 class="h3 mb-1">Panel Docente</h1>
        <p class="text-muted mb-0">Bienvenido, {{ auth.username ?? 'Docente' }}</p>
      </div>
      <button class="btn btn-outline-secondary" @click="handleLogout">
        Cerrar sesión
      </button>
    </div>

    <div class="card shadow-sm">
      <div class="card-body">
        <h2 class="h5">Módulos disponibles</h2>
        <p class="text-muted mb-0">Sección en construcción. Próximamente más funcionalidades.</p>
      </div>
    </div>
  </div>
</template>
