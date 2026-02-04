export interface DashboardCardsResponse {
  solicitudesPendientes: number;
  sesionesProgramadas: number;
  sesionesCompletadas: number;
  estudiantesAtendidos: number;
}

export interface DashboardSolicitudPendiente {
  idSolicitudRefuerzo: number;
  estudianteNombre: string;
  temarioAsignatura: string;
  fechaHora: string;
  tipo: string;
  nuevas: boolean;
}

export interface DashboardProximaSesion {
  idRefuerzoProgramado: number;
  titulo: string;
  fechaHora: string;
  estudianteNombre: string;
  modalidad: string;
}

export interface DashboardIndicadoresResponse {
  tasaAceptacion: number;
  asistenciaPromedio: number;
  sesionesCompletadasPct: number;
  horasRefuerzo: number;
}

export interface DashboardNotificacion {
  idNotificacion: number;
  titulo: string;
  mensaje: string;
  fechaEnvio: string;
}
