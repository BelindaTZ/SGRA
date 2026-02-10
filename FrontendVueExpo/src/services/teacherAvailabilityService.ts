import http from './http';
import type {
  AvailabilityResponse,
  AvailabilityUpdateRequest,
  AvailabilityUpdateResponse,
} from '../types/availability';

export const fetchAvailability = async (periodoId?: number | null) => {
  const response = await http.get<AvailabilityResponse>('/api/docente/disponibilidad', {
    params: periodoId ? { periodoId } : undefined,
  });
  return response.data;
};

export const updateAvailability = async (payload: AvailabilityUpdateRequest) => {
  const response = await http.put<AvailabilityUpdateResponse>('/api/docente/disponibilidad', payload);
  return response.data;
};

export const deactivateAvailability = async (payload: AvailabilityUpdateRequest) => {
  const response = await http.delete<AvailabilityUpdateResponse>('/api/docente/disponibilidad', {
    data: payload,
  });
  return response.data;
};
