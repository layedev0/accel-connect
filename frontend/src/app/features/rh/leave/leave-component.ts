import { Component, OnInit, inject, signal } from '@angular/core';
import { LeaveService, LeaveType } from '../services/leave.service';

@Component({
  selector: 'app-leave',
  standalone: true,
  imports: [],
  templateUrl: './leave-component.html',
  styleUrl: './leave-component.css',
})
export class LeaveComponent implements OnInit {
  private readonly leaveService = inject(LeaveService);

  readonly leaveTypes = signal<LeaveType[]>([]);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.leaveService.getLeaveTypes().subscribe({
      next: (types) => {
        this.leaveTypes.set(types);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  deductionLabel(rule: string): string {
    switch (rule) {
      case 'FULL':        return 'Déduction totale';
      case 'EXCESS_ONLY': return 'Excédent seulement';
      case 'NONE':        return 'Aucune déduction';
      default:            return rule;
    }
  }
}
