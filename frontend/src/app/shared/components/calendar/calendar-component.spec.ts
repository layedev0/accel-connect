import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PrettyCalendarComponent } from './calendar-component';

describe('PrettyCalendarComponent', () => {
  let component: PrettyCalendarComponent;
  let fixture: ComponentFixture<PrettyCalendarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PrettyCalendarComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PrettyCalendarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
