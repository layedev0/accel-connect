import { Component, signal } from '@angular/core';
import { RouterModule } from '@angular/router';
import { EmployeePortalSidebarComponent } from '../employee-portal-sidebar/employee-portal-sidebar-component';

@Component({
  selector: 'app-employee-portal-layout',
  standalone: true,
  imports: [RouterModule, EmployeePortalSidebarComponent],
  templateUrl: './employee-portal-layout-component.html',
  styleUrl: './employee-portal-layout-component.css',
})
export class EmployeePortalLayoutComponent {
  mobileSidebarOpen = signal(false);
}
