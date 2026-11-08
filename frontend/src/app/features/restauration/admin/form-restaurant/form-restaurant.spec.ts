import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FormRestaurant } from './form-restaurant';

describe('FormRestaurant', () => {
  let component: FormRestaurant;
  let fixture: ComponentFixture<FormRestaurant>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FormRestaurant]
    })
    .compileComponents();

    fixture = TestBed.createComponent(FormRestaurant);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
