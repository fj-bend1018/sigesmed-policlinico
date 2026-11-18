import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { AppointmentApiService } from '../../core/appointments/appointment-api.service';
import { WorkspaceApiService } from '../../core/workspaces/workspace-api.service';
import { RolePortalComponent } from './role-portal.component';

describe('RolePortalComponent (CN-BUS-003)', () => {
  let receptionSpy: jasmine.Spy;

  beforeEach(async () => {
    receptionSpy = jasmine.createSpy('receptionAppointments').and.returnValue(of([]));

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
            receptionAppointments: receptionSpy
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
});
