import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FormFoodComponent } from './form-food-component';

describe('FormFoodComponent', () => {
  let component: FormFoodComponent;
  let fixture: ComponentFixture<FormFoodComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FormFoodComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(FormFoodComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
