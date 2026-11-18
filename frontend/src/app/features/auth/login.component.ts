import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { DNI_PATTERN } from '../../core/forms/field-rules';
import { ErrorMessageService } from '../../core/http/error-message.service';

@Component({
  standalone: true,
  selector: 'app-login',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent implements OnInit {
  readonly form = new FormGroup({
    identificador: new FormControl('', { nonNullable: true, validators: [Validators.required,
      Validators.maxLength(254), control => {
        const value = String(control.value ?? '');
        return !value || DNI_PATTERN.test(value) ? null : Validators.email(control);
      }] }),
    clave: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(18)] })
  });
  loading = false;
  registered = false;
  showPassword = false;
  returnUrl = '';

  constructor(private readonly auth: AuthService, private readonly router: Router,
      private readonly route: ActivatedRoute, private readonly errors: ErrorMessageService) { }

  ngOnInit(): void {
    this.registered = this.route.snapshot.queryParamMap.get('registrado') === '1';
    this.returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? '';
  }

  submit(): void {
    if (this.form.invalid || this.loading) { this.form.markAllAsTouched(); return; }
    this.errors.clear();
    this.loading = true;
    const { identificador, clave } = this.form.getRawValue();
    this.auth.login(identificador.trim(), clave).subscribe({
      next: session => {
        this.errors.clear();
        const safeUrl = this.returnUrl.startsWith('/') && !this.returnUrl.startsWith('//') ? this.returnUrl : `/${session.portal}`;
        this.router.navigateByUrl(safeUrl);
      },
      error: () => this.loading = false
    });
  }
}
