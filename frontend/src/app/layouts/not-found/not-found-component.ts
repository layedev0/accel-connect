import { Component } from '@angular/core';
import { LottieComponent, AnimationOptions } from 'ngx-lottie';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-not-found-component',
  imports: [LottieComponent, RouterModule],
  templateUrl: './not-found-component.html',
  styleUrl: './not-found-component.css',
})
export class NotFoundComponent {
  lottieOptions: AnimationOptions = {
    path: '/assets/lottie/404.json',
    loop: true,
    autoplay: true,
    rendererSettings: {
      preserveAspectRatio: 'xMidYMid slice', // centré et rempli le conteneur
    },
  };
}
