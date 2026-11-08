import { Component, OnInit, signal, inject } from '@angular/core';
import { RestaurantService } from '../../services/impl/restaurant.service';
import { Restaurant } from '../../model/restaurant.model';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NotificationService } from '../../../../core/services/impl/notification.service';
import { ConfirmationService } from '../../../../core/services/impl/ConfirmationDialog.service';

@Component({
  selector: 'app-liste-restaurant',
  imports: [RouterLink, CommonModule, FormsModule],
  templateUrl: './liste-restaurant.html',
  styleUrl: './liste-restaurant.css',
})
export class ListeRestaurant implements OnInit {
  private readonly restaurantService = inject(RestaurantService);
  private readonly notificationService = inject(NotificationService);
  private readonly confirmationService = inject(ConfirmationService);

  restaurants = signal<Restaurant[]>([]);
  totalRestaurants = signal<number>(0);
  isEditModalOpen = signal(false);
  isBlockModalOpen = signal(false);
  selectedRestaurant = signal<Restaurant | null>(null);
  isSubmitting = signal(false);
  blockMessage = '';

  ngOnInit(): void {
    this.loadRestaurants();
  }

  loadRestaurants(): void {
    this.restaurantService.getRestaurants().subscribe({
      next: (data) => {
        this.restaurants.set(data);
        this.totalRestaurants.set(data.length);
      },
      error: (error) => {
        console.error(error);
        this.notificationService.error(
          'Erreur de chargement',
          'Impossible de charger les restaurants'
        );
      },
    });
  }

  openEditModal(restaurant: Restaurant): void {
    this.selectedRestaurant.set({ ...restaurant });
    this.isEditModalOpen.set(true);
  }

  closeEditModal(): void {
    this.isEditModalOpen.set(false);
    this.selectedRestaurant.set(null);
    this.isSubmitting.set(false);
  }

  // 🔒 NOUVEAU : Modal de blocage
  openBlockModal(restaurant: Restaurant): void {
    this.selectedRestaurant.set({ ...restaurant });
    this.blockMessage = restaurant.blockMessage || '';
    this.isBlockModalOpen.set(true);
  }

  closeBlockModal(): void {
    this.isBlockModalOpen.set(false);
    this.selectedRestaurant.set(null);
    this.blockMessage = '';
    this.isSubmitting.set(false);
  }

  // 🔒 NOUVEAU : Toggle blocage
  toggleBlock(event: Event): void {
    event.preventDefault();
    const restaurant = this.selectedRestaurant();

    if (!restaurant || !restaurant.id) return;

    this.isSubmitting.set(true);

    this.restaurantService.toggleOrdersBlock(restaurant.id, this.blockMessage).subscribe({
      next: (updated) => {
        const currentRestaurants = this.restaurants();
        const index = currentRestaurants.findIndex((r) => r.id === updated.id);

        if (index !== -1) {
          currentRestaurants[index] = updated;
          this.restaurants.set([...currentRestaurants]);
        }

        this.notificationService.success(
          updated.ordersBlocked ? 'Commandes bloquées' : 'Commandes débloquées',
          `${updated.restaurantName} : ${
            updated.ordersBlocked
              ? 'Les utilisateurs ne peuvent plus commander'
              : 'Les commandes sont à nouveau autorisées'
          }`
        );
        this.closeBlockModal();
      },
      error: (error) => {
        console.error('Erreur toggle blocage:', error);
        this.isSubmitting.set(false);
        this.notificationService.error('Erreur', 'Impossible de modifier le statut des commandes');
      },
    });
  }

  updateRestaurant(event: Event): void {
    event.preventDefault();
    const restaurant = this.selectedRestaurant();

    if (!restaurant || !restaurant.id) return;

    // Validation
    if (!restaurant.restaurantName?.trim()) {
      this.notificationService.warning('Champ requis', 'Le nom du restaurant est obligatoire');
      return;
    }

    if (!restaurant.address?.trim()) {
      this.notificationService.warning('Champ requis', "L'adresse est obligatoire");
      return;
    }

    if (!restaurant.contact?.trim()) {
      this.notificationService.warning('Champ requis', 'Le contact est obligatoire');
      return;
    }

    if (restaurant.deliveryFee < 0) {
      this.notificationService.warning(
        'Prix invalide',
        'Les frais de livraison doivent être positifs ou nuls'
      );
      return;
    }

    this.isSubmitting.set(true);

    this.restaurantService.updateRestaurant(restaurant.id, restaurant).subscribe({
      next: (updated) => {
        const currentRestaurants = this.restaurants();
        const index = currentRestaurants.findIndex((r) => r.id === updated.id);

        if (index !== -1) {
          currentRestaurants[index] = updated;
          this.restaurants.set([...currentRestaurants]);
        }

        this.notificationService.success(
          'Restaurant modifié',
          `${updated.restaurantName} a été mis à jour avec succès`
        );
        this.closeEditModal();
      },
      error: (error) => {
        console.error('Erreur mise à jour:', error);
        this.isSubmitting.set(false);
        this.notificationService.error(
          'Erreur de modification',
          'Impossible de modifier le restaurant'
        );
      },
    });
  }

  deleteRestaurant(restaurant: Restaurant): void {
    this.confirmationService.confirmDelete(
      'Supprimer ce restaurant ?',
      `Êtes-vous sûr de vouloir supprimer "${restaurant.restaurantName}" ? Cette action est irréversible et supprimera également toutes les associations avec les plats.`,
      () => {
        if (!restaurant.id) return;

        this.restaurantService.deleteRestaurant(restaurant.id).subscribe({
          next: () => {
            const currentRestaurants = this.restaurants().filter((r) => r.id !== restaurant.id);
            this.restaurants.set(currentRestaurants);
            this.totalRestaurants.set(currentRestaurants.length);

            this.notificationService.success(
              'Restaurant supprimé',
              `${restaurant.restaurantName} a été supprimé avec succès`
            );
          },
          error: (error) => {
            console.error('Erreur suppression:', error);
            this.notificationService.error(
              'Erreur de suppression',
              'Impossible de supprimer le restaurant'
            );
          },
        });
      }
    );
  }

  updateField(field: keyof Restaurant, value: any): void {
    const restaurant = this.selectedRestaurant();
    if (restaurant) {
      this.selectedRestaurant.set({ ...restaurant, [field]: value });
    }
  }
}
