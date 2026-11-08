import { Component, OnInit, OnDestroy, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { Subject, forkJoin, takeUntil } from 'rxjs';

// Services
import { AdminDashboardService } from '../services/impl/admin.dashboard.service';
import { AdminOrderService, AdminCreateOrderRequest, AdminOrderItemData } from '../services/impl/admin-order.service';
import { PaiementService } from '../services/impl/paiement.service';
import { NotificationService } from '../../../core/services/impl/notification.service';
import { ExcelExportService } from '../services/impl/excel-export.service';
import { AdminNotificationService } from '../services/impl/admin-notification.service';
import { RestaurantService } from '../services/impl/restaurant.service';
import { MenuClientService } from '../services/impl/menu.client.service';

// Models
import { DashboardFilters, DashboardView, RestaurantStats } from '../model/adminOrder.model';
import { PaymentSummary } from '../model/paiement.model';
import { WeeklyExportModal } from "./weekly-export-modal/weekly-export-modal";
import { ModalService } from '../services/impl/modal.service';
import { UserBalanceStatsDTO } from '../model/balance.model';
import { OrderDTO } from '../model/order.model';
import { Restaurant } from '../model/restaurant.model';

interface MealStat {
  mealName: string;
  quantity: number;
  totalAmount: number;
}

interface PaymentValidationState {
  canValidate: boolean;
  isFriday: boolean;
  currentDay: string;
  weekStartDate: string;
  weekEndDate: string;
  message: string;
  nextFriday?: string;
}

interface RestaurantStatsModal {
  restaurant: RestaurantStats;
  meals: MealStat[];
}

interface ModalMealItem {
  mealId: number;
  mealName: string;
  restaurantId: number;
  restaurantName: string;
  unitPrice: number;
  quantity: number;
}

interface ModalRestaurant {
  restaurantId: number;
  restaurantName: string;
  meals: ModalMealItem[];
  expanded: boolean;
}

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, FontAwesomeModule, WeeklyExportModal],
  templateUrl: './admin-component.html',
  styleUrls: ['./admin-component.css'],
})
export class AdminComponent implements OnInit, OnDestroy {
  // ============================================================================
  // INJECTION DE DÉPENDANCES
  // ============================================================================
  private readonly dashboardService = inject(AdminDashboardService);
  private readonly adminOrderService = inject(AdminOrderService);
  private readonly paymentService = inject(PaiementService);
  private readonly notificationService = inject(NotificationService);
  private readonly excelExportService = inject(ExcelExportService);
  private readonly notificationService2 = inject(AdminNotificationService);
  private readonly restaurantService = inject(RestaurantService);
  private readonly menuClientService = inject(MenuClientService);
  private readonly destroy$ = new Subject<void>();
  protected readonly modalService = inject(ModalService);

  // ============================================================================
  // ÉTATS PRINCIPAUX
  // ============================================================================
  isLoading = signal(true);
  dashboardData = signal<DashboardView | null>(null);
  // ============================================================================
  // FILTRES
  // ============================================================================
  filters = signal<DashboardFilters>({
    selectedDate: new Date().toISOString().split('T')[0],
    selectedRestaurantId: null,
    searchTerm: '',
    showCancelled: false,
  });

  // ============================================================================
  // MODAL CRÉER / MODIFIER COMMANDE ADMIN
  // ============================================================================
  showOrderModal = signal(false);
  orderModalTitle = signal('Ajouter une commande');
  orderModalEditOrderId = signal<number | null>(null);
  allUsers = signal<UserBalanceStatsDTO[]>([]);
  orderModalUserId = signal<number | null>(null);
  orderModalDate = signal<string>(new Date().toISOString().split('T')[0]);
  orderModalRestaurants = signal<ModalRestaurant[]>([]);
  orderModalLoading = signal(false);
  orderModalSubmitting = signal(false);

  orderModalTotalItems = computed(() =>
    this.orderModalRestaurants().reduce((sum, r) => sum + r.meals.reduce((s, m) => s + m.quantity, 0), 0)
  );

  orderModalTotal = computed(() =>
    this.orderModalRestaurants().reduce((sum, r) => sum + r.meals.reduce((s, m) => s + m.unitPrice * m.quantity, 0), 0)
  );

  isSendingMenuNotif = signal(false);
  notifyingRestaurantId = signal<number | null>(null);

  // ============================================================================
  // MODAL STATISTIQUES RESTAURANT
  // ============================================================================
  showStatsModal = signal(false);
  selectedRestaurantStats = signal<RestaurantStatsModal | null>(null);

