import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { AppConfig } from '../../core/config/app.config';

@Component({
  selector: 'app-coming-soon',
  standalone: true,
  imports: [RouterModule],
  templateUrl: './coming-soon-component.html',
})
export class ComingSoonComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);

  moduleLabel = 'Ce module';
  moduleIcon = 'assets/gif/ressources.gif';

  private readonly routeToModule: Record<string, { label: string; icon: string }> = {
    '/ressources': { label: 'Ressources', icon: 'assets/gif/ressources.gif' },
    '/rh':         { label: 'Ressources Humaines', icon: 'assets/gif/rh.gif' },
    '/commercial': { label: 'Commercial', icon: 'assets/gif/commercial.gif' },
    '/moyens':     { label: 'Moyens Généraux', icon: 'assets/gif/mg.gif' },
    '/formation':  { label: 'Formation', icon: 'assets/gif/formation.gif' },
  };

  ngOnInit(): void {
    const url = '/' + (this.route.snapshot.url[0]?.path ?? '');
    const match = this.routeToModule[url];
    if (match) {
      this.moduleLabel = match.label;
      this.moduleIcon = match.icon;
    }
  }
}
