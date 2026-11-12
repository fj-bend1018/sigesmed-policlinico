import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

export type LegacyWorkflowView = 'identification' | 'appointment' | 'confirmation' | 'payment' | 'error';

@Component({
  standalone: true,
  selector: 'app-legacy-workflow',
  imports: [CommonModule, RouterLink],
  templateUrl: './legacy-workflow.component.html'
})
export class LegacyWorkflowComponent {
  readonly view: LegacyWorkflowView;

  constructor(route: ActivatedRoute) {
    this.view = (route.snapshot.data['view'] as LegacyWorkflowView | undefined) ?? 'error';
  }

  get title(): string {
    const labels: Record<LegacyWorkflowView, string> = {
      identification: 'Identificación del paciente',
      appointment: 'Revisa los datos de tu cita',
      confirmation: 'Confirmación de cita',
      payment: 'Información de pago',
      error: 'No pudimos completar la solicitud'
    };
    return labels[this.view];
  }

  get description(): string {
    const descriptions: Record<LegacyWorkflowView, string> = {
      identification: 'Ingresa los datos de identificación para continuar con la atención.',
      appointment: 'Revisa el resumen de la atención antes de continuar.',
      confirmation: 'Consulta el resultado de una reserva asociada a tu cuenta.',
      payment: 'Consulta el estado de la opción de pago en este avance.',
      error: 'Inténtalo nuevamente en unos momentos o vuelve al inicio.'
    };
    return descriptions[this.view];
  }
}
