<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useAuthStore } from '../../stores/authStore';
import {
  fetchDashboardCards,
  fetchDashboardIndicadores,
  fetchDashboardProximasSesiones,
  fetchDashboardSolicitudesPendientes,
} from '../../services/teacherDashboardService';
import type {
  DashboardCardsResponse,
  DashboardIndicadoresResponse,
  DashboardProximaSesion,
  DashboardSolicitudPendiente,
} from '../../types/teacherDashboard';

const auth = useAuthStore();

const metrics = ref<DashboardCardsResponse | null>(null);
const pendingRequests = ref<DashboardSolicitudPendiente[]>([]);
const upcomingSessions = ref<DashboardProximaSesion[]>([]);
const indicadores = ref<DashboardIndicadoresResponse | null>(null);

const isLoading = ref({
  cards: false,
  solicitudes: false,
  sesiones: false,
  indicadores: false,
});
const errorMessage = ref<string | null>(null);

const formatDateLabel = (value?: string | null) => {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  return new Intl.DateTimeFormat('es-EC', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
};

const formatHours = (value?: number | null) => {
  if (value == null) return '—';
  return `${value.toFixed(2)} hrs`;
};

const periodIndicators = computed(() => {
  if (!indicadores.value) return [];
  return [
    {
      label: 'Tasa de Aceptación',
      value: Number(indicadores.value.tasaAceptacion ?? 0),
      accent: '#2f8b4f',
    },
    {
      label: 'Asistencia Promedio',
      value: Number(indicadores.value.asistenciaPromedio ?? 0),
      accent: '#d9466f',
    },
    {
      label: 'Sesiones Completadas',
      value: Number(indicadores.value.sesionesCompletadasPct ?? 0),
      accent: '#2f59c5',
    },
  ];
});

const hoursIndicator = computed(() => {
  if (!indicadores.value) return null;
  return {
    label: 'Horas de Refuerzo',
    value: formatHours(indicadores.value.horasRefuerzo),
    helper: 'Este período académico',
  };
});

const loadDashboard = async () => {
  errorMessage.value = null;
  isLoading.value = {
    cards: true,
    solicitudes: true,
    sesiones: true,
    indicadores: true,
  };

  try {
    const [cards, solicitudes, sesiones, indicadoresResponse] = await Promise.all([
      fetchDashboardCards(),
      fetchDashboardSolicitudesPendientes(),
      fetchDashboardProximasSesiones(),
      fetchDashboardIndicadores(),
    ]);
    metrics.value = cards;
    pendingRequests.value = solicitudes;
    upcomingSessions.value = sesiones;
    indicadores.value = indicadoresResponse;
  } catch (error) {
    console.error('Error cargando dashboard docente', error);
    errorMessage.value = 'No se pudieron cargar los datos del dashboard.';
  } finally {
    isLoading.value = {
      cards: false,
      solicitudes: false,
      sesiones: false,
      indicadores: false,
    };
  }
};

onMounted(loadDashboard);
</script>

<template>
  <section class="hero-card">
    <div class="hero-text">
      <h2>Bienvenido, {{ auth.username ?? 'Docente' }}</h2>
      <p>Panel de gestión de refuerzos académicos</p>
    </div>
    <RouterLink class="hero-button" to="/dashboard/docente/disponibilidad">
      <span class="hero-button-icon"><i class="bi bi-stopwatch"></i></span>
      Gestionar Disponibilidad
    </RouterLink>
  </section>

  <section class="metrics-grid">
    <article class="metric-card">
      <div class="metric-icon metric-icon--warning">
        <i class="bi bi-inbox"></i>
      </div>
      <div class="metric-info">
        <h3>{{ metrics?.solicitudesPendientes ?? '—' }}</h3>
        <span>Solicitudes Pendientes</span>
        <RouterLink to="/dashboard/docente/solicitudes">Ver todas</RouterLink>
      </div>
    </article>
    <article class="metric-card">
      <div class="metric-icon metric-icon--purple">
        <i class="bi bi-calendar-event"></i>
      </div>
      <div class="metric-info">
        <h3>{{ metrics?.sesionesProgramadas ?? '—' }}</h3>
        <span>Sesiones Programadas</span>
        <RouterLink to="/dashboard/docente/sesiones">Ver calendario</RouterLink>
      </div>
    </article>
    <article class="metric-card">
      <div class="metric-icon metric-icon--green">
        <i class="bi bi-check-circle"></i>
      </div>
      <div class="metric-info">
        <h3>{{ metrics?.sesionesCompletadas ?? '—' }}</h3>
        <span>Sesiones Completadas</span>
        <RouterLink to="/dashboard/docente/reportes">Ver historial</RouterLink>
      </div>
    </article>
    <article class="metric-card">
      <div class="metric-icon metric-icon--blue">
        <i class="bi bi-mortarboard"></i>
      </div>
      <div class="metric-info">
        <h3>{{ metrics?.estudiantesAtendidos ?? '—' }}</h3>
        <span>Estudiantes Atendidos</span>
        <RouterLink to="/dashboard/docente/reportes">Ver reportes</RouterLink>
      </div>
    </article>
  </section>

  <section class="dashboard-grid">
    <div class="panel-card">
      <div class="panel-header">
        <div class="panel-title">
          <span class="panel-icon"><i class="bi bi-bell"></i></span>
          <h4>Solicitudes Pendientes de Revisión</h4>
        </div>
        <span class="panel-badge" v-if="pendingRequests.length">{{ pendingRequests.length }} nuevas</span>
      </div>
      <div class="panel-list" v-if="pendingRequests.length">
        <div class="panel-item" v-for="request in pendingRequests" :key="request.idSolicitudRefuerzo">
          <div class="panel-avatar"><i class="bi bi-person"></i></div>
          <div class="panel-content">
            <strong>{{ request.estudianteNombre }}</strong>
            <small>{{ request.temarioAsignatura }}</small>
          </div>
          <div class="panel-meta">
            <span class="panel-date">{{ formatDateLabel(request.fechaHora) }}</span>
            <span class="chip">{{ request.tipo }}</span>
          </div>
        </div>
      </div>
      <div v-else class="empty-state">
        <span v-if="isLoading.solicitudes">Cargando solicitudes...</span>
        <span v-else>No hay solicitudes pendientes por revisar.</span>
      </div>
      <RouterLink class="panel-link" to="/dashboard/docente/solicitudes">Ver todas las solicitudes</RouterLink>
    </div>

    <div class="panel-card">
      <div class="panel-header">
        <div class="panel-title">
          <span class="panel-icon"><i class="bi bi-calendar3"></i></span>
          <h4>Próximas Sesiones</h4>
        </div>
      </div>
      <div v-if="upcomingSessions.length">
        <div class="panel-item" v-for="session in upcomingSessions" :key="session.idRefuerzoProgramado">
          <div class="panel-avatar"><i class="bi bi-book"></i></div>
          <div class="panel-content">
            <strong>{{ session.titulo }}</strong>
            <small>{{ formatDateLabel(session.fechaHora) }} • {{ session.estudianteNombre }}</small>
          </div>
          <span class="chip chip-muted">{{ session.modalidad }}</span>
        </div>
      </div>
      <div v-else class="empty-state empty-state--center">
        <template v-if="isLoading.sesiones">
          Cargando sesiones...
        </template>
        <template v-else>
          <div class="empty-icon"><i class="bi bi-calendar-event"></i></div>
          No tienes sesiones programadas
        </template>
      </div>
      <RouterLink class="panel-link" to="/dashboard/docente/sesiones">Ver calendario completo</RouterLink>
    </div>
  </section>

  <section class="actions-section">
    <h4>Acciones Rápidas</h4>
    <div class="actions-grid">
      <RouterLink class="action-card" to="/dashboard/docente/solicitudes">
        <span class="action-icon"><i class="bi bi-clipboard-check"></i></span>
        <span>Revisar Solicitudes</span>
      </RouterLink>
      <RouterLink class="action-card" to="/dashboard/docente/disponibilidad">
        <span class="action-icon"><i class="bi bi-alarm"></i></span>
        <span>Mi Disponibilidad</span>
      </RouterLink>
      <RouterLink class="action-card" to="/dashboard/docente/sesiones">
        <span class="action-icon"><i class="bi bi-people"></i></span>
        <span>Agrupar Sesiones</span>
      </RouterLink>
      <RouterLink class="action-card" to="/dashboard/docente/reportes">
        <span class="action-icon"><i class="bi bi-bar-chart"></i></span>
        <span>Mis Reportes</span>
      </RouterLink>
    </div>
  </section>

  <section class="indicator-card">
    <div class="indicator-header">
      <span class="indicator-icon"><i class="bi bi-graph-up"></i></span>
      <h4>Indicadores del Período</h4>
    </div>
    <div class="indicator-grid">
      <div class="indicator-item" v-for="indicator in periodIndicators" :key="indicator.label">
        <div class="indicator-label">
          <span>{{ indicator.label }}</span>
          <strong>{{ indicator.value }}%</strong>
        </div>
        <div class="indicator-track">
          <span
            class="indicator-fill"
            :style="{ width: `${indicator.value}%`, background: indicator.accent }"
          ></span>
        </div>
      </div>
      <div class="indicator-item indicator-item--hours" v-if="hoursIndicator">
        <div class="indicator-label">
          <span>{{ hoursIndicator.label }}</span>
          <strong>{{ hoursIndicator.value }}</strong>
        </div>
        <small>{{ hoursIndicator.helper }}</small>
      </div>
    </div>
    <p v-if="errorMessage" class="empty-state">{{ errorMessage }}</p>
  </section>
</template>

<style scoped>
.hero-card {
  background: linear-gradient(90deg, #0f5b3b 0%, #0e5337 55%, #0b4028 100%);
  padding: 22px 26px;
  border-radius: 16px;
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  box-shadow: 0 10px 22px rgba(14, 58, 35, 0.25);
}

.hero-text h2 {
  margin: 0 0 6px;
  font-weight: 600;
  font-size: 22px;
}

.hero-text p {
  margin: 0;
  font-size: 13px;
  opacity: 0.85;
}

.hero-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: #cda343;
  color: #fff;
  padding: 10px 18px;
  border-radius: 10px;
  text-decoration: none;
  font-size: 13px;
  font-weight: 600;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.2);
}

