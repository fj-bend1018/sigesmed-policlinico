import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  standalone: true, selector: 'app-recover', imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './recover.component.html'
})
export class RecoverComponent {
  readonly form = new FormGroup({ correo: new FormControl('', { nonNullable: true,
    validators: [Validators.required, Validators.email, Validators.maxLength(254)] }) });
  loading = false;
  submitted = false;
  constructor(private readonly auth: AuthService) { }

  submit(): void {
    if (this.form.invalid || this.loading) { this.form.markAllAsTouched(); return; }
    this.loading = true;
    this.auth.recover(this.form.controls.correo.value.trim()).subscribe({
      next: () => { this.submitted = true; this.loading = false; },
      error: () => this.loading = false
    });
  }
}
