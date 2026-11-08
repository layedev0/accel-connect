import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { KeycloakService } from '../../features/auth/service/keycloak.service';
import { UserService, User } from '../../features/auth/service/user.service';

export interface RhNavChild {
  label: string;
  route: string;
}

export interface RhNavItem {
  label: string;
  route?: string;
  iconPath: string;
  children?: RhNavChild[];
}

@Component({
  selector: 'app-rh-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './rh-sidebar-component.html',
  styleUrl: './rh-sidebar-component.css',
})
export class RhSidebarComponent implements OnInit {
  private readonly keycloakService = inject(KeycloakService);
  private readonly userService = inject(UserService);

  collapsed = signal(false);
  avatarUrl: string | null = null;
  currentUser = signal<User | null>(null);
  keycloakInfo = signal<{
    username?: string;
    firstName?: string;
    lastName?: string;
  } | null>(null);

  /** Label of the currently open accordion group (null = none open) */
  openGroup = signal<string | null>(null);

  navItems: RhNavItem[] = [
    {
      label: 'Dashboard',
      route: '/rh-admin/dashboard',
      iconPath:
        'M3 12l9-9 9 9M4 10.5V21h5v-6h6v6h5V10.5',
    },
    {
      label: 'Gestion des Employés',
      route: '/rh-admin/employees',
      iconPath:
        'M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z',
    },
    {
      label: 'Congés',
      iconPath:
        'M21 13.255A23.931 23.931 0 0112 15c-3.183 0-6.22-.62-9-1.745M16 6V4a2 2 0 00-2-2h-4a2 2 0 00-2 2v2m4 6h.01M5 20h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z',
      children: [
        { label: 'Types de congés', route: '/rh-admin/leave' },
        { label: 'Soldes de congés', route: '/rh-admin/leave-balances' },
        { label: 'Congés approuvés', route: '/rh-admin/approved-leaves' },
        { label: 'Congés rejetés', route: '/rh-admin/rejected-leaves' },
      ],
    },

    {
      label: 'Absences',
      iconPath:
        'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z',
      children: [
        { label: "Types d'absence", route: '/rh-admin/absence-types' },
        { label: 'Absences approuvées', route: '/rh-admin/approved-absences' },
        { label: 'Absences rejetées', route: '/rh-admin/rejected-absences' },
      ],
    },
    {
      label: 'Calendrier',
      route: '/rh-admin/calendar',
      iconPath:
        'M8 2v3M16 2v3M3.5 9h17M5 5h14a2 2 0 012 2v12a2 2 0 01-2 2H5a2 2 0 01-2-2V7a2 2 0 012-2z',
    },
    {
      label: 'Départements',
      route: '/rh-admin/departments',
      iconPath:
        'M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4',
    },
    {
      label: 'Compétences',
      route: '/rh-admin/qualifications',
      iconPath:
        'M9 12l2 2 4-4M7.835 4.697a3.42 3.42 0 001.946-.806 3.42 3.42 0 014.438 0 3.42 3.42 0 001.946.806 3.42 3.42 0 013.138 3.138 3.42 3.42 0 00.806 1.946 3.42 3.42 0 010 4.438 3.42 3.42 0 00-.806 1.946 3.42 3.42 0 01-3.138 3.138 3.42 3.42 0 00-1.946.806 3.42 3.42 0 01-4.438 0 3.42 3.42 0 00-1.946-.806 3.42 3.42 0 01-3.138-3.138 3.42 3.42 0 00-.806-1.946 3.42 3.42 0 010-4.438 3.42 3.42 0 00.806-1.946 3.42 3.42 0 013.138-3.138z',
    },
    {
      label: 'Emplois',
      iconPath:
        'M21 13.255A23.931 23.931 0 0112 15c-3.183 0-6.22-.62-9-1.745M16 6V4a2 2 0 00-2-2h-4a2 2 0 00-2 2v2m4 6h.01M5 20h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z',
      children: [
        { label: 'Intitulés de poste', route: '/rh-admin/job-titles' },
        { label: 'Catégories', route: '/rh-admin/job-categories' },
      ],
    },
    
  ];

  ngOnInit() {
    if (this.keycloakService.isAuthenticated()) {
      this.keycloakInfo.set(this.keycloakService.getUserInfo());
      this.loadCurrentUser();
    }
  }

  private loadCurrentUser() {
    this.userService.getCurrentUser().subscribe({
      next: (user) => {
        this.currentUser.set(user);
        this.avatarUrl = user.avatarUrl ?? this.generateAvatar(user);
      },
      error: () => {
        const info = this.keycloakInfo();
        if (info?.firstName || info?.lastName) {
          this.avatarUrl = this.generateAvatarFromKeycloak(info);
        }
      },
    });
  }

  private generateAvatar(user: User): string {
    const name = `${user.firstName || ''} ${user.lastName || ''}`.trim() || user.username;
    return `https://ui-avatars.com/api/?name=${encodeURIComponent(name)}&background=25509d&color=fff&bold=true&size=128`;
  }

  private generateAvatarFromKeycloak(info: {
    firstName?: string;
    lastName?: string;
    username?: string;
  }): string {
    const name =
      `${info.firstName || ''} ${info.lastName || ''}`.trim() || info.username || 'User';
    return `https://ui-avatars.com/api/?name=${encodeURIComponent(name)}&background=25509d&color=fff&bold=true&size=128`;
  }

  toggleCollapse() {
    this.collapsed.update((v) => !v);
  }

  toggleGroup(label: string): void {
    this.openGroup.update((current) => (current === label ? null : label));
  }

  isGroupOpen(label: string): boolean {
    return this.openGroup() === label;
  }

  closeGroup(): void {
    this.openGroup.set(null);
  }

  getDisplayName(): string {
    const user = this.currentUser();
    if (user) {
      return `${user.firstName || ''} ${user.lastName || ''}`.trim() || user.username;
    }
    const keycloak = this.keycloakInfo();
    if (keycloak) {
      return (
        `${keycloak.firstName || ''} ${keycloak.lastName || ''}`.trim() ||
        keycloak.username ||
        'Utilisateur'
      );
    }
    return 'Utilisateur';
  }

  getDisplayRole(): string {
    return 'DRH'; 
  }
}
