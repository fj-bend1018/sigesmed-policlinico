import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { AppointmentApiService } from '../../core/appointments/appointment-api.service';
import { WorkspaceApiService } from '../../core/workspaces/workspace-api.service';
import { RolePortalComponent } from './role-portal.component';

describe('RolePortalComponent (CN-BUS-003)', () => {
  let receptionSpy: jasmine.Spy;
  let recurringSpy: jasmine.Spy;
  let schedulesSpy: jasmine.Spy;

  beforeEach(async () => {
    receptionSpy = jasmine.createSpy('receptionAppointments').and.returnValue(of([]));
    recurringSpy = jasmine.createSpy('createRecurringSchedules').and.returnValue(of({
      doctorId: 1,
      doctor: 'Dr. Demo',
      creados: 8,
      horarios: []
    }));
    schedulesSpy = jasmine.createSpy('schedules').and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [RolePortalComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              data: { role: 'RECEPCIONISTA', section: 'appointments' },
              paramMap: convertToParamMap({})
            }
          }
        },
        {
          provide: WorkspaceApiService,
          useValue: {
            receptionAppointments: receptionSpy,
            createRecurringSchedules: recurringSpy,
            schedules: schedulesSpy,
            doctors: () => of([])
          }
        },
        {
          provide: AppointmentApiService,
          useValue: {}
        },
        {
          provide: AuthService,
          useValue: {
            currentSession: () => ({ role: 'RECEPCIONISTA', identifier: '48291637' })
          }
        }
      ]
    }).compileComponents();
  });

  it('rejects invalid DNIs (7000001, 7000A001) without calling API and allows valid DNI, name, and email with digits', () => {
    const fixture = TestBed.createComponent(RolePortalComponent);
    const comp = fixture.componentInstance;
    receptionSpy.calls.reset();

    comp.searchText = '7000001';
    comp.loadAppointments();
    expect(comp.errorMessage).toBe('El DNI debe tener 8 dígitos numéricos.');
    expect(receptionSpy).not.toHaveBeenCalled();

    comp.searchText = '7000A001';
    comp.loadAppointments();
    expect(comp.errorMessage).toBe('El DNI debe tener 8 dígitos numéricos.');
    expect(receptionSpy).not.toHaveBeenCalled();

    comp.searchText = '70000001';
    comp.loadAppointments();
    expect(comp.errorMessage).toBe('');
    expect(receptionSpy).toHaveBeenCalledWith(undefined, undefined, '70000001');

    comp.searchText = 'Benítez';
    comp.loadAppointments();
    expect(comp.errorMessage).toBe('');
    expect(receptionSpy).toHaveBeenCalledWith(undefined, undefined, 'Benítez');

    comp.searchText = 'ana2026@sigesmed.local';
    comp.loadAppointments();
    expect(comp.errorMessage).toBe('');
    expect(receptionSpy).toHaveBeenCalledWith(undefined, undefined, 'ana2026@sigesmed.local');
  });

  it('generates recurring schedules with weekdays and rest dates (CN-MED-004)', () => {
    const fixture = TestBed.createComponent(RolePortalComponent);
    const comp = fixture.componentInstance;

    comp.toggleRecurringMode();
    expect(comp.recurringMode).toBeTrue();

    comp.recurringForm.doctorId = '1';
    comp.recurringForm.diasSemana = ['MONDAY', 'WEDNESDAY'];
    comp.recurringForm.horaInicio = '08:00';
    comp.recurringForm.horaFin = '12:00';
    comp.recurringForm.fechaDesde = '2026-11-23';
    comp.recurringForm.fechaHasta = '2026-11-27';
    comp.recurringForm.nuevaFechaDescanso = '2026-11-27';
    comp.addRestDate();

    comp.saveRecurringSchedules();
    expect(recurringSpy).toHaveBeenCalledWith({
      doctorId: 1,
      diasSemana: ['MONDAY', 'WEDNESDAY'],
      horaInicio: '08:00',
      horaFin: '12:00',
      fechaDesde: '2026-11-23',
      fechaHasta: '2026-11-27',
      diasDescanso: ['2026-11-27']
    });
    expect(comp.successMessage).toContain('Se crearon 8 horarios recurrentes');
    expect(comp.recurringMode).toBeFalse();
  });
});
