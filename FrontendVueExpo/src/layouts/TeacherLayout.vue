<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/authStore';

const router = useRouter();
const auth = useAuthStore();

const isSidebarCollapsed = ref(false);
const isRoleMenuOpen = ref(false);

const availableRoles = computed(() => (auth.role ? [auth.role] : []));
const activeRole = computed(() => auth.role ?? '');

const toggleSidebar = () => {
  isSidebarCollapsed.value = !isSidebarCollapsed.value;
};

const toggleRoleMenu = () => {
  isRoleMenuOpen.value = !isRoleMenuOpen.value;
};

const selectRole = async (role: string) => {
  localStorage.setItem('sgra_active_role', role);
  isRoleMenuOpen.value = false;
  await router.push('/dashboard/docente');
};

const handleLogout = async () => {
  auth.logout();
  await router.push('/login');
};

const formatRole = (role: string) => {
  if (!role) return 'Docente';
  return role.charAt(0).toUpperCase() + role.slice(1).toLowerCase();
};
</script>

<template>
  <div class="teacher-shell" :class="{ 'is-collapsed': isSidebarCollapsed }">
    <aside class="teacher-sidebar">
      <div class="sidebar-brand">
        <div class="brand-logo">
          <img src="/SGRAicono.ico" alt="SGRA" />
        </div>
        <div class="brand-text">
          <span class="brand-title">SGRA</span>
          <span class="brand-subtitle">UTEQ</span>
        </div>
      </div>

      <nav class="sidebar-nav">
        <RouterLink
          to="/dashboard/docente"
          class="nav-item"
          title="Dashboard"
          exact-active-class="active"
        >
          <i class="bi bi-speedometer2"></i>
          <span class="nav-label">Dashboard</span>
        </RouterLink>
        <RouterLink
          to="/dashboard/docente/solicitudes"
          class="nav-item"
          title="Gestión de Solicitudes"
          active-class="active"
        >
          <i class="bi bi-list-check"></i>
          <span class="nav-label">Gestión de Solicitudes</span>
        </RouterLink>
        <RouterLink
          to="/dashboard/docente/disponibilidad"
          class="nav-item"
          title="Gestión de Disponibilidad"
          active-class="active"
        >
          <i class="bi bi-calendar2-check"></i>
          <span class="nav-label">Gestión de Disponibilidad</span>
        </RouterLink>
        <RouterLink
          to="/dashboard/docente/sesiones"
          class="nav-item"
          title="Sesiones"
          active-class="active"
        >
          <i class="bi bi-collection"></i>
          <span class="nav-label">Sesiones</span>
        </RouterLink>
        <RouterLink
          to="/dashboard/docente/reportes"
          class="nav-item"
          title="Reportes"
          active-class="active"
        >
          <i class="bi bi-bar-chart"></i>
          <span class="nav-label">Reportes</span>
        </RouterLink>
        <RouterLink
          to="/dashboard/docente/preferencias"
          class="nav-item"
          title="Preferencias"
          active-class="active"
        >
          <i class="bi bi-sliders2"></i>
          <span class="nav-label">Preferencias</span>
        </RouterLink>
      </nav>

      <button class="sidebar-logout" type="button" title="Cerrar sesión" @click="handleLogout">
        <i class="bi bi-box-arrow-right"></i>
        <span class="nav-label">Cerrar sesión</span>
      </button>
    </aside>

    <section class="teacher-main">
      <header class="teacher-topbar">
        <div class="topbar-title">
          <button
            class="menu-toggle"
            type="button"
            aria-label="Menu"
            :aria-expanded="!isSidebarCollapsed"
            @click="toggleSidebar"
          >
            <i class="bi bi-list"></i>
          </button>
        </div>
        <div class="topbar-actions">
          <button class="icon-button" type="button" aria-label="Notificaciones">
            <svg viewBox="0 0 24 24" class="icon-svg" aria-hidden="true">
              <path
                d="M12 2a4 4 0 0 0-4 4v1.2a6.5 6.5 0 0 0-3 5.6v3.5l-1.4 2a1 1 0 0 0 .8 1.6h15.2a1 1 0 0 0 .8-1.6l-1.4-2V12.8a6.5 6.5 0 0 0-3-5.6V6a4 4 0 0 0-4-4Zm0 20a2.5 2.5 0 0 0 2.4-2h-4.8A2.5 2.5 0 0 0 12 22Z"
              />
            </svg>
          </button>
          <div class="role-selector" :class="{ open: isRoleMenuOpen }">
            <button class="role-button" type="button" @click="toggleRoleMenu">
              <svg viewBox="0 0 24 24" class="icon-svg" aria-hidden="true">
                <path d="M12 12a4 4 0 1 0-4-4 4 4 0 0 0 4 4Zm0 2c-4.4 0-8 2.2-8 5v1h16v-1c0-2.8-3.6-5-8-5Z" />
              </svg>
              <span class="role-label">Rol: {{ formatRole(activeRole) }}</span>
              <svg viewBox="0 0 24 24" class="icon-svg" aria-hidden="true">
                <path d="M7 10l5 5 5-5z" />
              </svg>
            </button>
            <div v-if="isRoleMenuOpen" class="role-menu">
              <button
                v-for="role in availableRoles"
                :key="role"
                class="role-option"
                type="button"
                @click="selectRole(role)"
              >
                {{ formatRole(role) }}
              </button>
            </div>
          </div>
        </div>
      </header>

      <main class="teacher-content">
        <RouterView />
      </main>
    </section>
  </div>
