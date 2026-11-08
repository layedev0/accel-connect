package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.AdminCreateOrderRequest;
import heritage.africca.accel_connect.dto.OrderCreationPayload;
import heritage.africca.accel_connect.dto.OrderDTO;
import heritage.africca.accel_connect.dto.OrderItemData;
import heritage.africca.accel_connect.entity.Meal;
import heritage.africca.accel_connect.entity.Order;
import heritage.africca.accel_connect.entity.OrderItem;
import heritage.africca.accel_connect.entity.Restaurant;
import heritage.africca.accel_connect.entity.User;
import heritage.africca.accel_connect.entity.UserBalance;
import heritage.africca.accel_connect.entity.enumeration.OrderMode;
import heritage.africca.accel_connect.entity.enumeration.OrderStatus;
import heritage.africca.accel_connect.repository.MealRepository;
import heritage.africca.accel_connect.repository.OrderItemRepository;
import heritage.africca.accel_connect.repository.OrderRepository;
import heritage.africca.accel_connect.repository.RestaurantRepository;
import heritage.africca.accel_connect.repository.UserBalanceRepository;
import heritage.africca.accel_connect.repository.UserRepository;
import heritage.africca.accel_connect.service.Impl.NextcloudTalkService;
import heritage.africca.accel_connect.service.Impl.OrderServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 🛒 Commande du jour : ajout d'articles à une commande déjà confirmée
 * (une seule commande par utilisateur et par jour)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OrderServiceImpl")
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private MealRepository mealRepository;
    @Mock private RestaurantRepository restaurantRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserBalanceRepository userBalanceRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private DeliveryFeeService deliveryFeeService;
    @Mock private NextcloudTalkService nextcloudTalkService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User user;
    private UserBalance balance;
    private Restaurant restaurant;
    private Restaurant restaurantBis;
    private Meal burger;
    private Meal salade;
    private Authentication authentication;
    private MockedStatic<LocalTime> localTimeMock;

    /** Heures figées, construites AVANT l'activation du mock statique */
    private static final LocalTime BEFORE_DEADLINE = LocalTime.of(9, 0);
    private static final LocalTime AFTER_DEADLINE = LocalTime.of(12, 30);

    @BeforeEach
    void setUp() {
        // Fige l'heure avant la deadline de 12h pour rendre les tests déterministes
        localTimeMock = mockStatic(LocalTime.class, CALLS_REAL_METHODS);
        localTimeMock.when(LocalTime::now).thenReturn(BEFORE_DEADLINE);

        user = new User();
        user.setId(1L);
        user.setKeycloakId("kc-1");
        user.setUsername("pape");
        user.setIntern(false);

        balance = new UserBalance();
        balance.setUser(user);

        restaurant = new Restaurant();
        restaurant.setId(10L);
        restaurant.setRestaurantName("Chez Fatou");

        restaurantBis = new Restaurant();
        restaurantBis.setId(20L);
        restaurantBis.setRestaurantName("Le Dakarois");

        burger = new Meal();
        burger.setId(100L);
        burger.setMealName("Burger");

        salade = new Meal();
        salade.setId(200L);
        salade.setMealName("Salade");

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("kc-1")
                .build();
        authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(jwt);

        when(userRepository.findByKeycloakId("kc-1")).thenReturn(Optional.of(user));
        when(userBalanceRepository.findByUserId(1L)).thenReturn(Optional.of(balance));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        localTimeMock.close();
    }

    // ========== UTILITAIRES ==========

    /**
     * Construit la commande CONFIRMÉE du jour déjà enregistrée en base
     */
    private Order existingOrder(Meal meal, int quantity, double unitPrice) {
        Order order = new Order();
        order.setId(1L);
        order.setUser(user);
        order.setOrderDate(LocalDate.now());
        order.setStatus(OrderStatus.CONFIRMED);

        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setMeal(meal);
        item.setRestaurant(restaurant);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        order.addOrderItem(item);

        order.calculateSubtotal();
        order.setSubsidyAmount(1000.0);
        order.setAmountAfterSubsidy(Math.max(0, order.getSubtotal() - 1000.0));
        order.setDeliveryFeeShare(0.0);
        order.setTotalAmount(order.getAmountAfterSubsidy());

        // Le solde reflète ce qui a déjà été débité pour cette commande
        balance.addExpense(order.getTotalAmount());

        when(orderRepository.findByUserAndOrderDate(eq(user), any(LocalDate.class)))
                .thenReturn(Optional.of(order));
        return order;
    }

    private OrderCreationPayload payload(OrderMode mode, Meal meal, int quantity, double unitPrice) {
        return payload(mode, itemData(meal, restaurant, quantity, unitPrice));
    }

    private OrderCreationPayload payload(OrderMode mode, OrderItemData... items) {
        return new OrderCreationPayload(List.of(items), mode);
    }

    private OrderItemData itemData(Meal meal, Restaurant fromRestaurant, int quantity, double unitPrice) {
        OrderItemData data = new OrderItemData();
        data.setMealId(meal.getId());
        data.setRestaurantId(fromRestaurant.getId());
        data.setQuantity(quantity);
        data.setUnitPrice(unitPrice);
        return data;
    }

    /**
     * Retrouve une ligne par plat ET restaurant (deux lignes peuvent porter le même plat)
     */
    private OrderItem itemOf(Order order, Meal meal, Restaurant fromRestaurant) {
        return order.getOrderItems().stream()
                .filter(i -> i.getMeal().getId().equals(meal.getId())
                        && i.getRestaurant().getId().equals(fromRestaurant.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Ligne absente: " + meal.getMealName()
                        + " chez " + fromRestaurant.getRestaurantName()));
    }

    private OrderItem itemOf(Order order, Meal meal) {
        return order.getOrderItems().stream()
                .filter(i -> i.getMeal().getId().equals(meal.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Plat absent de la commande: " + meal.getMealName()));
    }

    @Nested
    @DisplayName("Mode ADD - ajout à la commande confirmée")
    class AddMode {

        @Test
        @DisplayName("cumule la quantité quand le plat est déjà commandé")
        void cumuleLaQuantiteDuMemePlat() {
            Order order = existingOrder(burger, 1, 2000.0);

            OrderDTO result = orderService.createOrder(authentication, payload(OrderMode.ADD, burger, 2, 2000.0));

            assertEquals(1, order.getOrderItems().size(), "Le plat ne doit pas être dupliqué");
            OrderItem item = itemOf(order, burger);
            assertEquals(3, item.getQuantity());
            assertEquals(6000.0, item.getSubtotal(), "Le sous-total de la ligne doit être recalculé");
            assertEquals(6000.0, result.getSubtotal());
        }

        @Test
        @DisplayName("ajoute une nouvelle ligne pour un plat non commandé, sans toucher aux anciennes")
        void ajouteUnNouveauPlat() {
            Order order = existingOrder(burger, 1, 2000.0);
            when(mealRepository.findById(200L)).thenReturn(Optional.of(salade));
            when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));

            orderService.createOrder(authentication, payload(OrderMode.ADD, salade, 1, 1500.0));

            assertEquals(2, order.getOrderItems().size());
            assertEquals(1, itemOf(order, burger).getQuantity(), "L'ancien plat reste inchangé");
            assertEquals(1500.0, itemOf(order, salade).getSubtotal());
            assertEquals(3500.0, order.getSubtotal());
        }

        @Test
        @DisplayName("n'applique la subvention qu'une seule fois sur le sous-total fusionné")
        void appliqueLaSubventionUneSeuleFois() {
            // 1ʳᵉ commande à 800 FCFA : entièrement couverte par la subvention
            Order order = existingOrder(burger, 1, 800.0);
            assertEquals(0.0, order.getAmountAfterSubsidy());
            when(mealRepository.findById(200L)).thenReturn(Optional.of(salade));
            when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));

            OrderDTO result = orderService.createOrder(authentication, payload(OrderMode.ADD, salade, 1, 700.0));

            assertEquals(1500.0, result.getSubtotal());
            assertEquals(1000.0, result.getSubsidyAmount(), "Une seule subvention pour la journée");
            assertEquals(500.0, result.getAmountAfterSubsidy(), "Seul le dépassement est facturé");
        }

        @Test
        @DisplayName("recalcule le solde : rembourse l'ancien total puis débite le nouveau")
        void recalculeLeSolde() {
            existingOrder(burger, 1, 2000.0); // déjà débité: 1000 FCFA
            assertEquals(1000.0, balance.getTotalSpent());

            orderService.createOrder(authentication, payload(OrderMode.ADD, burger, 2, 2000.0));

            // 6000 - 1000 de subvention = 5000 (et non 1000 + 5000)
            assertEquals(5000.0, balance.getTotalSpent());
            assertEquals(-5000.0, balance.getBalance());
            verify(userBalanceRepository).save(balance);
        }

        @Test
        @DisplayName("recalcule les frais de livraison du jour")
        void recalculeLesFraisDeLivraison() {
            Order order = existingOrder(burger, 1, 2000.0);

            orderService.createOrder(authentication, payload(OrderMode.ADD, burger, 1, 2000.0));

            verify(deliveryFeeService).recalculateDeliveryFeesForDate(order.getOrderDate());
        }

        @Test
        @DisplayName("est le mode par défaut quand le client n'envoie pas de mode")
        void modeParDefaut() {
            Order order = existingOrder(burger, 1, 2000.0);

            orderService.createOrder(authentication, payload(null, burger, 1, 2000.0));

            assertEquals(2, itemOf(order, burger).getQuantity());
        }

        @Test
        @DisplayName("ne cumule pas le même plat commandé dans un autre restaurant")
        void neCumulePasUnMemePlatDUnAutreRestaurant() {
            Order order = existingOrder(burger, 1, 2000.0);
            when(mealRepository.findById(100L)).thenReturn(Optional.of(burger));
            when(restaurantRepository.findById(20L)).thenReturn(Optional.of(restaurantBis));

            orderService.createOrder(authentication,
                    payload(OrderMode.ADD, itemData(burger, restaurantBis, 1, 2500.0)));

            assertEquals(2, order.getOrderItems().size(), "Deux restaurants = deux lignes distinctes");
            assertEquals(1, itemOf(order, burger, restaurant).getQuantity());
            assertEquals(1, itemOf(order, burger, restaurantBis).getQuantity());
            assertEquals(4500.0, order.getSubtotal());
        }

        @Test
        @DisplayName("conserve le prix unitaire de la ligne existante même si le client en envoie un autre")
        void conserveLePrixUnitaireDeLaLigneExistante() {
            Order order = existingOrder(burger, 1, 2000.0);

            orderService.createOrder(authentication, payload(OrderMode.ADD, burger, 1, 3000.0));

            OrderItem item = itemOf(order, burger);
            assertEquals(2, item.getQuantity());
            assertEquals(2000.0, item.getUnitPrice(), "Pas de re-tarification des articles déjà confirmés");
            assertEquals(4000.0, item.getSubtotal());
        }

        @Test
        @DisplayName("traite un panier mixte (plat déjà commandé + nouveau plat) en une seule requête")
        void traiteUnPanierMixteEnUneSeuleRequete() {
            Order order = existingOrder(burger, 1, 2000.0);
            when(mealRepository.findById(200L)).thenReturn(Optional.of(salade));
            when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));

            OrderDTO result = orderService.createOrder(authentication, payload(OrderMode.ADD,
                    itemData(burger, restaurant, 2, 2000.0),
                    itemData(salade, restaurant, 1, 1500.0)));

            assertEquals(2, order.getOrderItems().size());
            assertEquals(3, itemOf(order, burger).getQuantity());
            assertEquals(1, itemOf(order, salade).getQuantity());
            assertEquals(7500.0, result.getSubtotal());
            assertEquals(6500.0, result.getAmountAfterSubsidy());
        }

        @Test
        @DisplayName("rembourse aussi les frais de livraison déjà débités et remet la part à zéro")
        void rembourseAussiLesFraisDeLivraisonDejaDebites() {
            Order order = existingOrder(burger, 1, 2000.0); // 1000 après subvention
            // Des frais de livraison ont déjà été répartis sur cette commande
            order.addDeliveryFees(500.0);
            balance.addExpense(500.0);
            assertEquals(1500.0, order.getTotalAmount());
            assertEquals(1500.0, balance.getTotalSpent());

            orderService.createOrder(authentication, payload(OrderMode.ADD, burger, 1, 2000.0));

            assertEquals(0.0, order.getDeliveryFeeShare(),
                    "Les frais seront réinjectés par recalculateDeliveryFeesForDate");
            assertEquals(3000.0, order.getAmountAfterSubsidy());
            // 1500 remboursés puis 3000 débités, et non 1500 + 3000
            assertEquals(3000.0, balance.getTotalSpent());
        }

        @Test
        @DisplayName("applique la subvention stagiaire de 2000 FCFA")
        void appliqueLaSubventionInternDe2000() {
            user.setIntern(true);
            Order order = existingOrder(burger, 1, 2000.0);
            when(mealRepository.findById(200L)).thenReturn(Optional.of(salade));
            when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));

            OrderDTO result = orderService.createOrder(authentication, payload(OrderMode.ADD, salade, 1, 500.0));

            assertEquals(2500.0, result.getSubtotal());
            assertEquals(2000.0, result.getSubsidyAmount());
            assertEquals(500.0, result.getAmountAfterSubsidy());
            assertEquals(order.getAmountAfterSubsidy(), result.getAmountAfterSubsidy());
        }

        @Test
        @DisplayName("échoue si le solde de l'utilisateur est introuvable")
        void echoueSiLeSoldeEstIntrouvable() {
            existingOrder(burger, 1, 2000.0);
            when(userBalanceRepository.findByUserId(1L)).thenReturn(Optional.empty());

            RuntimeException error = assertThrows(RuntimeException.class,
                    () -> orderService.createOrder(authentication, payload(OrderMode.ADD, burger, 1, 2000.0)));

            assertTrue(error.getMessage().contains("solde"));
            verify(orderRepository, never()).save(any(Order.class));
        }
    }

    @Nested
    @DisplayName("Construction des lignes - références invalides")
    class BuildOrderItemErrors {

        @Test
        @DisplayName("refuse un plat introuvable")
        void refuseUnPlatIntrouvable() {
            existingOrder(burger, 1, 2000.0);
            when(mealRepository.findById(200L)).thenReturn(Optional.empty());

            RuntimeException error = assertThrows(RuntimeException.class,
                    () -> orderService.createOrder(authentication, payload(OrderMode.ADD, salade, 1, 1500.0)));

            assertTrue(error.getMessage().contains("n'existe pas ou n'est plus disponible"));
        }

        @Test
        @DisplayName("refuse un restaurant introuvable")
        void refuseUnRestaurantIntrouvable() {
            existingOrder(burger, 1, 2000.0);
            when(mealRepository.findById(200L)).thenReturn(Optional.of(salade));
            when(restaurantRepository.findById(10L)).thenReturn(Optional.empty());

            RuntimeException error = assertThrows(RuntimeException.class,
                    () -> orderService.createOrder(authentication, payload(OrderMode.ADD, salade, 1, 1500.0)));

            assertTrue(error.getMessage().contains("Le restaurant n'existe pas"));
        }
    }

    @Nested
    @DisplayName("Mode REPLACE - modification de la commande")
    class ReplaceMode {

        @Test
        @DisplayName("remplace les anciens articles au lieu de les cumuler")
        void remplaceLesArticles() {
            Order order = existingOrder(burger, 2, 2000.0);
            when(mealRepository.findById(200L)).thenReturn(Optional.of(salade));
            when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));

            OrderDTO result = orderService.createOrder(authentication, payload(OrderMode.REPLACE, salade, 1, 1500.0));

            assertEquals(1, order.getOrderItems().size());
            assertEquals(200L, order.getOrderItems().get(0).getMeal().getId());
            assertEquals(1500.0, result.getSubtotal());
            assertEquals(500.0, result.getAmountAfterSubsidy());
            // Ancien total (3000) remboursé, nouveau montant (500) débité
            assertEquals(500.0, balance.getTotalSpent());
        }
    }

    @Nested
    @DisplayName("Règles inchangées")
    class ExistingRules {

        @Test
        @DisplayName("refuse toute commande après une annulation du jour, quel que soit le mode")
        void refuseApresAnnulation() {
            Order cancelled = existingOrder(burger, 1, 2000.0);
            cancelled.setStatus(OrderStatus.CANCELLED);

            RuntimeException addError = assertThrows(RuntimeException.class,
                    () -> orderService.createOrder(authentication, payload(OrderMode.ADD, burger, 1, 2000.0)));
            RuntimeException replaceError = assertThrows(RuntimeException.class,
                    () -> orderService.createOrder(authentication, payload(OrderMode.REPLACE, burger, 1, 2000.0)));

            assertTrue(addError.getMessage().contains("annulé"));
            assertTrue(replaceError.getMessage().contains("annulé"));
            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("refuse un panier vide")
        void refusePanierVide() {
            RuntimeException error = assertThrows(RuntimeException.class,
                    () -> orderService.createOrder(authentication, new OrderCreationPayload(List.of(), OrderMode.ADD)));

            assertTrue(error.getMessage().contains("vide"));
        }

        @Test
        @DisplayName("refuse une commande après la deadline de 20h")
        void refuseApresDeadline() {
            localTimeMock.when(LocalTime::now).thenReturn(AFTER_DEADLINE);

            RuntimeException error = assertThrows(RuntimeException.class,
                    () -> orderService.createOrder(authentication, payload(OrderMode.ADD, burger, 1, 2000.0)));

            assertTrue(error.getMessage().contains("trop tard"));
        }

        @Test
        @DisplayName("crée une nouvelle commande quand il n'y en a pas encore aujourd'hui")
        void creeUneNouvelleCommande() {
            when(orderRepository.findByUserAndOrderDate(eq(user), any(LocalDate.class)))
                    .thenReturn(Optional.empty());
            when(mealRepository.findById(100L)).thenReturn(Optional.of(burger));
            when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));

            OrderDTO result = orderService.createOrder(authentication, payload(OrderMode.ADD, burger, 1, 2000.0));

            assertEquals(2000.0, result.getSubtotal());
            assertEquals(1000.0, result.getAmountAfterSubsidy());
            assertEquals(1000.0, balance.getTotalSpent());
        }
    }

    @Nested
    @DisplayName("Création admin - remplacement (non-régression du refactor)")
    class AdminCreateOrder {

        @Test
        @DisplayName("remplace les articles de la commande existante au lieu de les cumuler")
        void remplaceLaCommandeExistante() {
            Order order = existingOrder(burger, 2, 2000.0); // 4000 - 1000 = 3000 débités
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(mealRepository.findById(200L)).thenReturn(Optional.of(salade));
            when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));

            AdminCreateOrderRequest request = new AdminCreateOrderRequest(
                    1L, LocalDate.now(), List.of(itemData(salade, restaurant, 1, 1500.0)));

            OrderDTO result = orderService.adminCreateOrder(request);

            assertEquals(1, order.getOrderItems().size(), "Les anciens articles sont remplacés");
            assertEquals(200L, order.getOrderItems().get(0).getMeal().getId());
            assertEquals(1500.0, result.getSubtotal());
            assertEquals(500.0, result.getAmountAfterSubsidy());
            assertEquals(OrderStatus.CONFIRMED.name(), result.getStatus());
            // Ancien total (3000) remboursé, nouveau montant (500) débité
            assertEquals(500.0, balance.getTotalSpent());
        }
    }
}
