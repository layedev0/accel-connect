import { Component, inject, computed, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { CartService } from '../services/impl/cart.service';
import { OrderService } from '../services/impl/order.service';
import { NotificationService } from '../../../core/services/impl/notification.service';
import { ConfirmationService } from '../../../core/services/impl/ConfirmationDialog.service';
import { AnimationOptions, LottieComponent } from 'ngx-lottie';
import { AnimationItem } from 'lottie-web';
import { OrderDTO, OrderStatus } from '../model/order.model';

@Component({
  selector: 'app-panier',
  standalone: true,
  imports: [CommonModule, FontAwesomeModule, LottieComponent, RouterModule],
  templateUrl: './panier-component.html',
  styleUrl: './panier-component.css',
})
export class PanierComponent implements OnInit {
  private readonly cartService = inject(CartService);
  private readonly orderService = inject(OrderService);
  private readonly router = inject(Router);
  private readonly notificationService = inject(NotificationService);
  private readonly confirmationService = inject(ConfirmationService);

  cartSummary = computed(() => this.cartService.cartSummary());
  isOrdering = signal(false);
  isLoading = signal(false);

  /** 📦 Commande confirmée du jour, si elle existe (les articles s'y ajouteront) */
  todayOrder = signal<OrderDTO | null>(null);

  emptyCartOptions: AnimationOptions = {
    path: '/assets/lottie/emptycart.json',
    loop: true,
    autoplay: true,
  };

  ngOnInit(): void {
    this.validateCart();
    this.loadTodayOrder();
  }

  /**
   * 📦 Récupère la commande confirmée du jour pour prévenir l'utilisateur
   * que son panier viendra s'y ajouter
   */
  private loadTodayOrder(): void {
    this.orderService.getMyOrders().subscribe({
      next: (orders) => {
        const today = new Date().toISOString().split('T')[0];
        const order = orders.find(
          (o) => o.orderDate === today && o.status === OrderStatus.CONFIRMED
        );
        this.todayOrder.set(order ?? null);
      },
      error: (error) => {
        // Échec silencieux : ne doit jamais bloquer la commande
        console.error('❌ Erreur chargement commande du jour:', error);
        this.todayOrder.set(null);
      },
    });
  }

  /** ➕ Le panier va s'ajouter à une commande existante */
  get isAddingToExistingOrder(): boolean {
    return this.cartService.getOrderMode() === 'ADD' && this.todayOrder() !== null;
  }

  /**
   * ✅ Valide que les items du panier sont toujours disponibles
   */
  validateCart(): void {
    this.isLoading.set(true);
    this.cartService.validateCartItems().subscribe({
      next: ({ validItems, removedItems }) => {
        this.isLoading.set(false);

        if (removedItems.length > 0) {
          const itemNames = removedItems.map((item) => item.mealName).join(', ');
          this.notificationService.warning(
            'Articles retirés',
            `${removedItems.length} article(s) retiré(s) du panier car non disponible(s) aujourd'hui : ${itemNames}`
          );
        }
      },
      error: (error) => {
        console.error('❌ Erreur validation panier:', error);
        this.isLoading.set(false);
      },
    });
  }

  updateQuantity(mealId: number, restaurantId: number, delta: number): void {
    const summary = this.cartSummary();
    const item = summary.items.find((i) => i.mealId === mealId && i.restaurantId === restaurantId);

    if (!item) return;

    const newQuantity = item.quantity + delta;

    if (newQuantity <= 0) {
      this.removeItem(mealId, restaurantId);
    } else {
      this.cartService.updateQuantity(mealId, restaurantId, newQuantity);
    }
  }

  removeItem(mealId: number, restaurantId: number): void {
    this.confirmationService.confirmDelete(
      'Supprimer cet article ?',
      'Voulez-vous vraiment retirer cet article de votre panier ?',
      () => {
        this.cartService.removeItem(mealId, restaurantId);
        this.notificationService.success('Article supprimé', "L'article a été retiré");
      }
    );
  }

  clearCart(): void {
    this.confirmationService.confirmDelete(
      'Vider le panier ?',
      'Tous les articles seront retirés.',
      () => {
        this.cartService.clear();
        this.notificationService.success('Panier vidé', 'Tous les articles ont été retirés');
      }
    );
  }

  placeOrder(): void {
    const summary = this.cartSummary();

    if (summary.totalItems === 0) {
      this.notificationService.warning('Panier vide', 'Votre panier est vide');
      return;
    }

    // Valider une dernière fois avant de commander
    this.isLoading.set(true);
    this.cartService.validateCartItems().subscribe({
      next: ({ validItems, removedItems }) => {
        this.isLoading.set(false);

        if (removedItems.length > 0) {
          const itemNames = removedItems.map((item) => item.mealName).join(', ');
          this.notificationService.error(
            'Articles indisponibles',
            `Certains articles ne sont plus disponibles : ${itemNames}`
          );
          return;
        }

        if (validItems.length === 0) {
          this.notificationService.warning('Panier vide', 'Votre panier est vide');
          return;
        }

        // Continuer avec la confirmation
        this.confirmationService.confirm({
          title: this.isAddingToExistingOrder ? 'Ajouter à votre commande' : 'Confirmer la commande',
          message: this.buildConfirmationMessage(),
          type: 'info',
          confirmText: this.isAddingToExistingOrder ? 'Ajouter' : 'Commander',
          cancelText: 'Annuler',
          onConfirm: () => {
            this.processOrder();
          },
        });
      },
      error: (error) => {
        console.error('❌ Erreur validation:', error);
        this.isLoading.set(false);
        this.notificationService.error(
          'Erreur de validation',
          'Impossible de valider votre panier'
        );
      },
    });
  }

  /**
   * 💬 Message de confirmation adapté au mode d'envoi du panier
   */
  private buildConfirmationMessage(): string {
    if (this.cartService.getOrderMode() === 'REPLACE') {
      return 'Les articles de votre commande du jour seront remplacés par ce panier.';
    }

    const existing = this.todayOrder();
    if (!existing) {
      return 'Êtes-vous sûr de vouloir passer cette commande ?';
    }

    const newSubtotal = existing.subtotal + this.cartSummary().subtotal;
    const remaining = Math.max(0, newSubtotal - existing.subsidyAmount);

    return (
      `Vous avez déjà une commande aujourd'hui : ces articles y seront ajoutés. ` +
      `Nouveau sous-total : ${this.formatAmount(newSubtotal)} FCFA — ` +
      `reste à payer : ${this.formatAmount(remaining)} FCFA (hors frais de livraison).`
    );
  }

  private processOrder(): void {
    this.isOrdering.set(true);
    const isAdding = this.isAddingToExistingOrder;
    const payload = this.cartService.prepareOrderData();

    this.orderService.createOrder(payload).subscribe({
      next: (order) => {
        this.isOrdering.set(false);
        this.cartService.clear();

        this.notificationService.success(
          isAdding ? 'Articles ajoutés' : 'Commande confirmée',
          isAdding
            ? 'Vos articles ont été ajoutés à votre commande du jour !'
            : `Votre commande a été enregistrée avec succès !`
        );

        setTimeout(() => {
          this.router.navigate(['restauration/my-orders']);
        }, 300);
      },
      error: (error) => {
        console.error('❌ Erreur commande:', error);
        this.isOrdering.set(false);

        // Utiliser la méthode extractErrorMessage du service
        const errorMessage = this.orderService.extractErrorMessage(error);

        // Déterminer le titre en fonction du message
        let title = 'Erreur de commande';
        if (errorMessage.includes('trop tard')) {
          title = '⏰ Délai dépassé';
        } else if (errorMessage.includes('annulé')) {
          title = '🚫 Commande annulée';
        } else if (errorMessage.includes('vide')) {
          title = '🛒 Panier vide';
        }

        this.notificationService.error(title, errorMessage);
      },
    });
  }

  onAnimationCreated(animationItem: AnimationItem): void {
    console.log('Empty cart animation loaded!', animationItem);
  }

  formatAmount(amount: number): string {
    return new Intl.NumberFormat('fr-FR', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(amount);
  }

  get isEmpty(): boolean {
    return this.cartSummary().totalItems === 0;
  }
}
