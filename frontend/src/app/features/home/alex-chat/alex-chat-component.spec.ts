import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AlexChatComponent } from './alex-chat-component';

describe('AlexChatComponent', () => {
  let component: AlexChatComponent;
  let fixture: ComponentFixture<AlexChatComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AlexChatComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AlexChatComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
