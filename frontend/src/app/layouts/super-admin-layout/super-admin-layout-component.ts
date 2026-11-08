import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SuperAdminSidebarComponent } from '../super-admin-sidebar/super-admin-sidebar-component';
import { SuperAdminHeaderComponent } from '../super-admin-header/super-admin-header-component';

@Component({
  selector: 'app-super-admin-layout',
  standalone: true,
  imports: [RouterModule, SuperAdminSidebarComponent, SuperAdminHeaderComponent],
  templateUrl: './super-admin-layout-component.html',
  styleUrl: './super-admin-layout-component.css',
  host: {
    class: 'flex-1 flex flex-col min-h-0 block',
  },
})
export class SuperAdminLayoutComponent {}
