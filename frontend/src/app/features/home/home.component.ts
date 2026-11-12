import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';

export interface HeroSlideItem {
  readonly number: string;
  readonly tag: string;
  readonly deckCategory: string;
  readonly deckTitle: string;
  readonly deckSubtitle: string;
  readonly title: string;
  readonly description: string;
  readonly imageUrl: string;
  readonly imageClass: string;
}

export interface SpecialtyItem {
  readonly code: string;
  readonly name: string;
  readonly schedule: string;
  readonly description: string;
  readonly imageUrl: string;
  readonly imageClass: string;
}

export interface ComplementaryServiceItem {
  readonly badge: string;
  readonly name: string;
  readonly description: string;
  readonly imageUrl: string;
  readonly imageClass: string;
}

@Component({
  standalone: true,
  selector: 'app-home',
  imports: [CommonModule, RouterLink],
  templateUrl: './home.component.html'
})
export class HomeComponent implements OnInit, OnDestroy {
  readonly heroSlides: readonly HeroSlideItem[] = [
    {
      number: '01',
      tag: 'Policlínico Infantil · Obra Social de la Diócesis de Chosica',
      deckCategory: 'SLIDE 01 · ATENCIÓN ACTIVA',
      deckTitle: 'Pediatría general y crecimiento infantil',
      deckSubtitle: 'Consultorios especializados · San Gregorio, Ate',
      title: 'Cuidamos la salud de tu familia con cercanía y vocación.',
      description: 'Atención a niñas, niños y sus familias en San Gregorio, Ate. Conoce las seis especialidades, el laboratorio clínico, las ecografías y cómo contactar a recepción.',
      imageUrl: '/assets/rediseno/hero-slide-1.jpg',
      imageClass: 'hero-scene--one'
    },
    {
      number: '02',
      tag: 'Apoyo Diagnóstico · Laboratorio y Ecografías',
      deckCategory: 'SLIDE 02 · APOYO DIAGNÓSTICO',
      deckTitle: 'Laboratorio clínico y ecografías de precisión',
      deckSubtitle: 'Resultados confiables para el médico tratante',
      title: 'Una atención cercana para cada familia.',
      description: 'Explora nuestras seis áreas de atención y los servicios complementarios de laboratorio clínico y ecografías.',
      imageUrl: '/assets/rediseno/hero-slide-2.jpg',
      imageClass: 'hero-scene--two'
    },
    {
      number: '03',
      tag: 'Comunidad y Vocación · San Gregorio, Ate',
      deckCategory: 'SLIDE 03 · COMUNIDAD Y VOCACIÓN',
      deckTitle: 'Obra social al servicio de cada familia',
      deckSubtitle: 'Lunes a sábado · 8:00 a. m. a 8:00 p. m.',
      title: 'Calidad y calidez humana al servicio de la comunidad.',
      description: 'Estamos en San Gregorio, Ate. Revisa la dirección, el horario y los teléfonos para comunicarte con recepción.',
      imageUrl: '/assets/rediseno/spec-familiar.jpg',
      imageClass: 'hero-scene--three'
    }
  ];

