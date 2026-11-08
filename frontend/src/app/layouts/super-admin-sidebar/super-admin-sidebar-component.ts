import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

export interface SaNavItem {
  label: string;
  route: string;
  iconPath: string;
}

@Component({
  selector: 'app-super-admin-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './super-admin-sidebar-component.html',
  styleUrl: './super-admin-sidebar-component.css',
})
export class SuperAdminSidebarComponent {
  collapsed = signal(false);

  /** Super Admin navigation only — no RH links. */
  navItems: SaNavItem[] = [
    {
      label: 'Articles',
      route: '/super-admin/articles',
      iconPath:
        'M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 13a2 2 0 01-2-2V7m2 13a2 2 0 002-2V9a2 2 0 00-2-2h-2m-4-3H9M7 16h6M7 8h6v4H7V8z',
    },
  ];

  toggleCollapse(): void {
    this.collapsed.update((v) => !v);
  }

  getDisplayRole(): string {
    return 'Super Admin';
  }
}
