import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AddFoodToRestauComponent } from './add-food-to-restau-component';

describe('AddFoodToRestauComponent', () => {
  let component: AddFoodToRestauComponent;
  let fixture: ComponentFixture<AddFoodToRestauComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AddFoodToRestauComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AddFoodToRestauComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
