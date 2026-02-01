import { Injectable } from '@angular/core';

export interface SubjectMock {
  code: string;
  name: string;
}

export interface TeacherMock {
  id: number;
  name: string;
  department: string;
}

export type RequestStatus =
  | 'PENDIENTE'
  | 'ACEPTADA'
  | 'REALIZADA'
  | 'ESPERANDO ESPACIO'
  | 'PROGRAMADA'
  | 'COMPLETADA';

export type RequestType = 'Individual' | 'Grupal';

export interface StudentRequestMock {
  id: number;
  date: string;
  time: string;
  subject: SubjectMock;
  topic: string;
  teacher: TeacherMock;
  type: RequestType;
  status: RequestStatus;
}

export interface DashboardMetrics {
  pendientes: number;
  aceptadas: number;
  proximas: number;
  realizadas: number;
}

@Injectable({ providedIn: 'root' })
export class StudentMockService {
  readonly subjects: SubjectMock[] = [
    { code: 'INF301', name: 'Programación Avanzada' },
    { code: 'INF302', name: 'Base de Datos' },
    { code: 'INF303', name: 'Ingeniería de Software' },
    { code: 'INF304', name: 'Redes de Computadores' },
  ];

  readonly teachers: TeacherMock[] = [
    { id: 1, name: 'María Elena Rodríguez López', department: 'Sistemas' },
    { id: 2, name: 'Carlos Andrés Núñez', department: 'Software' },
    { id: 3, name: 'Lucía Fernanda Solís', department: 'Redes' },
  ];

  readonly timeSlots = [
    '08:00 - 09:00',
    '09:00 - 10:30',
    '10:30 - 12:00',
    '14:00 - 15:30',
    '16:00 - 17:30',
  ];

  readonly companions = [
    'Andrea Rojas',
    'Juan Paredes',
    'Luis Mendoza',
    'Carmen Villa',
  ];

  readonly requests: StudentRequestMock[] = [
    {
      id: 1,
      date: '08/01/2026',
      time: '09:00',
      subject: this.subjects[0],
      topic: 'Patrones de Diseño',
      teacher: this.teachers[0],
      type: 'Individual',
      status: 'PENDIENTE',
    },
    {
      id: 2,
      date: '05/01/2026',
      time: '10:30',
      subject: this.subjects[1],
      topic: 'Normalización',
      teacher: this.teachers[0],
      type: 'Grupal',
      status: 'ACEPTADA',
    },
    {
      id: 3,
      date: '10/12/2025',
      time: '16:00',
      subject: this.subjects[2],
      topic: 'Metodologías Ágiles',
      teacher: this.teachers[0],
      type: 'Individual',
      status: 'REALIZADA',
    },
    {
      id: 4,
      date: '07/01/2026',
      time: '08:00',
      subject: this.subjects[0],
      topic: 'Interfaces y Clases Abstractas',
      teacher: this.teachers[1],
      type: 'Individual',
      status: 'ESPERANDO ESPACIO',
    },
  ];

  getDashboardMetrics(): DashboardMetrics {
    return {
      pendientes: 1,
      aceptadas: 2,
      proximas: 0,
      realizadas: 1,
    };
  }
}
