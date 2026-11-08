import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { InternService, InternUser } from '../../restauration/services/impl/intern.service';
import { Router } from '@angular/router';
import { EmployeeContextService } from '../services/employee-context.service';

@Component({
  selector: 'app-employee-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './employee-list-component.html',
  styleUrl: './employee-list-component.css',
})
export class EmployeeListComponent implements OnInit {

  ngOnInit(): void {
    this.loadUsers();
  }

  sortColumn: keyof InternUser | null = 'firstName';
  sortDirection: 'asc' | 'desc' = 'asc';

  readonly pageSize = 10;
  currentPage = signal(1);

  private readonly internService = inject(InternService);
  private readonly router = inject(Router);
  private readonly employeeContext = inject(EmployeeContextService);

  allUsers = signal<InternUser[]>([]);
  searchQuery = signal('');

  filteredUsers = computed(() => {
    const q = this.searchQuery().toLowerCase().trim();
    if (!q) return this.allUsers();
    return this.allUsers().filter(emp =>
      emp.username?.toLowerCase().includes(q) ||
      emp.firstName?.toLowerCase().includes(q) ||
      emp.lastName?.toLowerCase().includes(q) ||
      emp.email?.toLowerCase().includes(q)
    );
  });

  pagedUsers = computed(() => {
    const start = (this.currentPage() - 1) * this.pageSize;
    return this.filteredUsers().slice(start, start + this.pageSize);
  });

  totalPages = computed(() =>
    Math.ceil(this.filteredUsers().length / this.pageSize)
  );

  pageNumbers = computed(() =>
    Array.from({ length: this.totalPages() }, (_, i) => i + 1)
  );

  loadUsers(): void {
    this.internService.getAllUsers().subscribe({
      next: (users) => {
        this.allUsers.set(users);
        this.currentPage.set(1);
      },
    });
  }

  goToDetail(user: InternUser): void {
    this.employeeContext.setCurrentEmployee(user);
    this.router.navigate(['/rh-admin/employee-profile', user.id], {
    state: { user },});
    }

  onSearch(): void {
    this.currentPage.set(1);
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages()) {
      this.currentPage.set(page);
    }
  }

  sortBy(column: keyof InternUser) {
    if (this.sortColumn === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDirection = 'asc';
    }
    const sorted = [...this.allUsers()].sort((a, b) => {
      const aVal = a[column];
      const bVal = b[column];
      const cmp = String(aVal ?? '').localeCompare(String(bVal ?? ''));
      return this.sortDirection === 'asc' ? cmp : -cmp;
    });
    this.allUsers.set(sorted);
    this.currentPage.set(1);
  }

  getSortIcon(column: keyof InternUser): string {
    if (this.sortColumn !== column) return 'none';
    return this.sortDirection === 'asc' ? 'up' : 'down';
  }
}
