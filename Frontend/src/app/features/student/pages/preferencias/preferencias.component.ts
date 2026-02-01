import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-preferencias-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './preferencias.component.html',
  styleUrl: './preferencias.component.scss',
})
export class PreferenciasPageComponent {
  channel = 'Correo';
  frequency = 'Semanal';
}
