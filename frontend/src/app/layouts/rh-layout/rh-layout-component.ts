import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { RhSidebarComponent } from '../rh-sidebar/rh-sidebar-component';
import { RhHeaderComponent } from '../rh-header/rh-header-component';

@Component({
  selector: 'app-rh-layout',
  standalone: true,
  imports: [RouterModule, RhSidebarComponent, RhHeaderComponent],
  templateUrl: './rh-layout-component.html',
  styleUrl: './rh-layout-component.css',
  host: {
    class: 'flex-1 flex flex-col min-h-0 block',
  },
})
export class RhLayoutComponent {}
