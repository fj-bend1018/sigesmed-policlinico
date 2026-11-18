import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { ErrorMessageService } from '../../core/http/error-message.service';

@Component({
  standalone: true,
  selector: 'app-register',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html'
})
export class RegisterComponent implements OnInit {
  readonly today = this.localDate(new Date());
  readonly latestBirthDate = this.localDate(this.previousDay());
  readonly form = new FormGroup({
    nombre: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(100)] }),
    apellidoPaterno: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(100)] }),
    apellidoMaterno: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(100)] }),
    dni: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.pattern(/^\d{8}$/)] }),
    fechaNacimiento: new FormControl('', { nonNullable: true, validators: [Validators.required, control => {
      const value = control.value as string;
      return !value || value < this.today ? null : { past: true };
    }] }),
    correo: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email, Validators.maxLength(254)] }),
    celular: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.pattern(/^\d{9}$/)] }),
    clave: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(18)] })
  });
  loading = false;
  returnUrl = '';

  constructor(private readonly auth: AuthService, private readonly router: Router,
      private readonly route: ActivatedRoute, private readonly errors: ErrorMessageService) { }

  ngOnInit(): void {
    const requestedUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? '';
    this.returnUrl = requestedUrl.startsWith('/') && !requestedUrl.startsWith('//') ? requestedUrl : '';
  }

  submit(): void {
    if (this.form.invalid || this.loading) { this.form.markAllAsTouched(); return; }
    this.errors.clear();
    this.loading = true;
    const payload = this.form.getRawValue();
    payload.nombre = payload.nombre.trim();
    payload.apellidoPaterno = payload.apellidoPaterno.trim();
    payload.apellidoMaterno = payload.apellidoMaterno.trim();
    payload.correo = payload.correo.trim();
    payload.celular = payload.celular.trim();
    this.auth.register(payload).subscribe({
      next: () => {
        this.errors.clear();
        this.router.navigate(['/login'], {
          queryParams: { registrado: '1', ...(this.returnUrl ? { returnUrl: this.returnUrl } : {}) }
        });
      },
      error: () => this.loading = false
    });
  }

  normalizeDigits(controlName: 'dni' | 'celular', maxLen: number, event: Event): void {
    const input = event.target as HTMLInputElement | null;
    if (!input) return;
    const digits = input.value.replace(/\D+/g, '').slice(0, maxLen);
    if (input.value !== digits) {
      input.value = digits;
    }
    this.form.controls[controlName].setValue(digits, { emitEvent: false });
  }

  private localDate(date: Date): string {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }

  private previousDay(): Date {
    const value = new Date();
    value.setDate(value.getDate() - 1);
    return value;
  }
}
