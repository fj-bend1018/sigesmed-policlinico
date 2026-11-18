import { CommonModule } from '@angular/common';
import { Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { NavigationCancel, NavigationEnd, NavigationError, NavigationStart, Router, RouterLink, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth/auth.service';
import { AuthSession } from './core/auth/auth.models';
import { SessionTransitionService } from './core/auth/session-transition.service';
import { AppointmentDraftService } from './core/appointments/appointment-draft.service';
import { ErrorMessageService } from './core/http/error-message.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent implements OnInit, OnDestroy {
  session: AuthSession | null;
  menuOpen = false;
  splashVisible = true;
  splashLeaving = false;
  showBackToTop = false;
  routeTransitionActive = false;
  showPublicChrome = true;
  isFullBleed = false;
  readonly errorMessage$ = this.errors.message$;
  readonly sessionTransition$ = this.sessionTransition.state$;
  private readonly splashTimers: ReturnType<typeof setTimeout>[] = [];
  private routeTransitionTimer: ReturnType<typeof setTimeout> | null = null;
  private lastRoutePath = '/';

  constructor(private readonly auth: AuthService, private readonly errors: ErrorMessageService,
      private readonly router: Router, private readonly appointmentDraft: AppointmentDraftService,
      private readonly sessionTransition: SessionTransitionService) {
    this.session = auth.currentSession();
    this.lastRoutePath = this.routePath(this.router.url);
    this.updateLayoutFlags(this.lastRoutePath);
    auth.session$.subscribe(session => this.session = session);
    this.router.events.subscribe(event => {
      if (event instanceof NavigationStart) {
        if (this.routePath(event.url) !== this.lastRoutePath) this.beginRouteTransition();
      } else if (event instanceof NavigationEnd) {
        const nextPath = this.routePath(event.urlAfterRedirects);
        if (nextPath !== this.lastRoutePath) this.errors.clear();
        this.updateBackToTopVisibility();
        this.lastRoutePath = nextPath;
        this.updateLayoutFlags(this.lastRoutePath);
        this.menuOpen = false;
        this.finishRouteTransition();
      } else if (event instanceof NavigationCancel || event instanceof NavigationError) {
        this.clearRouteTransition();
      }
    });
  }

  ngOnInit(): void {
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const leaveDelay = reducedMotion ? 350 : 1_650;
    const removeDelay = reducedMotion ? 600 : 2_050;
    this.splashTimers.push(setTimeout(() => this.splashLeaving = true, leaveDelay));
    this.splashTimers.push(setTimeout(() => this.splashVisible = false, removeDelay));
    this.updateBackToTopVisibility();
  }

  ngOnDestroy(): void {
    this.splashTimers.forEach(timer => clearTimeout(timer));
    if (this.routeTransitionTimer !== null) clearTimeout(this.routeTransitionTimer);
  }

  @HostListener('window:scroll')
  onWindowScroll(): void { this.updateBackToTopVisibility(); }

  scrollToTop(): void {
    const behavior: ScrollBehavior = window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth';
    window.scrollTo({ top: 0, behavior });
  }

  logout(): void {
    this.appointmentDraft.clear();
    this.auth.logout().subscribe({
      next: () => this.router.navigate(['/login']),
      error: () => this.router.navigate(['/login'])
    });
  }

  clearError(): void { this.errors.clear(); }
  toggleNav(): void { this.menuOpen = !this.menuOpen; }

  private updateBackToTopVisibility(): void {
    const currentPath = this.router.url.split(/[?#]/, 1)[0];
    this.showBackToTop = currentPath === '/' && window.scrollY > 320;
  }

  private routePath(url: string): string {
    return url.split(/[?#]/, 1)[0] || '/';
  }

  private updateLayoutFlags(path: string): void {
    const clean = path || '/';
    const isAuth = clean.startsWith('/login') || clean.startsWith('/registro')
      || clean.startsWith('/recuperar') || clean.startsWith('/restablecer');
    const isPortal = clean.startsWith('/paciente') || clean.startsWith('/recepcionista')
      || clean.startsWith('/administrador') || clean.startsWith('/doctor');
    this.showPublicChrome = !isAuth && !isPortal;
    this.isFullBleed = clean === '/' || isAuth || isPortal;
  }

  private beginRouteTransition(): void {
    if (this.routeTransitionTimer !== null) clearTimeout(this.routeTransitionTimer);
    this.routeTransitionTimer = null;
    this.routeTransitionActive = true;
  }

  private finishRouteTransition(): void {
    if (!this.routeTransitionActive) return;
    if (this.routeTransitionTimer !== null) clearTimeout(this.routeTransitionTimer);
    this.routeTransitionTimer = setTimeout(() => {
      this.routeTransitionActive = false;
      this.routeTransitionTimer = null;
    }, 140);
  }

  private clearRouteTransition(): void {
    if (this.routeTransitionTimer !== null) clearTimeout(this.routeTransitionTimer);
    this.routeTransitionTimer = null;
    this.routeTransitionActive = false;
  }
}
