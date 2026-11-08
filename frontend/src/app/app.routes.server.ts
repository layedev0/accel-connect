import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  {
    path: 'rh-admin/employee-profile/:userId',
    renderMode: RenderMode.Server
  },
  {
    path: 'rh-admin/employee-profile/:userId/personal-info',
    renderMode: RenderMode.Server
  },
  {
    path: 'rh-admin/employee-profile/:userId/skills',
    renderMode: RenderMode.Server
  },
  {
    path: 'rh-admin/employee-profile/:userId/dependents',
    renderMode: RenderMode.Server
  },
  {
    path: 'rh-admin/employee-profile/:userId/documents',
    renderMode: RenderMode.Server
  },
  {
    path: 'actualites/:id',
    renderMode: RenderMode.Server
  },
  {
    path: '**',
    renderMode: RenderMode.Prerender
  }
];