  // ============================================================================
  // MODAL PAIEMENT
  // ============================================================================
  showPaymentModal = signal(false);
  paymentSummary = signal<PaymentSummary | null>(null);
  isLoadingPayment = signal(false);
  lastPaymentDate = signal<string | null>(null);
  paymentValidationState = signal<PaymentValidationState | null>(null);

  // ============================================================================
  // COMPUTED PROPERTIES
  // ============================================================================
  canValidatePayments = computed(() => this.paymentValidationState()?.canValidate ?? false);

  filteredRestaurants = computed(() => {
    const data = this.dashboardData();
    return data ? data.restaurantStats : [];
  });

  filteredUsers = computed(() => {
    const data = this.dashboardData();
    const f = this.filters();
    if (!data) return [];

    let users = data.userOrders;

    // Filtre par restaurant
    if (f.selectedRestaurantId) {
      users = users.filter((u) =>
        u.meals.some((m) => {
          const restaurant = data.restaurantStats.find((r) => r.restaurantName === m.restaurant);
          return restaurant?.restaurantId === f.selectedRestaurantId;
        })
      );
    }

    // Filtre par recherche
    if (f.searchTerm) {
      const term = f.searchTerm.toLowerCase();
      users = users.filter(
        (u) =>
          u.username.toLowerCase().includes(term) ||
          u.meals.some((m) => m.name.toLowerCase().includes(term)) ||
          u.restaurants.some((r) => r.toLowerCase().includes(term))
      );
    }

    // Filtre commandes annulées
    if (!f.showCancelled) {
      users = users.filter((u) => u.orders.some((o) => o.status === 'CONFIRMED'));
    }

    return users;
  });

