import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  standalone: true, selector: 'app-reset', imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './reset.component.html'
})
export class ResetComponent {
  readonly form = new FormGroup({ nuevaClave: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(18)] }) });
  readonly token: string;
  loading = false;
  completed = false;
  constructor(private readonly route: ActivatedRoute, private readonly auth: AuthService, private readonly router: Router) {
    this.token = route.snapshot.queryParamMap.get('token') ?? '';
  }

  submit(): void {
    if (!this.token || this.form.invalid || this.loading) { this.form.markAllAsTouched(); return; }
    this.loading = true;
    this.auth.reset(this.token, this.form.controls.nuevaClave.value).subscribe({
      next: () => { this.completed = true; this.loading = false; },
      error: () => this.loading = false
    });
  }
  goToLogin(): void { this.router.navigate(['/login']); }
}
