import { Component, OnInit, signal, inject, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { KeycloakService } from '../../features/auth/service/keycloak.service';
import { UserService, User } from '../../features/auth/service/user.service';

interface PortalNavItem {
  label: string;
  route: string;
  iconPath: string;
}

@Component({
  selector: 'app-employee-portal-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './employee-portal-sidebar-component.html',
  styleUrl: './employee-portal-sidebar-component.css',
})
export class EmployeePortalSidebarComponent implements OnInit {
  private readonly keycloakService = inject(KeycloakService);
  private readonly userService = inject(UserService);

  collapsed = signal(false);
  avatarUrl: string | null = null;
  currentUser = signal<User | null>(null);

  @Input() mobileOpen = false;
  @Output() mobileClose = new EventEmitter<void>();

  navItems: PortalNavItem[] = [
    {
      label: 'Profil',
      route: '/rh/mon-profil',
      iconPath: 'M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z',
    },

     {
      label: 'Mes Congés',
      route: '/rh/conges',
      iconPath: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z',
    },

     {
      label: 'Mes Absences',
      route: '/rh/absences',
      iconPath: 'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z',
    },

    {
      label: 'Contacts d\'urgence',
      route: '/rh/mes-contacts',
      iconPath: 'M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z',
    },
    {
      label: 'Compétences',
      route: '/rh/mes-competences',
      iconPath: 'M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z',
    },
   
    {
      label: 'Documents',
      route: '/rh/mes-documents',
      iconPath: 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z',
    },
   
  ];

  ngOnInit(): void {
    if (this.keycloakService.isAuthenticated()) {
      this.loadCurrentUser();
      if (this.keycloakService.hasAnyRole(['MANAGER', 'HR_MANAGER'])) {
        this.navItems.splice(3, 0,
            {
              label: 'Approbation des congés',
              route: '/rh/approbation-conges',
              iconPath: 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z',
            },
            {
              label: 'Approbation des absences',
              route: '/rh/approbation-absences',
              iconPath: 'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z',
            }
          );

      }
    }
  }

  private loadCurrentUser(): void {
    this.userService.getCurrentUser().subscribe({
      next: (user) => {
        this.currentUser.set(user);
        this.avatarUrl = user.avatarUrl ?? this.generateAvatar(user);
      },
    });
  }

  private generateAvatar(user: User): string {
    const name = `${user.firstName || ''} ${user.lastName || ''}`.trim() || user.username;
    return `https://ui-avatars.com/api/?name=${encodeURIComponent(name)}&background=25509d&color=fff&bold=true&size=128`;
  }

  toggleCollapse(): void {
    this.collapsed.update((v) => !v);
  }

  onNavItemClick(): void {
    if (this.mobileOpen) {
      this.mobileClose.emit();
    }
  }

  getDisplayName(): string {
    const user = this.currentUser();
    if (user) {
      return `${user.firstName || ''} ${user.lastName || ''}`.trim() || user.username;
    }
    return 'Utilisateur';
  }
}
