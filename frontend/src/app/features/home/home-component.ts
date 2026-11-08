import { Component } from '@angular/core';
import { FirstSectionComponent } from './sections/first-section/first-section-component';
import { SecondSectionComponent } from "./sections/second-section/second-section-component";
import { ThirdSectionComponent } from "./sections/third-section/third-section-component";

@Component({
  selector: 'app-home-component',
  imports: [FirstSectionComponent, SecondSectionComponent, ThirdSectionComponent],
  templateUrl: './home-component.html',
  styleUrl: './home-component.css',
})
export class HomeComponent {}
