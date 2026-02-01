import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { StudentMockService } from '../../../../core/services/student-mock.service';

@Component({
  selector: 'app-nueva-solicitud-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './nueva-solicitud.component.html',
  styleUrl: './nueva-solicitud.component.scss',
})
export class NuevaSolicitudPageComponent {
  currentStep = 1;
  steps = [
    'Asignatura y Docente',
    'Tema y Motivo',
    'Tipo y Modalidad',
    'Horario',
    'Confirmación',
  ];

  showSubmitMessage = false;

  form = {
    subject: '',
    teacher: '',
    topic: '',
    motive: '',
    type: 'Individual',
    modality: 'Presencial',
    schedule: '',
    companions: [] as string[],
  };

  constructor(public mockService: StudentMockService, private router: Router) {}

  nextStep() {
    if (this.currentStep < this.steps.length) {
      this.currentStep += 1;
    }
  }

  prevStep() {
    if (this.currentStep > 1) {
      this.currentStep -= 1;
    }
  }

  cancel() {
    this.router.navigateByUrl('/dashboard/estudiante');
  }

  toggleCompanion(name: string, event: Event) {
    const checked = (event.target as HTMLInputElement).checked;

    if (checked) {
      this.form.companions = [...this.form.companions, name];
    } else {
      this.form.companions = this.form.companions.filter(item => item !== name);
    }
  }

  onTypeChange() {
    if (this.form.type === 'Individual') {
      this.form.companions = [];
    }
  }

  submit() {
    this.showSubmitMessage = true;
  }
}
