import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs/operators';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-student-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './student-layout.component.html',
  styleUrl: './student-layout.component.scss',
})
export class StudentLayoutComponent {
  title = 'Dashboard Estudiante';

  constructor(private router: Router, private route: ActivatedRoute, private auth: AuthService) {
    this.updateTitle();

    this.router.events.pipe(filter(event => event instanceof NavigationEnd)).subscribe(() => {
      this.updateTitle();
    });
  }

  onLogout() {
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }

  private updateTitle() {
    let currentRoute = this.route;

    while (currentRoute.firstChild) {
      currentRoute = currentRoute.firstChild;
    }

    this.title = currentRoute.snapshot.data?.['title'] ?? 'Dashboard Estudiante';
  }
}
