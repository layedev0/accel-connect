import { Injectable, signal, computed } from '@angular/core';
import { InternUser } from '../../restauration/services/impl/intern.service';

/**
 * Contexte employé partagé entre les vues Profil, Informations personnelles, etc.
 */
@Injectable({
  providedIn: 'root',
})
export class EmployeeContextService {
  private readonly currentEmployee = signal<InternUser | null>(null);

  readonly employee = computed(() => this.currentEmployee());

  setCurrentEmployee(user: InternUser | null): void {
    this.currentEmployee.set(user);
  }

  clear(): void {
    this.currentEmployee.set(null);
  }
}
