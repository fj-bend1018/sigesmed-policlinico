import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { DNI_PATTERN } from '../../core/forms/field-rules';

@Component({
  standalone: true,
  selector: 'app-login',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html'
})
export class LoginComponent implements OnInit {
  readonly form = new FormGroup({
    identificador: new FormControl('', { nonNullable: true, validators: [Validators.required,
      Validators.maxLength(254), control => {
        const value = String(control.value ?? '');
        return !value || DNI_PATTERN.test(value) ? null : Validators.email(control);
      }] }),
    clave: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(72)] })
  });
  loading = false;
  registered = false;
  returnUrl = '';

  constructor(private readonly auth: AuthService, private readonly router: Router,
      private readonly route: ActivatedRoute) { }

  ngOnInit(): void {
    this.registered = this.route.snapshot.queryParamMap.get('registrado') === '1';
    this.returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? '';
  }

  submit(): void {
    if (this.form.invalid || this.loading) { this.form.markAllAsTouched(); return; }
    this.loading = true;
    const { identificador, clave } = this.form.getRawValue();
    this.auth.login(identificador.trim(), clave).subscribe({
      next: session => {
        const safeUrl = this.returnUrl.startsWith('/') && !this.returnUrl.startsWith('//') ? this.returnUrl : `/${session.portal}`;
        this.router.navigateByUrl(safeUrl);
      },
      error: () => this.loading = false
    });
  }
}