  readonly specialties: readonly SpecialtyItem[] = [
    {
      code: '01 · INFANCIA',
      name: 'Pediatría general',
      schedule: 'Lun – Sáb',
      description: 'Atención de salud infantil con orientación sobre crecimiento, desarrollo y consultas frecuentes.',
      imageUrl: '/assets/rediseno/spec-pediatria.jpg',
      imageClass: 'photo-pediatrics'
    },
    {
      code: '02 · INTEGRAL',
      name: 'Medicina familiar',
      schedule: 'Lun – Sáb',
      description: 'Atención médica general que considera las necesidades de cada persona y su entorno familiar.',
      imageUrl: '/assets/rediseno/spec-familiar.jpg',
      imageClass: 'photo-family'
    },
    {
      code: '03 · CRECIMIENTO',
      name: 'Nutrición',
      schedule: 'Lun – Sáb',
      description: 'Orientación sobre alimentación equilibrada y hábitos saludables, según la etapa de crecimiento infantil.',
      imageUrl: '/assets/rediseno/spec-nutricion.jpg',
      imageClass: 'photo-nutrition'
    },
    {
      code: '04 · BIENESTAR',
      name: 'Psicología infantil',
      schedule: 'Lun – Sáb',
      description: 'Acompañamiento y orientación sobre bienestar emocional y conductual durante la infancia.',
      imageUrl: '/assets/rediseno/spec-psicologia.jpg',
      imageClass: 'photo-psychology'
    },
    {
      code: '05 · PREVENCIÓN',
      name: 'Odontología pediátrica',
      schedule: 'Lun – Sáb',
      description: 'Cuidado preventivo de la salud bucal infantil y orientación sobre hábitos de higiene en casa.',
      imageUrl: '/assets/rediseno/spec-odontologia.jpg',
      imageClass: 'photo-dentistry'
    },
    {
      code: '06 · ESPECIALIDAD',
      name: 'Otorrinolaringología',
      schedule: 'Lun – Sáb',
      description: 'Evaluación de oído, nariz y garganta, con atención especializada para niñas y niños.',
      imageUrl: '/assets/rediseno/spec-otorrino.jpg',
      imageClass: 'photo-ent'
    }
  ];

  readonly complementaryServices: readonly ComplementaryServiceItem[] = [
    {
      badge: 'SERVICIO COMPLEMENTARIO 01',
      name: 'Laboratorio clínico',
      description: 'Análisis clínicos que complementan la evaluación médica, según indicación del profesional tratante.',
      imageUrl: '/assets/rediseno/hero-slide-2.jpg',
      imageClass: 'photo-laboratory'
    },
    {
      badge: 'SERVICIO COMPLEMENTARIO 02',
      name: 'Ecografías',
      description: 'Estudios por imágenes que apoyan la evaluación médica según indicación profesional.',
      imageUrl: '/assets/rediseno/diag-ecografia.jpg',
      imageClass: 'photo-ultrasound'
    }
  ];

  activeHeroSlide = 0;
  private autoAdvanceTimer?: number;
  private pointerInsideHero = false;
  private focusWithinHero = false;
  private readonly prefersReducedMotion = typeof window !== 'undefined'
    && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  ngOnInit(): void {
    this.scheduleAutoAdvance();
  }

  ngOnDestroy(): void {
    this.clearAutoAdvance();
  }

  showHeroSlide(index: number): void {
    if (index < 0 || index >= this.heroSlides.length) { return; }
    this.activeHeroSlide = index;
    this.scheduleAutoAdvance();
  }

  previousHeroSlide(): void {
    this.showHeroSlide((this.activeHeroSlide - 1 + this.heroSlides.length) % this.heroSlides.length);
  }

  nextHeroSlide(): void {
    this.showHeroSlide((this.activeHeroSlide + 1) % this.heroSlides.length);
  }

  onHeroMouseEnter(): void {
    this.pointerInsideHero = true;
    this.scheduleAutoAdvance();
  }

  onHeroMouseLeave(): void {
    this.pointerInsideHero = false;
    this.scheduleAutoAdvance();
  }

  onHeroFocusIn(): void {
    this.focusWithinHero = true;
    this.scheduleAutoAdvance();
  }

  onHeroFocusOut(event: FocusEvent): void {
    const hero = event.currentTarget as HTMLElement;
    if (!hero.contains(event.relatedTarget as Node | null)) {
      this.focusWithinHero = false;
      this.scheduleAutoAdvance();
    }
  }

  private scheduleAutoAdvance(): void {
    this.clearAutoAdvance();
    if (this.pointerInsideHero || this.focusWithinHero || this.prefersReducedMotion || typeof window === 'undefined') { return; }

    this.autoAdvanceTimer = window.setTimeout(() => this.nextHeroSlide(), 5000);
  }

  private clearAutoAdvance(): void {
    if (this.autoAdvanceTimer !== undefined && typeof window !== 'undefined') {
      window.clearTimeout(this.autoAdvanceTimer);
      this.autoAdvanceTimer = undefined;
    }
  }
}
