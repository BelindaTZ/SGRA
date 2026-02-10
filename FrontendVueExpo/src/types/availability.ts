export type AvailabilityStatus = 'DISPONIBLE' | 'NO_DISPONIBLE' | 'SESION';

export interface AvailabilitySlotResponse {
  diaSemana: number;
  franjaId: number;
  status: AvailabilityStatus;
}

export interface FranjaHorarioDto {
  franjaId: number;
  horaInicio: string;
  horaFin: string;
}

export interface AvailabilityResponse {
  periodoId: number | null;
  periodo: string | null;
  franjas: FranjaHorarioDto[];
  slots: AvailabilitySlotResponse[];
}

export interface AvailabilitySlotRequest {
  diaSemana: number;
  franjaId: number;
  status: AvailabilityStatus;
}

export interface AvailabilityUpdateRequest {
  periodoId: number | null;
  slots: AvailabilitySlotRequest[];
}

export interface AvailabilityUpdateResponse {
  message: string;
  updated: number;
}
