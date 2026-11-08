import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ListeRestaurant } from './liste-restaurant';

describe('ListeRestaurant', () => {
  let component: ListeRestaurant;
  let fixture: ComponentFixture<ListeRestaurant>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ListeRestaurant]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ListeRestaurant);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
