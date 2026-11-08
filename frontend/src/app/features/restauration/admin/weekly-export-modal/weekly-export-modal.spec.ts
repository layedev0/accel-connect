import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WeeklyExportModal } from './weekly-export-modal';

describe('WeeklyExportModal', () => {
  let component: WeeklyExportModal;
  let fixture: ComponentFixture<WeeklyExportModal>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WeeklyExportModal]
    })
    .compileComponents();

    fixture = TestBed.createComponent(WeeklyExportModal);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
