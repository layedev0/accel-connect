import { Component } from '@angular/core';
import { RouterModule } from "@angular/router";
import { AdminNavComponent } from "../admin-nav/admin-nav-component";

@Component({
  selector: 'app-admin-layout-component',
  imports: [RouterModule, AdminNavComponent],
  templateUrl: './admin-layout-component.html',
  styleUrl: './admin-layout-component.css'
})
export class AdminLayoutComponent {

}
