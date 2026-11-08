import { Component, OnInit, inject, signal } from '@angular/core';
import { AbsenceService, AbsenceType } from '../services/absence.service';

@Component({
  selector: 'app-absence-type-admin',
  standalone: true,
  imports: [],
  templateUrl: './absence-type-admin-component.html',
})
export class AbsenceTypeAdminComponent implements OnInit {
  private readonly absenceService = inject(AbsenceService);

  readonly absenceTypes = signal<AbsenceType[]>([]);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.absenceService.getAbsenceTypes().subscribe({
      next: (types) => {
        this.absenceTypes.set(types);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
