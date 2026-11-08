import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { RestaurantService } from './services/impl/restaurant.service';
import { MenuClientService } from './services/impl/menu.client.service';
import { CartService } from './services/impl/cart.service';
import { NotificationService } from '../../core/services/impl/notification.service';
import { RestaurantDisplay } from './model/display.model';
import { UserBalanceDTO } from './model/balance.model';
import { Restaurant } from './model/restaurant.model';
import { UserBalanceService } from './services/impl/userBalance.service';
import { AnimationOptions, LottieComponent } from 'ngx-lottie';
import { CartItem } from './model/panier.model';

@Component({
  selector: 'app-restauration',
  standalone: true,
  imports: [CommonModule, RouterModule, FontAwesomeModule, LottieComponent],
  templateUrl: './restauration.component.html',
  styleUrl: './restauration.component.css',
})
export class RestaurationComponent implements OnInit {
  private readonly restaurantService = inject(RestaurantService);
  private readonly menuClientService = inject(MenuClientService);
  protected readonly cartService = inject(CartService);
  private readonly userBalanceService = inject(UserBalanceService);
  private readonly notificationService = inject(NotificationService);

  // State
  restaurants = signal<RestaurantDisplay[]>([]);
  userBalance = signal<UserBalanceDTO | null>(null);
  selectedDate = signal<string>(this.getTodayDate());
  selectedDay = signal<string>(this.getCurrentDay());
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  expandedRestaurantId = signal<number | null>(null);

  // Animation state pour smooth transitions
  isAnimating = signal(false);

  // Nombre de plats à afficher par défaut
  readonly DEFAULT_ITEMS_DISPLAY = 3;

  // Jours de la semaine
  days = ['Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi'];

  // Dropdown ouvert (null = aucun)
  openDropdown = signal<string | null>(null);

  toggleDropdown(id: string): void {
    this.openDropdown.set(this.openDropdown() === id ? null : id);
  }

  isDropdownOpen(id: string): boolean {
    return this.openDropdown() === id;
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return d.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }

  // Computed: Nombre total d'items sélectionnés
  getTotalSelectedItems(): number {
    return this.restaurants().reduce((total, restaurant) => {
      return total + restaurant.items.reduce((sum, item) => sum + item.quantity, 0);
    }, 0);
  }

  // Computed: Nombre d'items sélectionnés par restaurant
  getRestaurantSelectedCount(restaurantId: number): number {
    const restaurant = this.restaurants().find((r) => r.id === restaurantId);
    if (!restaurant) return 0;
    return restaurant.items.reduce((sum, item) => sum + item.quantity, 0);
  }

  // Computed: Total prix par restaurant
  getRestaurantTotal(restaurantId: number): number {
    const restaurant = this.restaurants().find((r) => r.id === restaurantId);
    if (!restaurant) return 0;
    return restaurant.items.reduce((sum, item) => sum + item.unitPrice * item.quantity, 0);
  }

  ngOnInit(): void {
    this.lottieOptions = {
      path: 'assets/lottie/3DChefDancing.json',
    };

    this.loadUserBalance();
    this.loadRestaurantsWithMenus();
  }

  getCurrentDay(): string {
    const jours = ['Dimanche', 'Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi', 'Samedi'];
    return jours[new Date().getDay()];
  }

  /**
   * 💰 Charge le solde utilisateur
   */
  loadUserBalance(): void {
    this.userBalanceService.getMyBalance().subscribe({
      next: (balance) => {
        this.userBalance.set(balance);
      },
      error: (error) => {
        console.error('❌ Erreur lors du chargement du solde:', error);
      },
    });
  }

  /**
   * 🍽️ Charge les restaurants et leurs menus
   */
  loadRestaurantsWithMenus(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.restaurantService.getRestaurants().subscribe({
      next: (restaurants: Restaurant[]) => {
        this.menuClientService.getRestaurantsWithTodayMenus(restaurants).subscribe({
          next: (restaurantsWithMenus) => {
            const display = this.mapToDisplay(restaurantsWithMenus);
            this.restaurants.set(display);
            this.isLoading.set(false);
          },
          error: (error) => {
            console.error('❌ Erreur menus:', error);
            this.errorMessage.set('Impossible de charger les menus du jour');
            this.isLoading.set(false);
          },
        });
      },
      error: (error) => {
        console.error('❌ Erreur restaurants:', error);
        this.errorMessage.set('Impossible de charger les restaurants');
        this.isLoading.set(false);
      },
    });
  }

