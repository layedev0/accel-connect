import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { SecondSectionComponent } from './second-section-component';
import { EmployeeDetailsService } from '../../../rh/services/employee-details.service';
import { ArticleService } from '../../services/article.service';

describe('SecondSectionComponent', () => {
  let component: SecondSectionComponent;
  let fixture: ComponentFixture<SecondSectionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SecondSectionComponent],
      providers: [
        provideRouter([]),
        {
          provide: EmployeeDetailsService,
          useValue: {
            getUpcomingBirthdays: () => of([]),
          },
        },
        {
          provide: ArticleService,
          useValue: {
            list: () => of([]),
            downloadCover: () => of(new Blob()),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SecondSectionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
