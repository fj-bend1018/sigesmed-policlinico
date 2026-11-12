import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Appointment, AppointmentApiService } from '../../core/appointments/appointment-api.service';

type PaymentMethod = 'CARD' | 'YAPE' | 'PLIN';

@Component({
  standalone: true,
  selector: 'app-payment-demo',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './payment-demo.component.html',
  styleUrl: './payment-demo.component.scss'
})
export class PaymentDemoComponent implements OnInit {
  appointment: Appointment | null = null;
  loading = true;
  loadError = false;
  selectedMethod: PaymentMethod = 'CARD';
  cardNumber = '';
  cardHolder = '';
  expiry = '';
  cvv = '';
  yapePhone = '';
  yapeCode = '';
  simulationComplete = false;
  formMessage = '';
  private appointmentId = 0;

  constructor(private readonly route: ActivatedRoute, private readonly api: AppointmentApiService) { }

  ngOnInit(): void {
    this.appointmentId = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isSafeInteger(this.appointmentId) || this.appointmentId <= 0) {
      this.loading = false;
      this.loadError = true;
      return;
    }
    this.api.appointment(this.appointmentId).subscribe({
      next: appointment => { this.appointment = appointment; this.loading = false; },
      error: () => { this.loading = false; this.loadError = true; }
    });
  }

  get cardNetwork(): string {
    const digits = this.cardNumber.replace(/\D/g, '');
    if (digits.startsWith('4')) return 'Visa';
    if (/^5[1-5]/.test(digits)) return 'Mastercard';
    if (digits.length >= 4) {
      const prefix = Number(digits.slice(0, 4));
      if (prefix >= 2221 && prefix <= 2720) return 'Mastercard';
    }
    if (/^3[47]/.test(digits)) return 'American Express';
    if (/^(30[0-5]|36|38|39)/.test(digits)) return 'Diners Club';
    return '';
  }

  get cardLength(): number { return this.cardNetwork === 'American Express' ? 15 : 16; }

  get cardReady(): boolean {
    const digits = this.cardNumber.replace(/\D/g, '');
    return Boolean(this.cardNetwork) && digits.length === this.cardLength
      && this.cardHolder.trim().length >= 2
      && /^(0[1-9]|1[0-2])\/\d{2}$/.test(this.expiry)
      && /^\d{3,4}$/.test(this.cvv);
  }

  get yapeReady(): boolean {
    return /^\d{9}$/.test(this.yapePhone) && /^\d{6}$/.test(this.yapeCode);
  }

  get canSimulate(): boolean {
    if (this.simulationComplete) return false;
    if (this.selectedMethod === 'CARD') return this.cardReady;
    if (this.selectedMethod === 'YAPE') return this.yapeReady;
    return true;
  }

  selectMethod(method: PaymentMethod): void {
    this.selectedMethod = method;
    this.formMessage = '';
    this.simulationComplete = false;
  }

  updateCardNumber(value: string): void {
    const digits = value.replace(/\D/g, '').slice(0, 16);
    this.cardNumber = digits.replace(/(.{4})/g, '$1 ').trim();
  }

  limitDigits(value: string, max: number): string {
    return value.replace(/\D/g, '').slice(0, max);
  }

  formatExpiry(value: string): void {
    const digits = value.replace(/\D/g, '').slice(0, 4);
    this.expiry = digits.length > 2 ? `${digits.slice(0, 2)}/${digits.slice(2)}` : digits;
  }

  simulatePayment(): void {
    if (!this.canSimulate) {
      this.formMessage = this.selectedMethod === 'CARD'
        ? 'Completa los datos ficticios y verifica que la tarjeta de prueba sea Visa, Mastercard, American Express o Diners Club.'
        : 'Completa el celular y el código ficticios de seis dígitos para simular Yape.';
      return;
    }
    this.formMessage = '';
    this.simulationComplete = true;
  }

  get attentionName(): string {
    return this.appointment?.servicio ?? this.appointment?.especialidades?.join(', ') ?? 'Atención médica';
  }

  get providerName(): string {
    return this.appointment?.doctor ?? 'Servicio complementario';
  }
}
