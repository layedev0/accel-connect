import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';
import { environment } from './environments/environment';

// Runtime config: /config.json is overridden on OpenShift by a ConfigMap mount
fetch('/config.json', { cache: 'no-store' })
  .then((res) => (res.ok ? res.json() : {}))
  .catch(() => ({}))
  .then((config) => {
    Object.assign(environment, config);
    return bootstrapApplication(App, appConfig);
  })
  .catch((err) => console.error('❌ Application bootstrap failed:', err));
