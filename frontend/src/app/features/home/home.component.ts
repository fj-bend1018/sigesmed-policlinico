import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';

@Component({
  standalone: true,
  selector: 'app-home',
  imports: [CommonModule],
  templateUrl: './home.component.html'
})
export class HomeComponent implements OnInit, OnDestroy {
  readonly heroSlides = [
    {
      title: 'Cuidamos la salud de tu familia con cercanía y vocación.',
      description: 'Atención a niñas, niños y sus familias en San Gregorio, Ate. Conoce las seis especialidades, el laboratorio clínico, las ecografías y cómo contactar a recepción.',
      imageClass: 'hero-scene--one'
    },
    {
      title: 'Una atención cercana para cada familia.',
      description: 'Explora nuestras seis áreas de atención y los servicios complementarios de laboratorio clínico y ecografías.',
      imageClass: 'hero-scene--two'
    },
    {
      title: 'Calidad y calidez humana al servicio de la comunidad.',
      description: 'Estamos en San Gregorio, Ate. Revisa la dirección, el horario y los teléfonos para comunicarte con recepción.',
      imageClass: 'hero-scene--three'
    }
  ] as const;

  readonly specialties = [
    { name: 'Pediatría general', description: 'Atención de salud infantil con orientación sobre crecimiento, desarrollo y consultas frecuentes.', imageClass: 'photo-pediatrics' },
    { name: 'Medicina familiar', description: 'Atención médica general que considera las necesidades de cada persona y su entorno familiar.', imageClass: 'photo-family' },
    { name: 'Nutrición', description: 'Orientación sobre alimentación equilibrada y hábitos saludables, según la etapa de crecimiento infantil.', imageClass: 'photo-nutrition' },
    { name: 'Psicología infantil', description: 'Acompañamiento y orientación sobre bienestar emocional y conductual durante la infancia.', imageClass: 'photo-psychology' },
    { name: 'Odontología pediátrica', description: 'Cuidado preventivo de la salud bucal infantil y orientación sobre hábitos de higiene en casa.', imageClass: 'photo-dentistry' },
    { name: 'Otorrinolaringología', description: 'Evaluación de oído, nariz y garganta, con atención especializada para niñas y niños.', imageClass: 'photo-ent' }
  ] as const;

  readonly complementaryServices = [
    { name: 'Laboratorio clínico', description: 'Análisis clínicos que complementan la evaluación médica, según indicación del profesional tratante.', imageClass: 'photo-laboratory' },
    { name: 'Ecografías', description: 'Estudios por imágenes que apoyan la evaluación médica según indicación profesional.', imageClass: 'photo-ultrasound' }
  ] as const;

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