  /**
   * ➕ Augmente la quantité d'un plat avec animation
   */
  increaseQuantity(restaurantId: number, itemId: number, event?: Event): void {
    event?.stopPropagation();
    const updated = this.restaurants().map((restaurant) => {
      if (restaurant.id === restaurantId) {
        return {
          ...restaurant,
          items: restaurant.items.map((item) =>
            item.id === itemId ? { ...item, quantity: item.quantity + 1 } : item
          ),
        };
      }
      return restaurant;
    });
    this.restaurants.set(updated);
  }

  /**
   * ➖ Diminue la quantité d'un plat avec animation
   */
  decreaseQuantity(restaurantId: number, itemId: number, event?: Event): void {
    event?.stopPropagation();
    const updated = this.restaurants().map((restaurant) => {
      if (restaurant.id === restaurantId) {
        return {
          ...restaurant,
          items: restaurant.items.map((item) =>
            item.id === itemId && item.quantity > 0
              ? { ...item, quantity: item.quantity - 1 }
              : item
          ),
        };
      }
      return restaurant;
    });
    this.restaurants.set(updated);
  }

  /**
   * 🛒 Ajoute tous les plats sélectionnés au panier
   */
  addAllToCart(): void {
    const allItems: CartItem[] = [];

    // Collecter tous les items avec quantity > 0
    this.restaurants().forEach((restaurant) => {
      restaurant.items.forEach((item) => {
        if (item.quantity > 0) {
          allItems.push({
            mealId: item.id!,
            mealName: item.mealName,
            restaurantId: restaurant.id,
            restaurantName: restaurant.name,
            unitPrice: item.unitPrice,
            quantity: item.quantity,
            imageUrl: item.imageUrl,
          });
        }
      });
    });

    if (allItems.length === 0) {
      this.notificationService.warning('Sélection vide', 'Veuillez sélectionner au moins un plat');
      return;
    }

    // Ajouter au panier avec les quantités correctes
    allItems.forEach((item) => {
      this.cartService.addItem(item);
    });

    // Réinitialiser les quantités
    this.resetAllQuantities();

    this.notificationService.success(
      'Ajouté au panier',
      `${allItems.length} article(s) ajouté(s) avec succès`
    );
  }

  /**
   * 🔄 Réinitialise toutes les quantités
   */
  resetAllQuantities(): void {
    const updated = this.restaurants().map((restaurant) => ({
      ...restaurant,
      items: restaurant.items.map((item) => ({ ...item, quantity: 0 })),
    }));
    this.restaurants.set(updated);
  }

  /**
   * 📂 Toggle l'expansion d'un restaurant avec animation fluide
   */
  toggleRestaurant(restaurantId: number, event?: Event): void {
    event?.stopPropagation();

    if (this.isAnimating()) return;

    this.isAnimating.set(true);

    if (this.expandedRestaurantId() === restaurantId) {
      this.expandedRestaurantId.set(null);
    } else {
      this.expandedRestaurantId.set(restaurantId);
    }

    // Reset animation state après la durée de l'animation
    setTimeout(() => {
      this.isAnimating.set(false);
    }, 300);
  }

  /**
   * 🔄 Ouvrir tous les restaurants
   */
  expandAll(): void {
    // Ouvrir le premier restaurant s'il existe
    const firstRestaurant = this.restaurants()[0];
    if (firstRestaurant) {
      this.expandedRestaurantId.set(firstRestaurant.id);
    }
  }

  /**
   * 🔄 Fermer tous les restaurants
   */
  collapseAll(): void {
    this.expandedRestaurantId.set(null);
  }

  /**
   * 👁️ Vérifie si un restaurant est ouvert
   */
  isRestaurantExpanded(restaurantId: number): boolean {
    return this.expandedRestaurantId() === restaurantId;
  }

  /**
   * 🍽️ Récupère les plats à afficher pour un restaurant
   */
  getVisibleItems(restaurant: RestaurantDisplay): any[] {
    if (this.isRestaurantExpanded(restaurant.id)) {
      return restaurant.items;
    }
    return restaurant.items.slice(0, this.DEFAULT_ITEMS_DISPLAY);
  }

