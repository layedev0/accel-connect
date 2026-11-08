import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { ArticleDetailComponent } from './article-detail-component';
import { ArticleService } from '../services/article.service';

describe('ArticleDetailComponent', () => {
  let component: ArticleDetailComponent;
  let fixture: ComponentFixture<ArticleDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ArticleDetailComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: () => '1',
              },
            },
          },
        },
        {
          provide: ArticleService,
          useValue: {
            list: () =>
              of([
                {
                  id: 1,
                  title: 'Article',
                  content: 'Contenu de test',
                  articleType: 'RECHERCHE',
                  documentName: 'doc.pdf',
                  documentType: 'application/pdf',
                  documentSize: 1200,
                  hasCoverImage: false,
                },
              ]),
            downloadCover: () => of(new Blob()),
            getComments: () => of([]),
            getLikeStatus: () => of({ likeCount: 0, likedByCurrentUser: false }),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ArticleDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

