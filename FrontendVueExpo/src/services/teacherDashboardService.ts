import http from './http';
import type {
  DashboardCardsResponse,
  DashboardIndicadoresResponse,
  DashboardNotificacion,
  DashboardProximaSesion,
  DashboardSolicitudPendiente,
} from '../types/teacherDashboard';

export const fetchDashboardCards = async (periodoId?: number | null) => {
  const response = await http.get<DashboardCardsResponse>('/api/docente/dashboard/cards', {
    params: periodoId ? { periodoId } : undefined,
  });
  return response.data;
};

export const fetchDashboardSolicitudesPendientes = async (limit = 5, periodoId?: number | null) => {
  const response = await http.get<DashboardSolicitudPendiente[]>('/api/docente/dashboard/solicitudes-pendientes', {
    params: {
      limit,
      ...(periodoId ? { periodoId } : {}),
    },
  });
  return response.data;
};

export const fetchDashboardProximasSesiones = async (limit = 3, periodoId?: number | null) => {
  const response = await http.get<DashboardProximaSesion[]>('/api/docente/dashboard/proximas-sesiones', {
    params: {
      limit,
      ...(periodoId ? { periodoId } : {}),
    },
  });
  return response.data;
};

export const fetchDashboardIndicadores = async (periodoId?: number | null) => {
  const response = await http.get<DashboardIndicadoresResponse>('/api/docente/dashboard/indicadores', {
    params: periodoId ? { periodoId } : undefined,
  });
  return response.data;
};

export const fetchDashboardNotificaciones = async (limit = 10) => {
  const response = await http.get<DashboardNotificacion[]>('/api/docente/dashboard/notificaciones', {
    params: { limit },
  });
  return response.data;
};
