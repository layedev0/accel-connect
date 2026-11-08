import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-super-admin-header',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './super-admin-header-component.html',
  styleUrl: './super-admin-header-component.css',
})
export class SuperAdminHeaderComponent {}
