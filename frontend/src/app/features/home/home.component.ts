import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

export interface SpecialtyItem {
  readonly code: string;
  readonly name: string;
  readonly schedule: string;
  readonly description: string;
  readonly imageUrl: string;
}

export interface ComplementaryServiceItem {
  readonly badge: string;
  readonly name: string;
  readonly description: string;
  readonly ctaLabel: string;
  readonly imageUrl: string;
}

export interface FaqItem {
  readonly question: string;
  readonly answer: string;
  open: boolean;
}

@Component({
  standalone: true,
  selector: 'app-home',
  imports: [CommonModule, RouterLink],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent {
  readonly specialties: readonly SpecialtyItem[] = [
    {
      code: 'PEDIATRIA_GENERAL',
      name: 'Pediatría general',
      schedule: 'Lunes a sábado · 8:00 a. m. – 8:00 p. m.',
      description: 'Control del crecimiento, desarrollo infantil, vacunación y atención integral para niños y adolescentes.',
      imageUrl: '/assets/rediseno/spec-pediatria.jpg'
    },
    {
      code: 'MEDICINA_FAMILIAR',
      name: 'Medicina familiar',
      schedule: 'Lunes a sábado · 8:00 a. m. – 8:00 p. m.',
      description: 'Atención primaria continua y preventiva orientada al cuidado integral de cada miembro de la familia.',
      imageUrl: '/assets/rediseno/spec-familiar.jpg'
    },
    {
      code: 'NUTRICION',
      name: 'Nutrición',
      schedule: 'Lunes a sábado · 8:00 a. m. – 8:00 p. m.',
      description: 'Evaluación nutricional personalizada y planes de alimentación saludable para todas las etapas de la vida.',
      imageUrl: '/assets/rediseno/spec-nutricion.jpg'
    },
    {
      code: 'PSICOLOGIA_INFANTIL',
      name: 'Psicología infantil',
      schedule: 'Lunes a sábado · 8:00 a. m. – 8:00 p. m.',
      description: 'Acompañamiento emocional, conductual y psicopedagógico para el bienestar y desarrollo de los menores.',
      imageUrl: '/assets/rediseno/spec-psicologia.jpg'
    },
    {
      code: 'ODONTOLOGIA_PEDIATRICA',
      name: 'Odontología pediátrica',
      schedule: 'Lunes a sábado · 8:00 a. m. – 8:00 p. m.',
      description: 'Prevención, diagnóstico y tratamiento odontológico especializado en un entorno cálido para los niños.',
      imageUrl: '/assets/rediseno/spec-odontologia.jpg'
    },
    {
      code: 'OTORRINOLARINGOLOGIA',
      name: 'Otorrinolaringología',
      schedule: 'Lunes a sábado · 8:00 a. m. – 8:00 p. m.',
      description: 'Diagnóstico y manejo clínico de afecciones de oído, nariz, garganta y vías respiratorias altas.',
      imageUrl: '/assets/rediseno/spec-otorrino.jpg'
    }
  ];

  readonly complementaryServices: readonly ComplementaryServiceItem[] = [
    {
      badge: 'Diagnóstico Clínico',
      name: 'Laboratorio clínico',
      description: 'Análisis hematológicos, bioquímicos y pruebas clínicas de rutina con entrega oportuna de resultados para el seguimiento de tu consulta.',
      ctaLabel: 'Agendar análisis',
      imageUrl: '/assets/rediseno/hero-slide-2.jpg'
    },
    {
      badge: 'Diagnóstico por Imágenes',
      name: 'Ecografías',
      description: 'Estudios ecográficos generales y especializados realizados con equipamiento adecuado y lectura médica confiable.',
      ctaLabel: 'Agendar ecografía',
      imageUrl: '/assets/rediseno/diag-ecografia.jpg'
    }
  ];

  readonly faqs: FaqItem[] = [
    {
      question: '¿Cómo reservo una cita médica?',
      answer: 'Puedes reservar en línea ingresando a "Reservar cita" o desde tu Portal del Paciente eligiendo la especialidad, fecha y horario disponible.',
      open: true
    },
    {
      question: '¿Qué especialidades médicas y servicios ofrecen?',
      answer: 'Contamos con 6 especialidades médicas (Pediatría general, Medicina familiar, Nutrición, Psicología infantil, Odontología pediátrica y Otorrinolaringología) y 2 servicios de diagnóstico (Laboratorio clínico y Ecografías).',
      open: false
    },
    {
      question: '¿Con cuánta anticipación puedo cancelar o reprogramar?',
      answer: 'Las citas pueden cancelarse o reprogramarse hasta con 2 horas de anticipación directamente desde tu panel de paciente o comunicándote con recepción.',
      open: false
    },
    {
      question: '¿Cuál es la dirección exacta y el horario de atención?',
      answer: 'Estamos en Av. Alfonso Ugarte con Av. La Esperanza, Urb. San Gregorio, Ate, Lima. Atendemos de lunes a sábado de 8:00 a. m. a 8:00 p. m.',
      open: false
    },
    {
      question: '¿Cómo me comunico con recepción o administración?',
      answer: 'Puedes llamarnos a los teléfonos (01) 351-6775 o (01) 352-0695, o escribirnos al correo oficial psaludchosica@hotmail.com.',
      open: false
    }
  ];

  toggleFaq(index: number): void {
    this.faqs[index].open = !this.faqs[index].open;
  }
}
