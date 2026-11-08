import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { PlatService } from '../../services/impl/plat.service';
import { RestaurantService } from '../../services/impl/restaurant.service';
import { MenuService } from '../../services/impl/menu.service';
import { Meal } from '../../model/plat.model';
import { Restaurant } from '../../model/restaurant.model';

@Component({
  selector: 'app-add-food-to-restau-component',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  templateUrl: './add-food-to-restau-component.html',
  styleUrl: './add-food-to-restau-component.css',
})
export class AddFoodToRestauComponent implements OnInit {
  private readonly platService = inject(PlatService);
  private readonly restaurantService = inject(RestaurantService);
  private readonly menuService = inject(MenuService);
  private readonly router = inject(Router);

  // Signaux pour la réactivité
  restaurants = signal<Restaurant[]>([]);
  meals = signal<Meal[]>([]);
  filteredMeals = signal<Meal[]>([]);
  selectedRestaurant = signal<Restaurant | null>(null);
  selectedMealIds = signal<Set<number>>(new Set());
  selectedDate = signal<string>(this.getTodayDate());
  useTodayDate = signal<boolean>(true);

  searchQuery = signal<string>('');
  isLoading = signal<boolean>(false);
  isSaving = signal<boolean>(false);
  errorMessage = signal<string>('');
  successMessage = signal<string>('');

  ngOnInit(): void {
    this.loadRestaurants();
    this.loadMeals();
    this.checkIfTodayIsWeekend();
  }

  getTodayDate(): string {
    const today = new Date();
    return today.toISOString().split('T')[0];
  }

  checkIfTodayIsWeekend(): void {
    if (this.isWeekend(this.getTodayDate())) {
      this.errorMessage.set(
        "Aujourd'hui est un week-end. Veuillez sélectionner une date en semaine."
      );
      this.useTodayDate.set(false);
    }
  }

  toggleDateMode(): void {
    this.useTodayDate.set(!this.useTodayDate());
    if (this.useTodayDate()) {
      this.selectedDate.set(this.getTodayDate());
      this.checkIfTodayIsWeekend();
    }
    this.errorMessage.set('');
    if (this.selectedRestaurant()) {
      this.loadExistingMenu();
    }
  }

  isWeekend(dateString: string): boolean {
    const date = new Date(dateString);
    const day = date.getDay();
    return day === 0 || day === 6; // 0 = Dimanche, 6 = Samedi
  }

  onDateChange(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.selectedDate.set(value);

    if (this.isWeekend(value)) {
      this.errorMessage.set('Les menus ne peuvent pas être créés pour le week-end');
    } else {
      this.errorMessage.set('');
      // Charger le menu existant pour cette date si disponible
      if (this.selectedRestaurant()) {
        this.loadExistingMenu();
      }
    }
  }

  getEffectiveDate(): string {
    return this.useTodayDate() ? this.getTodayDate() : this.selectedDate();
  }

  loadExistingMenu(): void {
    if (!this.selectedRestaurant()) return;

    const effectiveDate = this.getEffectiveDate();

    this.menuService.getMenuByDate(this.selectedRestaurant()!.id, effectiveDate).subscribe({
      next: (menu) => {
        // Pré-sélectionner les plats du menu existant
        const mealIds = new Set(menu.mealIds);
        this.selectedMealIds.set(mealIds);
      },
      error: (error) => {
        // Aucun menu existant, c'est normal
        this.selectedMealIds.set(new Set());
      },
    });
  }

  loadRestaurants(): void {
    this.isLoading.set(true);
    this.restaurantService.getRestaurants().subscribe({
      next: (data) => {
        this.restaurants.set(data);
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Erreur lors du chargement des restaurants:', error);
        this.errorMessage.set('Impossible de charger les restaurants');
        this.isLoading.set(false);
      },
    });
  }

  loadMeals(): void {
    this.platService.getPlats().subscribe({
      next: (data) => {
        this.meals.set(data);
        this.filteredMeals.set(data);
      },
      error: (error) => {
        this.errorMessage.set('Impossible de charger les plats');
      },
    });
  }

  selectRestaurant(restaurant: Restaurant): void {
    this.selectedRestaurant.set(restaurant);
    this.errorMessage.set('');

    this.selectedMealIds.set(new Set());

    this.loadExistingMenu();
  }

  toggleMealSelection(mealId: number): void {
    const currentSelected = new Set(this.selectedMealIds());
    if (currentSelected.has(mealId)) {
      currentSelected.delete(mealId);
    } else {
      currentSelected.add(mealId);
    }
    this.selectedMealIds.set(currentSelected);
  }

  isMealSelected(mealId: number): boolean {
    return this.selectedMealIds().has(mealId);
  }

  selectAllMeals(): void {
    const allMealIds = new Set(this.filteredMeals().map((m) => m.id!));
    this.selectedMealIds.set(allMealIds);
  }

  deselectAllMeals(): void {
    this.selectedMealIds.set(new Set());
  }

  filterMeals(): void {
    const query = this.searchQuery().toLowerCase();
    let filtered = this.meals();

    if (query) {
      filtered = filtered.filter(
        (meal) =>
          meal.mealName.toLowerCase().includes(query)
      );
    }
    this.filteredMeals.set(filtered);
  }

  onSearchChange(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchQuery.set(value);
    this.filterMeals();
  }

  async createMenu(): Promise<void> {
    if (!this.selectedRestaurant()) {
      this.errorMessage.set('Veuillez sélectionner un restaurant');
      return;
    }

    if (this.selectedMealIds().size === 0) {
      this.errorMessage.set('Veuillez sélectionner au moins un plat');
      return;
    }

    const effectiveDate = this.getEffectiveDate();

    if (this.isWeekend(effectiveDate)) {
      this.errorMessage.set('Les menus ne peuvent pas être créés pour le week-end');
      return;
    }

    this.isSaving.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    const request = {
      restaurantId: this.selectedRestaurant()!.id,
      menuDate: effectiveDate,
      mealIds: Array.from(this.selectedMealIds()),
    };

    this.menuService.createOrUpdateMenu(request).subscribe({
      next: (menu) => {
        const dateObj = new Date(effectiveDate);
        const isToday = effectiveDate === this.getTodayDate();
        const dateText = isToday
          ? "aujourd'hui"
          : dateObj.toLocaleDateString('fr-FR', {
              weekday: 'long',
              year: 'numeric',
              month: 'long',
              day: 'numeric',
            });

        this.successMessage.set(
          `Menu créé avec succès pour ${this.selectedRestaurant()!.restaurantName} - ${dateText}`
        );

        // Réinitialiser après 3 secondes
        setTimeout(() => {
          this.selectedMealIds.set(new Set());
          this.successMessage.set('');
        }, 3000);

        this.isSaving.set(false);
      },
      error: (error) => {
        console.error('Erreur lors de la création du menu:', error);
        const errorMsg = error.error?.message || 'Erreur lors de la création du menu';
        this.errorMessage.set(errorMsg);
        this.isSaving.set(false);
      },
    });
  }

  cancel(): void {
    this.router.navigate(['/admin/add-food-to-restau']);
    this.selectedMealIds.set(new Set());
    this.selectedRestaurant.set(null);
  }

  getRestaurantColor(index: number): string {
    const colors = ['orange', 'green', 'purple', 'blue', 'red', 'yellow'];
    return colors[index % colors.length];
  }

  getDayName(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('fr-FR', { weekday: 'long' });
  }
}