.hero-button-icon {
  font-size: 14px;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.metric-card {
  background: #fff;
  border-radius: 14px;
  padding: 16px;
  border: 1px solid #e6ecea;
  box-shadow: 0 8px 16px rgba(15, 23, 42, 0.06);
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.metric-info h3 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
}

.metric-info span {
  display: block;
  color: #56635d;
  font-size: 12px;
  margin-bottom: 6px;
}

.metric-info a {
  color: #2d5bd1;
  font-size: 12px;
  text-decoration: none;
  font-weight: 600;
}

.metric-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  font-size: 18px;
  color: #fff;
}

.metric-icon--warning {
  background: #f1931b;
}

.metric-icon--purple {
  background: #8b2bbf;
}

.metric-icon--green {
  background: #2f8b4f;
}

.metric-icon--blue {
  background: #2a7bd9;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.panel-card {
  background: #fff;
  border-radius: 14px;
  padding: 16px;
  border: 1px solid #e6ecea;
  box-shadow: 0 8px 16px rgba(15, 23, 42, 0.06);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel-title h4 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #1f2b24;
}

.panel-icon {
  font-size: 16px;
}

.panel-badge {
  background: #f0f4ff;
  color: #2d5bd1;
  font-size: 11px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 999px;
}

.panel-list {
  display: grid;
  gap: 10px;
}

