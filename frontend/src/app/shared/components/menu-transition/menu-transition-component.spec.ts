import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MenuTransitionComponent } from './menu-transition-component';

describe('MenuTransitionComponent', () => {
  let component: MenuTransitionComponent;
  let fixture: ComponentFixture<MenuTransitionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MenuTransitionComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MenuTransitionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
