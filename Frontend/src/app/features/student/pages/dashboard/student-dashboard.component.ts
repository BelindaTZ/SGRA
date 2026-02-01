import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DashboardMetrics, StudentMockService } from '../../../../core/services/student-mock.service';

@Component({
  selector: 'app-student-dashboard-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './student-dashboard.component.html',
  styleUrl: './student-dashboard.component.scss',
})
export class StudentDashboardPageComponent {
  username = localStorage.getItem('sgra_username') ?? 'Estudiante';
  metrics: DashboardMetrics;

  constructor(private mockService: StudentMockService) {
    this.metrics = this.mockService.getDashboardMetrics();
  }
}
