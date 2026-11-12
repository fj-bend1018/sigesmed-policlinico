import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Appointment, AppointmentApiService } from '../../core/appointments/appointment-api.service';

@Component({
  standalone: true, selector: 'app-confirmation', imports: [CommonModule, RouterLink],
  templateUrl: './confirmation.component.html'
})
export class ConfirmationComponent implements OnInit {
  appointment: Appointment | null = null;
  loading = true;
  constructor(private readonly route: ActivatedRoute, private readonly api: AppointmentApiService) { }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isSafeInteger(id) || id <= 0) { this.loading = false; return; }
    this.api.appointment(id).subscribe({
      next: appointment => { this.appointment = appointment; this.loading = false; },
      error: () => this.loading = false
    });
  }
}
