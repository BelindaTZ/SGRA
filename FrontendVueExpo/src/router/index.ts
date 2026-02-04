import { createRouter, createWebHistory } from 'vue-router';
import LoginPage from '../pages/authPages/LoginPage.vue';
import TeacherLayout from '../layouts/TeacherLayout.vue';
import TeacherDashboard from '../pages/teacherPages/TeacherDashboard.vue';
import AvailabilityPage from '../pages/teacherPages/AvailabilityPage.vue';
import RequestsPage from '../pages/teacherPages/RequestsPage.vue';
import SessionsPage from '../pages/teacherPages/SessionsPage.vue';
import ReportsPage from '../pages/teacherPages/ReportsPage.vue';
import PreferencesPage from '../pages/teacherPages/PreferencesPage.vue';
import StudentHome from '../pages/studentPages/StudentHome.vue';
import WorkingPage from '../pages/sharedPages/WorkingPage.vue';
import NotFoundPage from '../pages/sharedPages/NotFoundPage.vue';
import { useAuthStore } from '../stores/authStore';

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: LoginPage },
  {
    path: '/dashboard/docente',
    component: TeacherLayout,
    meta: { requiresAuth: true, roles: ['TEACHER'] },
    children: [
      { path: '', component: TeacherDashboard },
      { path: 'solicitudes', component: RequestsPage },
      { path: 'disponibilidad', component: AvailabilityPage },
      { path: 'sesiones', component: SessionsPage },
      { path: 'reportes', component: ReportsPage },
      { path: 'preferencias', component: PreferencesPage },
    ],
  },
  {
    path: '/dashboard/estudiante',
    component: StudentHome,
    meta: { requiresAuth: true, roles: ['STUDENT'] },
  },
  {
    path: '/dashboard/en-construccion',
    component: WorkingPage,
    meta: { requiresAuth: true },
  },
  { path: '/:pathMatch(.*)*', component: NotFoundPage },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to) => {
  const auth = useAuthStore();

  if (to.meta?.requiresAuth && !auth.isAuthenticated) {
    return '/login';
  }

  const allowedRoles = (to.meta?.roles ?? []) as string[];
  if (allowedRoles.length > 0 && auth.role && !allowedRoles.includes(auth.role)) {
    const redirectMap: Record<string, string> = {
      TEACHER: '/dashboard/docente',
      STUDENT: '/dashboard/estudiante',
      ADMIN: '/dashboard/en-construccion',
      COORDINATOR: '/dashboard/en-construccion',
    };
    return redirectMap[auth.role] ?? '/login';
  }

  return true;
});

export default router;
