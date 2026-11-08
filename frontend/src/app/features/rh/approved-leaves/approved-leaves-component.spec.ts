import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ApprovedLeavesComponent } from './approved-leaves-component';
import { LeaveService, LeaveRequestSummary } from '../services/leave.service';
import { UserService } from '../../auth/service/user.service';
import { KeycloakService } from '../../auth/service/keycloak.service';

const makeSummary = (overrides: Partial<LeaveRequestSummary> = {}): LeaveRequestSummary => ({
  id: 1,
  employeeName: 'Jean Dupont',
  employeeEmail: 'jean@example.com',
  leaveTypeCode: 'ANNUAL_LEAVE',
  leaveTypeName: 'Congé annuel',
  dateApplied: '2025-01-01',
  startDate: '2025-03-10',
  endDate: '2025-03-14',
  totalDays: 5,
  overallStatus: 'Approuvé',
  days: [],
  comments: [],
  ...overrides,
});

describe('ApprovedLeavesComponent', () => {
  let component: ApprovedLeavesComponent;
  let leaveServiceSpy: jasmine.SpyObj<LeaveService>;
  let userServiceSpy: jasmine.SpyObj<UserService>;
  let keycloakServiceSpy: jasmine.SpyObj<KeycloakService>;

  beforeEach(async () => {
    leaveServiceSpy = jasmine.createSpyObj('LeaveService', [
      'getApprovedRequests',
      'cancelApprovedLeaveRequest',
    ]);
    leaveServiceSpy.getApprovedRequests.and.returnValue(of([]));

    userServiceSpy = jasmine.createSpyObj('UserService', ['getCurrentUser']);
    userServiceSpy.getCurrentUser.and.returnValue(of({ id: 1 } as any));

    keycloakServiceSpy = jasmine.createSpyObj('KeycloakService', ['hasAnyRole']);
    keycloakServiceSpy.hasAnyRole.and.returnValue(false);

    await TestBed.configureTestingModule({
      imports: [ApprovedLeavesComponent],
      providers: [
        { provide: LeaveService, useValue: leaveServiceSpy },
        { provide: UserService, useValue: userServiceSpy },
        { provide: KeycloakService, useValue: keycloakServiceSpy },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(ApprovedLeavesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('loads approved requests on init', () => {
    expect(leaveServiceSpy.getApprovedRequests).toHaveBeenCalledTimes(1);
  });

  // ── filteredRequests ──────────────────────────────────────────────────────

  describe('filteredRequests', () => {
    const req1 = makeSummary({ id: 1, employeeName: 'Jean Dupont',   employeeEmail: 'jean@company.com',  startDate: '2025-03-10', endDate: '2025-03-14' });
    const req2 = makeSummary({ id: 2, employeeName: 'Awa Ndiaye',    employeeEmail: 'awa@company.com',   startDate: '2025-04-01', endDate: '2025-04-05' });
    const req3 = makeSummary({ id: 3, employeeName: 'Fatou Fall',    employeeEmail: 'fatou@company.com', startDate: '2025-05-20', endDate: '2025-05-25' });

    beforeEach(() => {
      component.allRequests.set([req1, req2, req3]);
      component.resetFilters();
    });

    it('returns all requests when no filter is active', () => {
      expect(component.filteredRequests().length).toBe(3);
    });

    it('filters by email (case-insensitive, partial match)', () => {
      component.filterEmail.set('AWA');
      expect(component.filteredRequests()).toEqual([req2]);
    });

    it('filters by firstName / name (partial match)', () => {
      component.filterFirstName.set('fat');
      expect(component.filteredRequests()).toEqual([req3]);
    });

    it('filters by start date — excludes leaves ending before the filter start', () => {
      component.filterStartDate.set('2025-04-01');
      const ids = component.filteredRequests().map(r => r.id);
      expect(ids).not.toContain(1); // endDate 2025-03-14 < 2025-04-01
      expect(ids).toContain(2);
      expect(ids).toContain(3);
    });

    it('filters by end date — excludes leaves starting after the filter end', () => {
      component.filterEndDate.set('2025-04-30');
      const ids = component.filteredRequests().map(r => r.id);
      expect(ids).not.toContain(3); // startDate 2025-05-20 > 2025-04-30
      expect(ids).toContain(1);
      expect(ids).toContain(2);
    });

    it('combines email and date range filters', () => {
      component.filterEmail.set('company.com');
      component.filterStartDate.set('2025-04-01');
      component.filterEndDate.set('2025-04-30');
      expect(component.filteredRequests()).toEqual([req2]);
    });

    it('returns empty when no record matches', () => {
      component.filterEmail.set('nobody@nowhere.com');
      expect(component.filteredRequests().length).toBe(0);
    });
  });

  // ── resetFilters ──────────────────────────────────────────────────────────

  describe('resetFilters', () => {
    it('clears all filter signals and resets page to 1', () => {
      component.filterEmail.set('test@test.com');
      component.filterFirstName.set('Jean');
      component.filterStartDate.set('2025-01-01');
      component.filterEndDate.set('2025-12-31');
      component.currentPage.set(3);

      component.resetFilters();

      expect(component.filterEmail()).toBe('');
      expect(component.filterFirstName()).toBe('');
      expect(component.filterStartDate()).toBe('');
      expect(component.filterEndDate()).toBe('');
      expect(component.currentPage()).toBe(1);
    });
  });

  // ── hasActiveFilters ──────────────────────────────────────────────────────

  describe('hasActiveFilters', () => {
    it('returns false when all filters are empty', () => {
      component.resetFilters();
      expect(component.hasActiveFilters()).toBeFalse();
    });

    it('returns true when email filter is set', () => {
      component.filterEmail.set('x');
      expect(component.hasActiveFilters()).toBeTrue();
    });

    it('returns true when date filter is set', () => {
      component.filterStartDate.set('2025-01-01');
      expect(component.hasActiveFilters()).toBeTrue();
    });
  });

  // ── pagination ────────────────────────────────────────────────────────────

  describe('pagination', () => {
    it('totalPages returns 1 when items fit in one page', () => {
      component.allRequests.set([makeSummary()]);
      expect(component.totalPages()).toBe(1);
    });

    it('totalPages rounds up correctly', () => {
      const items = Array.from({ length: 16 }, (_, i) => makeSummary({ id: i + 1 }));
      component.allRequests.set(items);
      expect(component.totalPages()).toBe(2); // pageSize = 15
    });

    it('paged returns only the current page slice', () => {
      const items = Array.from({ length: 20 }, (_, i) => makeSummary({ id: i + 1 }));
      component.allRequests.set(items);
      component.currentPage.set(2);
      const page = component.paged();
      expect(page.length).toBe(5);
      expect(page[0].id).toBe(16);
    });
  });

  // ── cancel modal ──────────────────────────────────────────────────────────

  describe('openCancelModal / closeModal', () => {
    const req = makeSummary();

    it('sets modalRequest and clears comment on open', () => {
      component.modalComment.set('old comment');
      component.openCancelModal(req);
      expect(component.modalRequest()).toEqual(req);
      expect(component.modalComment()).toBe('');
    });

    it('clears modalRequest on close', () => {
      component.openCancelModal(req);
      component.closeModal();
      expect(component.modalRequest()).toBeNull();
    });
  });

  // ── confirmCancel ─────────────────────────────────────────────────────────

  describe('confirmCancel', () => {
    const req = makeSummary({ id: 42, employeeName: 'Jean Dupont' });

    it('calls cancelApprovedLeaveRequest and reloads on success', () => {
      leaveServiceSpy.cancelApprovedLeaveRequest.and.returnValue(of(undefined));
      leaveServiceSpy.getApprovedRequests.and.returnValue(of([]));
      component.openCancelModal(req);

      component.confirmCancel();

      expect(leaveServiceSpy.cancelApprovedLeaveRequest).toHaveBeenCalledWith(42, undefined);
      expect(component.successMsg()).toContain('Jean Dupont');
      expect(component.modalRequest()).toBeNull();
    });

    it('sets errorMsg on failure', () => {
      leaveServiceSpy.cancelApprovedLeaveRequest.and.returnValue(
        throwError(() => ({ error: { message: 'Erreur serveur' } }))
      );
      component.openCancelModal(req);

      component.confirmCancel();

      expect(component.errorMsg()).toBe('Erreur serveur');
      expect(component.modalRequest()).toBeNull();
    });

    it('does nothing when no modal request is set', () => {
      component.closeModal();
      component.confirmCancel();
      expect(leaveServiceSpy.cancelApprovedLeaveRequest).not.toHaveBeenCalled();
    });
  });
});
