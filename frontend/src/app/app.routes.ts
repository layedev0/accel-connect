/**
 * 🌐 Routes de l'application
 * - Section utilisateur : protégée par authGuard
 * - Section admin : protégée par authGuard + adminOnlyGuard
 */
import { Routes } from '@angular/router';
import { HomeComponent } from './features/home/home-component';
import { NotFoundComponent } from './layouts/not-found/not-found-component';
import { PanierComponent } from './features/restauration/panier/panier.component';
import { MainLayoutComponent } from './layouts/main-layout/main-layout-component';
import { AdminLayoutComponent } from './layouts/admin-layout/admin-layout-component';
import { AdminComponent } from './features/restauration/admin/admin-component';
import { FormFoodComponent } from './features/restauration/admin/form-food/form-food-component';
import { AddFoodToRestauComponent } from './features/restauration/admin/add-food-to-restau/add-food-to-restau-component';
import { ListPlatComponent } from './features/restauration/admin/list-plat/list-plat-component';
import { ListeRestaurant } from './features/restauration/admin/liste-restaurant/liste-restaurant';
import { FormRestaurant } from './features/restauration/admin/form-restaurant/form-restaurant';
import { AdminWalletManagementComponent } from './features/restauration/admin/admin-wallet-management/admin-wallet-management.component';
import { AdminInternManagementComponent } from './features/restauration/admin/admin-intern-management/admin-intern-management.component';
import { MyOrdersComponent } from './features/restauration/order/my-orders-component/my-orders-component';
import { RestaurationComponent } from './features/restauration/restauration.component';
import { adminOnlyGuard, adminRHGuard, managerGuard, superAdminGuard } from './features/auth/guards/role.guard';
import { authGuard } from './features/auth/guards/auth.guard';
import { SuperAdminLayoutComponent } from './layouts/super-admin-layout/super-admin-layout-component';
import { ArticleManagementComponent } from './features/super-admin/articles/article-management-component';
import { RhLayoutComponent } from './layouts/rh-layout/rh-layout-component';
import { EmployeeListComponent } from './features/rh/employee-list/employee-list-component';
import { LeaveComponent } from './features/rh/leave/leave-component';
import { EmployeeDetailComponent } from './features/rh/employee-detail/employee-detail-component';
import { EmployeeProfileLayoutComponent } from './features/rh/employee-profile-layout/employee-profile-layout-component';
import { EmployeePersonalInfoComponent } from './features/rh/employee-personal-info/employee-personal-info-component';
import { EmployeeDependentsComponent } from './features/rh/employee-dependents/employee-dependents-component';
import { EmployeeSkillsComponent } from './features/rh/employee-skills/employee-skills-component';
import { EmployeeDocumentsComponent } from './features/rh/employee-documents/employee-documents-component';
import { SkillsComponent } from './features/rh/skills/skills-component';
import { DepartmentsComponent } from './features/rh/departments/departments-component';
import { JobTitlesComponent } from './features/rh/job-titles/job-titles-component';
import { JobCategoriesComponent } from './features/rh/job-categories/job-categories-component';
import { ComingSoonComponent } from './layouts/coming-soon/coming-soon-component';
import { EmployeePortalLayoutComponent } from './layouts/employee-portal-layout/employee-portal-layout-component';
import { MyPersonalInfoComponent } from './features/rh/my-personal-info/my-personal-info-component';
import { MyDependentsComponent } from './features/rh/my-dependents/my-dependents-component';
import { MySkillsComponent } from './features/rh/my-skills/my-skills-component';
import { MyLeaveComponent } from './features/rh/my-leave/my-leave-component';
import { MyDocumentsComponent } from './features/rh/my-documents/my-documents-component';
import { LeaveBalanceAdminComponent } from './features/rh/leave-balance-admin/leave-balance-admin-component';
import { Dashboard } from './features/rh/dashboard/dashboard';
import { MyAbsenceComponent } from './features/rh/my-absence/my-absence-component';
import { AbsenceTypeAdminComponent } from './features/rh/absence-type-admin/absence-type-admin-component';
import { ApprovedLeavesComponent } from './features/rh/approved-leaves/approved-leaves-component';
import { ApprovedAbsencesComponent } from './features/rh/approved-absences/approved-absences-component';
import { RejectedLeavesComponent } from './features/rh/rejected-leaves/rejected-leaves-component';
import { RejectedAbsencesComponent } from './features/rh/rejected-absences/rejected-absences-component';
import { LeaveCalendarComponent } from './features/rh/leave-calendar/leave-calendar-component';
import { ArticleDetailComponent } from './features/home/article-detail/article-detail-component';



