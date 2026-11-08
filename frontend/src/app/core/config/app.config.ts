export class AppConfig {
  static readonly logoUrl = 'assets/logo/accel_logo.png';
  static readonly defaultAvatar = 'assets/img/nasr_new.jpg';

  // FontAwesome icon names (rendered via <fa-icon [icon]="['fas', name]">)
  static readonly icons = [
    'bars',
    'folder-open',
    'users',
    'chart-bar',
    'truck',
    'graduation-cap',
  ];

  static readonly iconTitles = [
    'Restaurant',
    'Ressources',
    'R.H.',
    'Commercial',
    'Logistic',
    'Formation',
  ];

  static readonly iconRoutes = [
    '/restauration',
    '/ressources',
    '/rh',
    '/commercial',
    '/moyens',
    '/formation',
  ];

  // 🆕 Chemins des animations Lottie pour chaque menu
  static readonly lottieAnimations = [
    '/assets/lottie/Onlinefoodorder.json', // Menu
    '/assets/lottie/relax.json', // Ressources
    '/assets/lottie/Profile.json', // R.H.
    '/assets/lottie/commercial.json', // Commercial
    '/assets/lottie/LoadingWebKomship.json', // Moyens Généraux
    '/assets/lottie/relax.json', // Formation
  ];

  // 🆕 Titres personnalisés pour chaque animation
  static readonly animationTitles = [
    'Chargement du Menu',
    'Chargement des Ressources',
    'Chargement RH',
    'Chargement Commercial',
    'Chargement Moyens Généraux',
    'Chargement Formation',
  ];

  // 🆕 Couleurs de thème pour chaque animation
  static readonly animationThemes = [
    { primary: '#25509D', secondary: '#99CFBD' }, // Menu
    { primary: '#4ade80', secondary: '#10b981' }, // Ressources (vert)
    { primary: '#f59e0b', secondary: '#fbbf24' }, // R.H. (orange)
    { primary: '#ef4444', secondary: '#f87171' }, // Commercial (rouge)
    { primary: '#8b5cf6', secondary: '#a78bfa' }, // Moyens (violet)
    { primary: '#06b6d4', secondary: '#22d3ee' }, // Formation (cyan)
  ];

  static readonly colors = {
    primary: '#25509D',
    secondary: '#99CFBD',
    accent: '#E84141',
    dark: '#20264E',
    darkAccent: '#6B140F',
    gray: '#303131',
    lightGray: '#E1E1E1',
    white: '#FFFFFF',
  };
}
