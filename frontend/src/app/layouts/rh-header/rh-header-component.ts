import { Component, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';

export interface RhHeaderTab {
  label: string;
  route: string;
  active?: boolean;
}

@Component({
  selector: 'app-rh-header',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './rh-header-component.html',
  styleUrl: './rh-header-component.css',
})
export class RhHeaderComponent implements OnInit, OnDestroy {
  private readonly router = inject(Router);
  private sub: ReturnType<typeof this.router.events.subscribe> | null = null;

  isEmployeeProfile = signal(false);

  /** Onglets affichés sur la liste employés / congés / qualifications */
  mainTabs: RhHeaderTab[] = [
    { label: 'Dashboard', route: '/rh-admin/dashboard' },
    { label: 'Liste Employés', route: '/rh-admin/employees' },
    { label: 'Congé', route: '/rh-admin/leave' },
    { label: 'Compétences', route: '/rh-admin/qualifications' },
  ];

  /** Base des onglets profil (la route sera suffixée avec /:userId ou /:userId/personal-info) */
  profileTabConfigs: { label: string; path: string }[] = [
    { label: 'Profil', path: '' },
    { label: 'Informations personnelles', path: 'personal-info' },
    { label: 'Contacts d\'urgence', path: 'dependents' },
    { label: 'Compétences', path: 'skills' },
    { label: 'Documents', path: 'documents' },
  ];

  ngOnInit(): void {
    this.updateProfileFlag();
    this.sub = this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe(() => this.updateProfileFlag());
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  private updateProfileFlag(): void {
    this.isEmployeeProfile.set(
      this.router.url.includes('/rh-admin/employee-profile')
    );
  }

  /** Extrait le userId depuis l'URL (ex: /rh-admin/employee-profile/123/personal-info → 123) */
  getProfileUserId(): string | null {
    const match = this.router.url.match(/\/employee-profile\/(\d+)/);
    return match ? match[1] : null;
  }

  /** Routes des onglets profil avec le userId actuel */
  get profileTabs(): RhHeaderTab[] {
    const userId = this.getProfileUserId();
    const base = '/rh-admin/employee-profile';
    if (!userId) {
      return this.profileTabConfigs.map((c) => ({
        ...c,
        route: c.path === '#' ? '#' : `${base}`,
      }));
    }
    return this.profileTabConfigs.map((c) => ({
      ...c,
      route: c.path === '#' ? '#' : c.path ? `${base}/${userId}/${c.path}` : `${base}/${userId}`,
    }));
  }

  backToEmployeeList(): void {
    this.router.navigate(['/rh-admin/employees']);
  }

  isProfileTabActive(tab: RhHeaderTab): boolean {
    if (tab.route === '#') return false;
    const url = this.router.url.split('?')[0];
    // Profil tab: active only when on base URL (no child path like personal-info or dependents)
    if (tab.route.match(/\/employee-profile\/\d+$/) && !tab.route.includes('/personal-info') && !tab.route.includes('/dependents')) {
      return url === tab.route || url === tab.route + '/';
    }
    return url === tab.route || url.startsWith(tab.route + '/');
  }
}
