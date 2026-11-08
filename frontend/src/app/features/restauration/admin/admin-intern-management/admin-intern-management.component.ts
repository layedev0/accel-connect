import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { InternService, InternUser } from '../../services/impl/intern.service';
import { NotificationService } from '../../../../core/services/impl/notification.service';

@Component({
  selector: 'app-admin-intern-management',
  standalone: true,
  imports: [CommonModule, FormsModule, FontAwesomeModule],
  templateUrl: './admin-intern-management-component.html',
  styleUrl: './admin-intern-management-component.css',
})
export class AdminInternManagementComponent implements OnInit {
  private readonly internService = inject(InternService);
  private readonly notificationService = inject(NotificationService);

  users = signal<InternUser[]>([]);
  filteredUsers = signal<InternUser[]>([]);
  isLoading = signal(false);
  searchTerm = '';
  activeFilter = signal<'all' | 'interns'>('all');
  processingUserId = signal<number | null>(null);

  totalInterns = signal(0);
  totalNonInterns = signal(0);

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.isLoading.set(true);
    this.internService.getAllUsers().subscribe({
      next: (users) => {
        this.users.set(users);
        this.filteredUsers.set(users);
        this.calculateStats(users);
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Erreur lors du chargement des utilisateurs:', error);
        this.isLoading.set(false);
        this.notificationService.error(
          'Erreur',
          'Impossible de charger les utilisateurs'
        );
      },
    });
  }

  private calculateStats(users: InternUser[]): void {
    this.totalInterns.set(users.filter((u) => u.intern).length);
    this.totalNonInterns.set(users.filter((u) => !u.intern).length);
  }

  onSearch(): void {
    this.applyFilters();
  }

  onFilterChange(filter: 'all' | 'interns'): void {
    this.activeFilter.set(filter);
    this.applyFilters();
  }

  private applyFilters(): void {
    const term = this.searchTerm.toLowerCase();
    const filter = this.activeFilter();
    let list = this.users();

    if (filter === 'interns') {
      list = list.filter((u) => u.intern);
    }

    if (term) {
      list = list.filter(
        (u) =>
          (u.firstName || '').toLowerCase().includes(term) ||
          (u.lastName || '').toLowerCase().includes(term) ||
          (u.username || '').toLowerCase().includes(term)
      );
    }

    this.filteredUsers.set(list);
  }

  getDisplayName(user: InternUser): string {
    const first = user.firstName || '';
    const last = user.lastName || '';
    const name = `${first} ${last}`.trim();
    return name || user.username || 'Utilisateur';
  }

  toggleInternStatus(user: InternUser): void {
    const displayName = this.getDisplayName(user);
    const action = user.intern ? 'retirer du statut stagiaire' : 'marquer comme stagiaire';
    const newSubsidy = user.intern ? '1 000' : '2 000';

    if (!confirm(`Confirmer: ${action} ${displayName} ?\nSubvention: ${newSubsidy} FCFA`)) {
      return;
    }

    this.processingUserId.set(user.id);

    const request$ = user.intern
      ? this.internService.removeIntern(user.id)
      : this.internService.setIntern(user.id);

    request$.subscribe({
      next: (response) => {
        this.notificationService.success('Succes', response.message);
        this.processingUserId.set(null);
        this.loadUsers();
      },
      error: (error) => {
        console.error('Erreur:', error);
        const errorMessage = error.error?.message || 'Erreur lors de la mise a jour';
        this.notificationService.error('Erreur', errorMessage);
        this.processingUserId.set(null);
      },
    });
  }

  formatAmount(amount: number): string {
    return this.internService.formatAmount(amount);
  }
}
