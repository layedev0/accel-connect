import { TestBed } from '@angular/core/testing';
import { ChatServiceImpl } from './chat.service';


describe('ChatService', () => {
  let service: ChatServiceImpl;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ChatServiceImpl);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
