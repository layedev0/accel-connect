import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-employee-profile-layout',
  standalone: true,
  imports: [RouterModule],
  template: '<router-outlet />',
})
export class EmployeeProfileLayoutComponent {}