  /**
   * 📊 Vérifie si un restaurant a plus de plats que la limite
   */
  hasMoreItems(restaurant: RestaurantDisplay): boolean {
    return restaurant.items.length > this.DEFAULT_ITEMS_DISPLAY;
  }

  /**
   * 🔢 Calcule le nombre de plats cachés
   */
  getHiddenItemsCount(restaurant: RestaurantDisplay): number {
    return Math.max(0, restaurant.items.length - this.DEFAULT_ITEMS_DISPLAY);
  }

  /**
   * 📊 Formate un montant
   */
  formatAmount(amount: number): string {
    return new Intl.NumberFormat('fr-FR', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(amount);
  }

  /**
   * 🏷️ Vérifie si c'est le weekend
   */
  isWeekend(dateStr: string): boolean {
    const date = new Date(dateStr);
    const day = date.getDay();
    return day === 0 || day === 6; // 0 = Dimanche, 6 = Samedi
  }

  // Utilitaires
  private getTodayDate(): string {
    return new Date().toISOString().split('T')[0];
  }

  // À ajouter/remplacer dans RestaurationComponent

  private mapToDisplay(data: any[]): RestaurantDisplay[] {
    const gradients = [
      { from: '#EF4444', to: '#DC2626', border: 'red-500' },
      { from: '#F59E0B', to: '#D97706', border: 'orange-500' },
      { from: '#10B981', to: '#059669', border: 'green-500' },
      { from: '#3B82F6', to: '#2563EB', border: 'blue-500' },
      { from: '#8B5CF6', to: '#7C3AED', border: 'purple-500' },
    ];

    return data.map((rwm, idx) => {
      const gradient = gradients[idx % gradients.length];

      return {
        id: rwm.restaurant.id,
        name: rwm.restaurant.restaurantName,
        subtitle: `Livraison: ${rwm.restaurant.deliveryFee} FCFA`,
        icon: 'utensils',
        gradientFrom: gradient.from,
        gradientTo: gradient.to,
        hasMenu: rwm.hasMenu,
        ordersBlocked: rwm.restaurant.ordersBlocked || false, // 🔒 NOUVEAU
        blockMessage: rwm.restaurant.blockMessage, // 🔒 NOUVEAU
        items: (rwm.menu?.meals || []).map((meal: any, mealIdx: number) => ({
          id: meal.id,
          mealName: meal.mealName,
          unitPrice: meal.unitPrice,
          imageUrl: meal.imageUrl,
          quantity: 0,
          gradientFrom: gradient.from,
          gradientTo: gradient.to,
          hoverBorder: gradient.border,
        })),
      };
    });
  }

  // Animation Lottie
  lottieOptions: AnimationOptions = {
    path: '',
  };

  onAnimationCreated(anim: any): void {
    // console.log('Animation créée:', anim);
  }
  // Ajouter ces méthodes dans RestaurationComponent

  /**
   * 📏 Détermine si un nom de plat est considéré comme long
   */
  isMealNameLong(mealName: string): boolean {
    return mealName.length > 15;
  }

  /**
   * 📏 Détermine si un nom de plat est très long
   */
  isMealNameVeryLong(mealName: string): boolean {
    return mealName.length > 40;
  }

  /**
   * ✂️ Tronque un nom de plat avec ellipsis (optionnel, pour usage alternatif)
   */
  truncateMealName(mealName: string, maxLength: number = 30): string {
    if (mealName.length <= maxLength) return mealName;
    return mealName.substring(0, maxLength - 3) + '...';
  }

  /**
   * 🎨 Retourne les classes CSS appropriées selon la longueur du nom
   */
  getMealNameClasses(mealName: string): string {
    if (mealName.length > 40) {
      return 'text-[9px] sm:text-[10px] md:text-xs cursor-help line-clamp-2';
    } else if (mealName.length > 15) {
      return 'text-[10px] sm:text-xs md:text-sm cursor-help truncate';
    }
    return 'text-[10px] sm:text-xs md:text-sm truncate';
  }

  /**
   * 📱 Détecte si on est sur mobile (optionnel, pour comportements spécifiques)
   */
  isMobile(): boolean {
    return window.innerWidth < 640;
  }

  /**
   * 💡 Formate le nom pour l'affichage du tooltip (peut ajouter des infos supplémentaires)
   */
  formatTooltipTitle(item: any): string {
    let title = item.mealName;
    return title;
  }
}
