import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  RequestStatus,
  StudentMockService,
  StudentRequestMock,
} from '../../../../core/services/student-mock.service';

@Component({
  selector: 'app-mis-solicitudes-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './mis-solicitudes.component.html',
  styleUrl: './mis-solicitudes.component.scss',
})
export class MisSolicitudesPageComponent {
  requests: StudentRequestMock[] = [];
  statusOptions: RequestStatus[] = [
    'PENDIENTE',
    'ACEPTADA',
    'PROGRAMADA',
    'REALIZADA',
    'ESPERANDO ESPACIO',
  ];

  constructor(public mockService: StudentMockService) {
    this.requests = this.mockService.requests;
  }

  getStatusClass(status: RequestStatus): string {
    switch (status) {
      case 'PENDIENTE':
        return 'status-chip pendiente';
      case 'ACEPTADA':
        return 'status-chip aceptada';
      case 'REALIZADA':
      case 'COMPLETADA':
        return 'status-chip realizada';
      case 'PROGRAMADA':
        return 'status-chip programada';
      case 'ESPERANDO ESPACIO':
        return 'status-chip esperando';
      default:
        return 'status-chip';
    }
  }

  getTypeClass(type: string): string {
    return type === 'Grupal' ? 'type-chip grupal' : 'type-chip individual';
  }
}