export const routes: Routes = [
  // ------------------------------------------------
  // 🌍 ROUTES UTILISATEURS (admin + normal)
  // ------------------------------------------------
  {
    path: '',
    component: MainLayoutComponent,
    title: 'Accel Connect',
    canActivate: [authGuard],
    children: [
      { path: '', component: HomeComponent },
      {
        path: 'restauration',
        title: 'Accel Connect | Restauration',
        children: [
          { path: '', component: RestaurationComponent },
          { path: 'cart', component: PanierComponent, title: 'Accel Connect | Panier' },
          { path: 'my-orders', component: MyOrdersComponent, title: 'Accel Connect | Mes commandes' },
        ],
      },
      { path: 'actualites/:id', component: ArticleDetailComponent, title: 'Accel Connect | Actualité' },
      { path: 'not-found', component: NotFoundComponent, title: '404' },

      {
        path: 'rh-admin',
        component: RhLayoutComponent,
        title: 'Accel Connect | Admin RH',
        canActivate: [adminRHGuard],
        children: [
          { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
          { path: 'dashboard', component: Dashboard, title: 'Accel Connect | Dashboard' },
          { path: 'calendar', component: LeaveCalendarComponent, title: 'Accel Connect | Calendrier' },
          {
            path: 'employees',
            component: EmployeeListComponent,
            title: 'Accel Connect | Employee List',
          },
          {
            path: 'employee-profile/:userId',
            component: EmployeeProfileLayoutComponent,
            title: 'Accel Connect | Profil employé',
            children: [
              { path: '', component: EmployeeDetailComponent, title: 'Accel Connect | Profil' },
              {
                path: 'personal-info',
                component: EmployeePersonalInfoComponent,
                title: 'Accel Connect | Informations personnelles',
              },
              {
                path: 'dependents',
                component: EmployeeDependentsComponent,
                title: 'Accel Connect | Personnes à charge',
              },
              {
                path: 'skills',
                component: EmployeeSkillsComponent,
                title: 'Accel Connect | Compétences',
              },
              {
                path: 'documents',
                component: EmployeeDocumentsComponent,
                title: 'Accel Connect | Documents',
              },
            ],
          },
          { path: 'leave', component: LeaveComponent, title: 'Accel Connect | Leave' },
          { path: 'leave-balances', component: LeaveBalanceAdminComponent, title: 'Accel Connect | Soldes de congés', canActivate: [adminRHGuard] },
          { path: 'qualifications', component: SkillsComponent, title: 'Accel Connect | Qualifications' },
          { path: 'departments', component: DepartmentsComponent, title: 'Accel Connect | Départements' },
          { path: 'job-titles', component: JobTitlesComponent, title: 'Accel Connect | Intitulés de poste' },
          { path: 'job-categories', component: JobCategoriesComponent, title: 'Accel Connect | Catégories de poste' },
          { path: 'absence-types', component: AbsenceTypeAdminComponent, title: "Accel Connect | Types d'absence" },
          { path: 'approved-leaves', component: ApprovedLeavesComponent, title: 'Accel Connect | Congés approuvés', canActivate: [adminRHGuard] },
          { path: 'rejected-leaves', component: RejectedLeavesComponent, title: 'Accel Connect | Congés rejetés', canActivate: [adminRHGuard] },
          { path: 'approved-absences', component: ApprovedAbsencesComponent, title: 'Accel Connect | Absences approuvées', canActivate: [adminRHGuard] },
          { path: 'rejected-absences', component: RejectedAbsencesComponent, title: 'Accel Connect | Absences rejetées', canActivate: [adminRHGuard] },
        ],
      },

      {
        path: 'super-admin',
        component: SuperAdminLayoutComponent,
        title: 'Accel Connect | Super Admin',
        canActivate: [superAdminGuard],
        children: [
          { path: '', redirectTo: 'articles', pathMatch: 'full' },
          { path: 'articles', component: ArticleManagementComponent, title: 'Accel Connect | Articles' },
        ],
      },

      { path: 'ressources', component: ComingSoonComponent, title: 'Accel Connect | Ressources' },
      {
        path: 'rh',
        component: EmployeePortalLayoutComponent,
        title: 'Accel Connect | Espace Employé',
        children: [
          { path: '', redirectTo: 'mon-profil', pathMatch: 'full' },
          { path: 'mon-profil', component: MyPersonalInfoComponent, title: 'Accel Connect | Mon Profil' },
          { path: 'mes-contacts', component: MyDependentsComponent, title: 'Accel Connect | Contacts d\'urgence' },
          { path: 'mes-competences', component: MySkillsComponent, title: 'Accel Connect | Mes Compétences' },
          { path: 'conges', component: MyLeaveComponent, title: 'Accel Connect | Mes Congés' },
          { path: 'mes-documents', component: MyDocumentsComponent, title: 'Accel Connect | Mes Documents' },
          { path: 'approbation-conges', loadComponent: () => import('./features/rh/leave-approval/leave-approval-component').then(m => m.LeaveApprovalComponent), title: 'Accel Connect | Approbation des congés', canActivate: [managerGuard] },
          { path: 'absences', component: MyAbsenceComponent, title: 'Accel Connect | Mes Absences' },
          { path: 'approbation-absences', loadComponent: () => import('./features/rh/absence-approval/absence-approval-component').then(m => m.AbsenceApprovalComponent), title: 'Accel Connect | Approbation des absences', canActivate: [managerGuard] },
        ],
      },
      { path: 'commercial', component: ComingSoonComponent, title: 'Accel Connect | Commercial' },
      { path: 'moyens', component: ComingSoonComponent, title: 'Accel Connect | Moyens Généraux' },
      { path: 'formation', component: ComingSoonComponent, title: 'Accel Connect | Formation' },

    ],
  },

  // ------------------------------------------------
  // 🔐 ROUTES ADMIN EXCLUSIVES
  // ------------------------------------------------
  {
    path: 'admin',
    component: AdminLayoutComponent,
    title: 'Accel Connect | Admin',
    canActivate: [authGuard, adminOnlyGuard],
    children: [
      { path: '', component: AdminComponent, data: { title: 'Dashboard' } },
      { path: 'dashboard', component: AdminComponent },
      { path: 'add-food', component: FormFoodComponent },
      { path: 'add-food-to-restau', component: AddFoodToRestauComponent },
      { path: 'liste-food', component: ListPlatComponent },
      { path: 'liste-restaurant', component: ListeRestaurant },
      { path: 'add-restaurant', component: FormRestaurant },
      { path: 'wallets', component: AdminWalletManagementComponent },
      { path: 'interns', component: AdminInternManagementComponent },


    ],
  },

  // ------------------------------------------------
  // ❌ ROUTES NON TROUVÉES
  // ------------------------------------------------
  { path: '**', redirectTo: '/not-found', pathMatch: 'full' },
];