</template>

<style scoped>
.teacher-shell {
  display: grid;
  grid-template-columns: 240px 1fr;
  min-height: 100vh;
  background: #f4f7f5;
  color: #1f2b24;
}

.teacher-shell.is-collapsed {
  grid-template-columns: 72px 1fr;
}

.teacher-sidebar {
  background: linear-gradient(180deg, #0f5b3b 0%, #0b4028 100%);
  color: #fff;
  padding: 20px 18px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-logo {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(255, 255, 255, 0.35);
  flex: 0 0 44px;
  overflow: hidden;
}

.brand-logo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.brand-text {
  display: flex;
  flex-direction: column;
}

.teacher-shell.is-collapsed .brand-text {
  display: none;
}

.teacher-shell.is-collapsed .sidebar-brand {
  justify-content: center;
}

.brand-title {
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.brand-subtitle {
  font-size: 12px;
  opacity: 0.9;
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 10px;
  color: #e7f5ec;
  text-decoration: none;
  font-weight: 500;
  transition: background 0.2s ease;
  font-size: 14px;
}

.nav-label {
  white-space: nowrap;
}

.teacher-shell.is-collapsed .nav-item {
  justify-content: center;
  padding: 10px;
}

.teacher-shell.is-collapsed .nav-label {
  display: none;
}

.nav-item i {
  font-size: 16px;
}

.nav-item:hover,
.nav-item.active {
  background: rgba(255, 255, 255, 0.22);
  color: #fff;
}

.sidebar-logout {
  border: 1px solid rgba(255, 255, 255, 0.4);
  background: transparent;
  color: #fff;
  padding: 8px 14px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 13px;
}

.teacher-shell.is-collapsed .sidebar-logout {
  justify-content: center;
  padding: 10px;
}

.teacher-main {
  display: flex;
  flex-direction: column;
}

.teacher-topbar {
  background: #0f5b3b;
  color: #fff;
  padding: 10px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.12);
}

.topbar-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.menu-toggle {
  border: 1px solid rgba(255, 255, 255, 0.4);
  background: rgba(255, 255, 255, 0.18);
  color: #fff;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  font-size: 24px;
}

.menu-toggle:hover {
  background: rgba(255, 255, 255, 0.28);
}

.menu-toggle:focus-visible {
  outline: 2px solid rgba(255, 255, 255, 0.7);
  outline-offset: 2px;
}

.topbar-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.icon-button {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.35);
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 15px;
}

.role-selector {
  position: relative;
}

.role-button {
  display: flex;
  align-items: center;
  gap: 6px;
  border: 1px solid rgba(255, 255, 255, 0.35);
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  padding: 6px 10px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
}

.role-menu {
  position: absolute;
  right: 0;
  top: calc(100% + 6px);
  background: #fff;
  border-radius: 10px;
  padding: 6px;
  box-shadow: 0 10px 20px rgba(15, 23, 42, 0.12);
  min-width: 180px;
  z-index: 10;
}

.role-option {
  width: 100%;
  text-align: left;
  border: none;
  background: transparent;
  padding: 8px 10px;
  border-radius: 8px;
  font-size: 13px;
  color: #1f2b24;
}

.role-option:hover {
  background: #f4f7f5;
}

.icon-svg {
  width: 16px;
  height: 16px;
  fill: currentColor;
}

.teacher-content {
  padding: 22px;
}

@media (max-width: 992px) {
  .teacher-shell {
    grid-template-columns: 72px 1fr;
  }

  .teacher-shell:not(.is-collapsed) {
    grid-template-columns: 240px 1fr;
  }

  .teacher-sidebar {
    padding: 16px 12px;
  }

  .sidebar-nav {
    gap: 10px;
  }

  .teacher-content {
    padding: 20px;
  }
}
</style>