.panel-item {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 12px;
  align-items: center;
  background: #f8faf9;
  border-radius: 12px;
  padding: 10px 12px;
  border: 1px solid #edf1ef;
}

.panel-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #eef2f7;
  display: grid;
  place-items: center;
  font-size: 14px;
}

.panel-content strong {
  display: block;
  font-size: 13px;
}

.panel-content small {
  color: #6c7b73;
  font-size: 12px;
}

.panel-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  font-size: 11px;
  color: #66736c;
}

.panel-date {
  font-size: 11px;
}

.panel-link {
  text-decoration: none;
  font-size: 12px;
  font-weight: 600;
  color: #2d5bd1;
  align-self: flex-end;
}

.chip {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  background: #e6f2ff;
  color: #2166c7;
}

.chip-muted {
  background: #eef3ef;
  color: #2e4b3b;
}

.empty-state {
  padding: 14px;
  border-radius: 12px;
  border: 1px dashed #d7e6dc;
  color: #6c7b73;
  background: #f7faf8;
  font-size: 13px;
}

.empty-state--center {
  text-align: center;
  display: grid;
  gap: 6px;
  justify-items: center;
}

.empty-icon {
  font-size: 22px;
}

.actions-section h4 {
  margin: 0 0 12px;
  font-weight: 600;
  font-size: 16px;
}

.actions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 14px;
}

.action-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e6ecea;
  box-shadow: 0 8px 16px rgba(15, 23, 42, 0.06);
  padding: 16px;
  text-decoration: none;
  color: #1f2b24;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  font-weight: 600;
}

.action-icon {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: #f3f6ff;
  display: grid;
  place-items: center;
  font-size: 15px;
  color: #2d5bd1;
}

.indicator-card {
  margin-top: 20px;
  background: #fff;
  border-radius: 14px;
  padding: 16px;
  border: 1px solid #e6ecea;
  box-shadow: 0 8px 16px rgba(15, 23, 42, 0.06);
}

.indicator-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.indicator-header h4 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
}

.indicator-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.indicator-label {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  margin-bottom: 6px;
  color: #4a5952;
  font-weight: 600;
}

.indicator-track {
  height: 6px;
  background: #e9edf5;
  border-radius: 999px;
  overflow: hidden;
}

.indicator-fill {
  display: block;
  height: 100%;
}

.indicator-item--hours {
  background: #f8fafb;
  border-radius: 12px;
  padding: 12px;
  border: 1px solid #edf1ef;
}

.indicator-item--hours small {
  color: #66736c;
  font-size: 11px;
}

@media (max-width: 768px) {
  .hero-card {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}
</style>
