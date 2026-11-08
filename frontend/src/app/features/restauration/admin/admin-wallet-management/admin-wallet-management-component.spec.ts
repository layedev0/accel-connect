import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminWalletManagementComponent } from './admin-wallet-management.component';

describe('AdminWalletManagementComponent', () => {
  let component: AdminWalletManagementComponent;
  let fixture: ComponentFixture<AdminWalletManagementComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminWalletManagementComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdminWalletManagementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
