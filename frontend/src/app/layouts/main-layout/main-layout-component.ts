import { Component } from '@angular/core';
import { NavComponent } from "../nav/nav-component";
import { RouterModule } from "@angular/router";
import { FooterComponent } from "../footer/footer-component";

@Component({
  selector: 'app-main-layout-component',
  imports: [NavComponent, RouterModule, FooterComponent],
  templateUrl: './main-layout-component.html',
  styleUrl: './main-layout-component.css'
})
export class MainLayoutComponent {

}