  // ============================================================================
  // LIFECYCLE HOOKS
  // ============================================================================
  ngOnInit(): void {
    this.initializeComponent();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ============================================================================
  // INITIALISATION
  // ============================================================================
  private initializeComponent(): void {
    this.loadData();
    this.loadLastPaymentDate();
    this.checkPaymentValidationState();
    this.checkFridayReminder();
  }

  openExportModal(): void {
    this.modalService.openWeeklyExportModal();
  }

  closeExportModal(): void {
    this.modalService.closeWeeklyExportModal();
  }

  notifyMenuAvailable(): void {
    if (this.isSendingMenuNotif()) return;
    this.isSendingMenuNotif.set(true);
    this.notificationService2.notifyMenuAvailable().subscribe({
      next: (res) => {
        this.notificationService.success(
          '✅ Notifications envoyées',
          `${res.sent} utilisateur(s) notifié(s)${res.failed > 0 ? ` (${res.failed} échec(s))` : ''}`
        );
        this.isSendingMenuNotif.set(false);
      },
      error: () => {
        this.notificationService.error('❌ Erreur', 'Impossible d\'envoyer les notifications');
        this.isSendingMenuNotif.set(false);
      },
    });
  }

  notifyRestaurantDelivered(restaurantId: number, event: Event): void {
    event.stopPropagation();
    if (this.notifyingRestaurantId() === restaurantId) return;
    this.notifyingRestaurantId.set(restaurantId);
    const date = this.filters().selectedDate;
    this.notificationService2.notifyRestaurantDelivered(restaurantId, date).subscribe({
      next: (res) => {
        this.notificationService.success(
          '✅ Notifications envoyées',
          `${res.sent} commande(s) notifiée(s) pour ${res.restaurantName}${res.failed > 0 ? ` (${res.failed} échec(s))` : ''}`
        );
        this.notifyingRestaurantId.set(null);
      },
      error: () => {
        this.notificationService.error('❌ Erreur', 'Impossible d\'envoyer les notifications');
        this.notifyingRestaurantId.set(null);
      },
    });
  }

  // ============================================================================
  // CHARGEMENT DES DONNÉES
  // ============================================================================
  loadData(): void {
    this.isLoading.set(true);
    const date = this.filters().selectedDate;

    this.dashboardService
      .loadDashboardData(date)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.dashboardData.set(data);
          this.isLoading.set(false);
        },
        error: (error) => {
          this.isLoading.set(false);
          // this.notificationService.error(
          //   'Erreur de chargement',
          //   'Impossible de charger les données du dashboard: ' +
          //   (error.error?.message || error.message)
          // );
          console.error('Erreur chargement dashboard', error);
        },
      });
  }

  private loadLastPaymentDate(): void {
    this.paymentService
      .getLastPaymentDate()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.lastPaymentDate.set(response.lastPaymentDate);
        },
        error: (error) => {
          console.error('Erreur chargement dernière date paiement', error);
        },
      });
  }

  private checkPaymentValidationState(): void {
    this.paymentService
      .checkCanValidatePayments()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (state) => {
          this.paymentValidationState.set(state);
        },
        error: (error) => {
          console.error('Erreur lors de la vérification du jour', error);
        },
      });
  }

  private checkFridayReminder(): void {
    if (this.excelExportService.isFriday()) {
      this.notificationService.info(
        "📅 C'est vendredi !",
        "N'oubliez pas de télécharger le rapport hebdomadaire des commandes",
        10000
      );
    }
  }

  // ============================================================================
  // GESTION DES FILTRES
  // ============================================================================
  onDateChange(newDate: string): void {
    this.filters.update((f) => ({ ...f, selectedDate: newDate }));
    this.loadData();
  }

  selectRestaurant(restaurantId: number | null): void {
    this.filters.update((f) => ({ ...f, selectedRestaurantId: restaurantId }));
  }

  onSearch(term: string): void {
    this.filters.update((f) => ({ ...f, searchTerm: term }));
  }

  updateShowCancelled(event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    this.filters.update((f) => ({ ...f, showCancelled: checked }));
  }

  // ============================================================================
  // GESTION DES COMMANDES (ADMIN)
  // ============================================================================
  adminCancelOrder(orderId: number): void {
    const reason = prompt(
      '⚠️ Pourquoi annulez-vous cette commande?\n\n(Ex: Non livrée, Erreur de commande, Annulation client)'
    );

    if (!reason || reason.trim() === '') {
      this.notificationService.warning(
        'Annulation ignorée',
        'Vous devez fournir une raison pour annuler la commande'
      );
      return;
    }

    if (!confirm(`Confirmer l'annulation de la commande #${orderId}?\n\nRaison: ${reason}`)) {
      return;
    }

    this.adminOrderService
      .adminCancelOrder(orderId, reason)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.notificationService.success(
            '✅ Commande annulée',
            response.message +
            (response.refundedAmount
              ? ` • ${this.formatAmount(response.refundedAmount)} FCFA remboursés`
              : '')
          );
          this.loadData();
        },
        error: (error) => {
          this.notificationService.error(
            '❌ Erreur',
            error.error?.message || "Impossible d'annuler la commande"
          );
        },
      });
  }

  reactivateOrder(orderId: number): void {
    const reason = prompt(
      "🔄 Pourquoi réactiver cette commande?\n\n(Ex: Erreur d'annulation, Commande finalement livrée)"
    );

    if (!reason || reason.trim() === '') {
      this.notificationService.warning(
        'Réactivation ignorée',
        'Vous devez fournir une raison pour réactiver la commande'
      );
      return;
    }

    if (!confirm(`Confirmer la réactivation de la commande #${orderId}?\n\nRaison: ${reason}`)) {
      return;
    }

    this.adminOrderService
      .reactivateOrder(orderId, reason)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.notificationService.success(
            '✅ Commande réactivée',
            response.message +
            (response.chargedAmount
              ? ` • ${this.formatAmount(response.chargedAmount)} FCFA débités`
              : '')
          );
          this.loadData();
        },
        error: (error) => {
          this.notificationService.error(
            '❌ Erreur',
            error.error?.message || 'Impossible de réactiver la commande'
          );
        },
      });
  }

  removeOrderItem(orderId: number, orderItemId: number, mealName: string): void {
    const reason = prompt(
      `⚠️ Pourquoi supprimer "${mealName}"?\n\n(Ex: Non livré, Erreur de saisie, Plat refusé)`
    );

    if (!reason || reason.trim() === '') {
      this.notificationService.warning(
        'Suppression ignorée',
        'Vous devez fournir une raison pour supprimer le plat'
      );
      return;
    }

    if (
      !confirm(
        `Confirmer la suppression de "${mealName}"?\n\nCommande #${orderId}\nRaison: ${reason}`
      )
    ) {
      return;
    }

    this.adminOrderService
      .removeOrderItem(orderId, orderItemId, reason)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.notificationService.success(
            '✅ Plat supprimé',
            response.message +
            (response.refundedAmount
              ? ` • ${this.formatAmount(response.refundedAmount)} FCFA remboursés`
              : '')
          );
          this.loadData();
        },
        error: (error) => {
          this.notificationService.error(
            '❌ Erreur',
            error.error?.message || 'Impossible de supprimer le plat'
          );
        },
      });
  }

  updateOrderItemQuantity(
    orderId: number,
    orderItemId: number,
    mealName: string,
    currentQuantity: number
  ): void {
    const newQuantityStr = prompt(
      `📝 Modifier la quantité de "${mealName}"\n\nQuantité actuelle: ${currentQuantity}\nNouvelle quantité:`,
      currentQuantity.toString()
    );

    if (!newQuantityStr) return;

    const newQuantity = parseInt(newQuantityStr, 10);

    if (isNaN(newQuantity) || newQuantity < 1) {
      this.notificationService.warning('Quantité invalide', 'La quantité doit être au moins 1');
      return;
    }

    if (newQuantity === currentQuantity) {
      this.notificationService.info('Aucun changement', 'La quantité est identique');
      return;
    }

    const reason = prompt(
      `⚠️ Pourquoi modifier la quantité de ${currentQuantity} à ${newQuantity}?\n\n(Ex: Quantité reçue différente, Erreur de saisie)`
    );

    if (!reason || reason.trim() === '') {
      this.notificationService.warning(
        'Modification ignorée',
        'Vous devez fournir une raison pour modifier la quantité'
      );
      return;
    }

    this.adminOrderService
      .updateOrderItemQuantity(orderId, orderItemId, newQuantity, reason)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          const amountMessage =
            response.refundedAmount && response.refundedAmount > 0
              ? ` • ${this.formatAmount(response.refundedAmount)} FCFA remboursés`
              : response.chargedAmount && response.chargedAmount > 0
                ? ` • ${this.formatAmount(response.chargedAmount)} FCFA débités`
                : '';

          this.notificationService.success(
            '✅ Quantité modifiée',
            response.message + amountMessage
          );
          this.loadData();
        },
        error: (error) => {
          this.notificationService.error(
            '❌ Erreur',
            error.error?.message || 'Impossible de modifier la quantité'
          );
        },
      });
  }

  // ============================================================================
  // MODAL CRÉER / MODIFIER COMMANDE
  // ============================================================================

  openAddOrderModal(): void {
    this.orderModalTitle.set('Ajouter une commande');
    this.orderModalEditOrderId.set(null);
    this.orderModalUserId.set(null);
    this.orderModalDate.set(this.filters().selectedDate);
    this.orderModalRestaurants.set([]);
    this.showOrderModal.set(true);
    this._loadOrderModalData();
  }

  openEditOrderModal(order: OrderDTO): void {
    this.orderModalTitle.set('Modifier la commande de ' + order.username);
    this.orderModalEditOrderId.set(order.id);
    this.orderModalUserId.set(order.userId);
    this.orderModalDate.set(order.orderDate.toString());
    this.orderModalRestaurants.set([]);
    this.showOrderModal.set(true);
    this._loadOrderModalData(order);
  }

  closeOrderModal(): void {
    this.showOrderModal.set(false);
  }

  private _loadOrderModalData(existingOrder?: OrderDTO): void {
    this.orderModalLoading.set(true);

    forkJoin({
      users: this.dashboardService.getAllUsers(),
      restaurants: this.restaurantService.getRestaurants(),
      menus: this.menuClientService.getMenusByDate(this.orderModalDate()),
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: ({ users, restaurants, menus }) => {
        this.allUsers.set(users);

        const modalRestaurants: ModalRestaurant[] = restaurants
          .map(r => {
            const menu = menus.find(m => m.restaurantId === r.id);
            if (!menu || !menu.meals || menu.meals.length === 0) return null;
            const meals: ModalMealItem[] = menu.meals.map(meal => {
              let qty = 0;
              if (existingOrder) {
                const existingItem = existingOrder.items.find(
                  i => i.mealId === meal.id && i.restaurantId === r.id
                );
                qty = existingItem?.quantity ?? 0;
              }
              return {
                mealId: meal.id!,
                mealName: meal.mealName,
                restaurantId: r.id,
                restaurantName: r.restaurantName,
                unitPrice: meal.unitPrice,
                quantity: qty,
              };
            });
            return { restaurantId: r.id, restaurantName: r.restaurantName, meals, expanded: true };
          })
          .filter((r): r is ModalRestaurant => r !== null);

        this.orderModalRestaurants.set(modalRestaurants);
        this.orderModalLoading.set(false);
      },
      error: () => {
        this.orderModalLoading.set(false);
        this.notificationService.error('Erreur', 'Impossible de charger les données du menu');
      },
    });
  }

  toggleModalRestaurant(restaurantId: number): void {
    this.orderModalRestaurants.update(rs =>
      rs.map(r => r.restaurantId === restaurantId ? { ...r, expanded: !r.expanded } : r)
    );
  }

  increaseModalQty(restaurantId: number, mealId: number): void {
    this.orderModalRestaurants.update(rs =>
      rs.map(r => r.restaurantId !== restaurantId ? r : {
        ...r,
        meals: r.meals.map(m => m.mealId !== mealId ? m : { ...m, quantity: m.quantity + 1 }),
      })
    );
  }

  decreaseModalQty(restaurantId: number, mealId: number): void {
    this.orderModalRestaurants.update(rs =>
      rs.map(r => r.restaurantId !== restaurantId ? r : {
        ...r,
        meals: r.meals.map(m => m.mealId !== mealId || m.quantity === 0 ? m : { ...m, quantity: m.quantity - 1 }),
      })
    );
  }

  submitOrderModal(): void {
    if (!this.orderModalUserId()) {
      this.notificationService.warning('Utilisateur requis', 'Veuillez sélectionner un utilisateur');
      return;
    }

    const items: AdminOrderItemData[] = this.orderModalRestaurants()
      .flatMap(r => r.meals.filter(m => m.quantity > 0).map(m => ({
        mealId: m.mealId,
        restaurantId: m.restaurantId,
        quantity: m.quantity,
        unitPrice: m.unitPrice,
      })));

    if (items.length === 0) {
      this.notificationService.warning('Panier vide', 'Sélectionnez au moins un plat');
      return;
    }

    const request: AdminCreateOrderRequest = {
      userId: this.orderModalUserId()!,
      date: this.orderModalDate(),
      items,
    };

    this.orderModalSubmitting.set(true);

    this.adminOrderService.adminCreateOrder(request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.orderModalSubmitting.set(false);
          this.notificationService.success('✅ Commande créée', response.message);
          this.closeOrderModal();
          this.loadData();
        },
        error: (error) => {
          this.orderModalSubmitting.set(false);
          this.notificationService.error('❌ Erreur', error.error?.error || 'Impossible de créer la commande');
        },
      });
  }

  // ============================================================================
  // MODAL STATISTIQUES RESTAURANT
  // ============================================================================
  openRestaurantStats(restaurant: RestaurantStats): void {
    const data = this.dashboardData();
    if (!data) return;

    const mealStatsMap = new Map<string, MealStat>();

    data.userOrders.forEach((user) => {
      user.orders
        .filter((order) => order.status === 'CONFIRMED')
        .forEach((order) => {
          order.items
            .filter((item) => item.restaurantId === restaurant.restaurantId)
            .forEach((item) => {
              const existing = mealStatsMap.get(item.mealName) || {
                mealName: item.mealName,
                quantity: 0,
                totalAmount: 0,
              };

              existing.quantity += item.quantity;
              existing.totalAmount += item.subtotal;

              mealStatsMap.set(item.mealName, existing);
            });
        });
    });

    const meals = Array.from(mealStatsMap.values()).sort((a, b) => b.quantity - a.quantity);

    this.selectedRestaurantStats.set({ restaurant, meals });
    this.showStatsModal.set(true);
  }

  closeStatsModal(): void {
    this.showStatsModal.set(false);
    this.selectedRestaurantStats.set(null);
  }

  exportRestaurant(restaurant: RestaurantStats): void {
    const data = this.dashboardData();
    if (!data) return;

    const orders = data.userOrders.flatMap((u) => u.orders);
    const restaurantOrders = orders.filter((o) =>
      o.items.some((item) => item.restaurantId === restaurant.restaurantId)
    );

    this.dashboardService.exportRestaurantCSV(
      restaurant.restaurantName,
      restaurantOrders,
      data.date
    );

    this.notificationService.success(
      'Export réussi',
      `Les données du restaurant "${restaurant.restaurantName}" ont été exportées`
    );
  }

  // ============================================================================
  // MODAL PAIEMENT
  // ============================================================================
  openPaymentModal(): void {
    const validationState = this.paymentValidationState();

    if (!validationState?.canValidate) {
      this.notificationService.warning(
        '⚠️ Validation impossible',
        validationState?.message || 'La validation ne peut être effectuée que le vendredi',
        7000
      );
      return;
    }

    this.isLoadingPayment.set(true);
    this.showPaymentModal.set(true);

    const startDate = validationState.weekStartDate;
    const endDate = validationState.weekEndDate;

    this.paymentService
      .getPaymentSummary(startDate, endDate)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (summary) => {
          this.paymentSummary.set(summary);
          this.isLoadingPayment.set(false);

          if (summary.restaurants.length === 0) {
            this.notificationService.warning(
              'Aucune donnée',
              'Aucune commande confirmée trouvée pour cette semaine'
            );
          } else {
            this.notificationService.success(
              'Récapitulatif chargé',
              `Semaine du ${this.formatDate(summary.startDate)} au ${this.formatDate(
                summary.endDate
              )} • ` +
              `${summary.restaurants.length} restaurant(s) • ${summary.globalStats.totalOrders} commande(s)`,
              8000
            );
          }
        },
        error: (error) => {
          this.isLoadingPayment.set(false);
          this.showPaymentModal.set(false);
          this.notificationService.error(
            'Erreur de chargement',
            error.error?.message || 'Impossible de charger le récapitulatif de paiement',
            7000
          );
        },
      });
  }

  validatePayment(): void {
    const summary = this.paymentSummary();
    if (!summary) return;

    const confirmMessage =
      '⚠️ CONFIRMATION REQUISE\n\n' +
      'Voulez-vous vraiment clôturer cette semaine ?\n\n' +
      `📅 Semaine: ${this.formatDate(summary.startDate)} → ${this.formatDate(summary.endDate)}\n` +
      `🏪 ${summary.restaurants.length} restaurant(s)\n` +
      `💰 ${this.formatAmount(summary.globalStats.totalAmountWithoutSubsidy)} FCFA à payer\n` +
      `🚚 ${this.formatAmount(summary.globalStats.totalDeliveryFees)} FCFA frais de livraison\n` +
      `📦 ${summary.globalStats.totalOrders} commande(s)\n\n` +
      '✅ Le rapport Excel sera automatiquement téléchargé\n' +
      '⚠️ Les frais de livraison seront calculés et débités\n' +
      '❌ Cette action est IRRÉVERSIBLE';

    if (!confirm(confirmMessage)) {
      return;
    }

    this.isLoadingPayment.set(true);

    this.paymentService
      .validatePaymentAndDownload({
        startDate: summary.startDate,
        endDate: summary.endDate,
      })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.notificationService.success(
            '✅ Clôture réussie !',
            `${response.validation.affectedOrders} commande(s) traitée(s) • ` +
            `${response.validation.affectedUsers} utilisateur(s) concerné(s) • ` +
            `📥 Rapport Excel téléchargé (${(response.excel.size / 1024).toFixed(0)} Ko)`,
            15000
          );

          this.closePaymentModal();
          this.loadData();
          this.loadLastPaymentDate();
          this.checkPaymentValidationState();
        },
        error: (error) => {
          this.isLoadingPayment.set(false);
          this.notificationService.error(
            'Échec de clôture',
            error.error?.error || error.error?.message || 'Impossible de clôturer la semaine',
            10000
          );
        },
      });
  }

  closePaymentModal(): void {
    this.showPaymentModal.set(false);
    this.paymentSummary.set(null);
    this.isLoadingPayment.set(false);
  }

  // ============================================================================
  // EXPORT EXCEL
  // ============================================================================
  downloadWeeklyExcel(): void {
    this.excelExportService.downloadWeeklyReport();
  }

  previewWeeklyReport(): void {
    const week = this.excelExportService.getCurrentWeek();

    this.excelExportService
      .previewWeeklyReport(week.monday, week.friday)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (preview) => {
          console.log('Aperçu des données:', preview);
          this.notificationService.success(
            'Aperçu prêt',
            `${preview.userConsumptions.length} utilisateur(s) trouvé(s) pour cette semaine`
          );
        },
        error: (error) => {
          this.notificationService.error('❌ Erreur', "Impossible de charger l'aperçu");
        },
      });
  }

  // ============================================================================
  // UTILITAIRES DE FORMATAGE
  // ============================================================================
  formatAmount(amount: number): string {
    return this.dashboardService.formatAmount(amount);
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString('fr-FR', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    });
  }

  getBalanceColor(balance: number): string {
    if (balance < 0) return 'text-[#E84141]';
    if (balance > 0) return 'text-[#99CFBD]';
    return 'text-[#303131]';
  }
}
