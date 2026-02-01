import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login.component';
import { LayoutComponent } from './shared/layout/layout.component';
import { authGuard } from './core/guards/authGuard';
import { roleGuard } from './core/guards/roleGuard';
import { StudentLayoutComponent } from './features/student/layout/student-layout.component';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },

  { path: 'login', component: LoginComponent },

  {
    path: 'dashboard/estudiante',
    component: StudentLayoutComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['STUDENT'] },
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/student/pages/dashboard/student-dashboard.component')
            .then(m => m.StudentDashboardPageComponent),
        data: { title: 'Dashboard Estudiante' },
      },
      {
        path: 'nueva-solicitud',
        loadComponent: () =>
          import('./features/student/pages/nueva-solicitud/nueva-solicitud.component')
            .then(m => m.NuevaSolicitudPageComponent),
        data: { title: 'Nueva Solicitud de Refuerzo' },
      },
      {
        path: 'mis-solicitudes',
        loadComponent: () =>
          import('./features/student/pages/mis-solicitudes/mis-solicitudes.component')
            .then(m => m.MisSolicitudesPageComponent),
        data: { title: 'Mis Solicitudes' },
      },
      {
        path: 'historial',
        loadComponent: () =>
          import('./features/student/pages/historial/historial.component')
            .then(m => m.HistorialPageComponent),
        data: { title: 'Historial' },
      },
      {
        path: 'preferencias',
        loadComponent: () =>
          import('./features/student/pages/preferencias/preferencias.component')
            .then(m => m.PreferenciasPageComponent),
        data: { title: 'Preferencias' },
      },
    ],
  },

  {
    path: 'dashboard',
    component: LayoutComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'admin',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
        loadComponent: () =>
          import('./features/dashboards/adminDashboard/adminDashboard.component')
            .then(m => m.AdminDashboardComponent),
      },
      {
        path: 'coordinador',
        canActivate: [roleGuard],
        data: { roles: ['COORDINATOR'] },
        loadComponent: () =>
          import('./features/dashboards/coordinatorDashboard/coordinatorDashboard.component')
            .then(m => m.CoordinatorDashboardComponent),
      },
      {
        path: 'docente',
        canActivate: [roleGuard],
        data: { roles: ['TEACHER'] },
        loadComponent: () =>
          import('./features/dashboards/teacherDashboard/teacherDashboard.component')
            .then(m => m.TeacherDashboardComponent),
      },

      // si entras a /dashboard, manda a coordinador (o al que quieras)
      { path: '', redirectTo: 'coordinador', pathMatch: 'full' },
    ],
  },

  { path: '**', redirectTo: 'login' },
];
