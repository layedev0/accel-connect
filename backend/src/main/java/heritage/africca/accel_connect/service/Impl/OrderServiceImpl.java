package heritage.africca.accel_connect.service.Impl;

import heritage.africca.accel_connect.dto.*;
import heritage.africca.accel_connect.dto.AdminCreateOrderRequest;
import heritage.africca.accel_connect.entity.*;
import heritage.africca.accel_connect.entity.enumeration.OrderMode;
import heritage.africca.accel_connect.entity.enumeration.OrderStatus;
import heritage.africca.accel_connect.repository.*;
import heritage.africca.accel_connect.service.DeliveryFeeService;
import heritage.africca.accel_connect.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final MealRepository mealRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final UserBalanceRepository userBalanceRepository;
    private final OrderItemRepository orderItemRepository;
    private final DeliveryFeeService deliveryFeeService;
    private final NextcloudTalkService nextcloudTalkService;

    private static final double SUBSIDY_AMOUNT_NORMAL = 1000.0;
    private static final double SUBSIDY_AMOUNT_INTERN = 2000.0;
    private static final int MODIFICATION_DEADLINE_HOUR = 12;

    private double getSubsidyForUser(User user) {
        return Boolean.TRUE.equals(user.getIntern()) ? SUBSIDY_AMOUNT_INTERN : SUBSIDY_AMOUNT_NORMAL;
    }

    private void sendOrderConfirmationNotification(User user, Order order) {
        String mealList = order.getOrderItems().stream()
                .map(item -> item.getQuantity() > 1
                        ? item.getQuantity() + "x " + item.getMeal().getMealName()
                        : item.getMeal().getMealName())
                .collect(Collectors.joining(", "));
        String message = "Bonjour, votre commande de " + mealList
                + " est bien confirmée, vous serez notifié quand elle sera disponible !";
        String nextcloudId = "SSO-" + user.getKeycloakId();
        CompletableFuture.runAsync(() -> nextcloudTalkService.sendDirectMessage(nextcloudId, message));
    }

    // ========== MÉTHODES UTILISATEUR (inchangées) ==========

    private boolean isBeforeDeadline() {
        LocalTime now = LocalTime.now();
        return now.getHour() < MODIFICATION_DEADLINE_HOUR;
    }

    @Transactional
    @Override
    public OrderDTO createOrder(Authentication auth, OrderCreationPayload payload) {
        log.info("🛒 Tentative de création/modification de commande...");

        if (!isBeforeDeadline()) {
            throw new RuntimeException(
                    "Il est trop tard pour commander ou modifier aujourd'hui. Les commandes sont acceptées jusqu'à "
                            + MODIFICATION_DEADLINE_HOUR + "h.");
        }

        if (payload.getItems() == null || payload.getItems().isEmpty()) {
            throw new RuntimeException("Votre panier est vide. Ajoutez des plats avant de commander.");
        }

        User user = getCurrentUser(auth);
        LocalDate today = LocalDate.now();

        return orderRepository.findByUserAndOrderDate(user, today)
                .map(existingOrder -> {
                    if (existingOrder.getStatus() == OrderStatus.CANCELLED) {
                        throw new RuntimeException(
                                "Vous avez annulé votre commande du jour. Vous ne pouvez plus commander aujourd'hui.");
                    }
                    if (payload.getMode() == OrderMode.REPLACE) {
                        log.info("🔄 Remplacement de la commande existante #{}...", existingOrder.getId());
                        return updateExistingOrder(existingOrder, payload);
                    }
                    log.info("➕ Ajout d'articles à la commande existante #{}...", existingOrder.getId());
                    return mergeIntoExistingOrder(existingOrder, payload);
                })
                .orElseGet(() -> {
                    log.info("Création d'une nouvelle commande...");
                    return createNewOrder(user, today, payload);
                });
    }

    private OrderDTO createNewOrder(User user, LocalDate orderDate, OrderCreationPayload payload) {
        UserBalance balance = userBalanceRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserBalance newBalance = new UserBalance();
                    newBalance.setUser(user);
                    return userBalanceRepository.save(newBalance);
                });

        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(orderDate);

        for (OrderItemData itemData : payload.getItems()) {
            order.addOrderItem(buildOrderItem(itemData));
        }

        applyOrderTotals(order, user);
        order.setStatus(OrderStatus.CONFIRMED);

        Order savedOrder = orderRepository.save(order);

        if (savedOrder.getAmountAfterSubsidy() > 0) {
            balance.addExpense(savedOrder.getAmountAfterSubsidy());
            userBalanceRepository.save(balance);
            log.info("💳 UserBalance débité de {} FCFA", savedOrder.getAmountAfterSubsidy());
        }

        // 🔄 RECALCULER LES FRAIS DE LIVRAISON EN TEMPS RÉEL
        deliveryFeeService.recalculateDeliveryFeesForDate(savedOrder.getOrderDate());

        sendOrderConfirmationNotification(user, savedOrder);
        log.info("✅ Commande #{} créée - Total: {} FCFA", savedOrder.getId(), savedOrder.getTotalAmount());
        return convertToDTO(savedOrder);
    }

    private OrderDTO updateExistingOrder(Order existingOrder, OrderCreationPayload payload) {
        User user = existingOrder.getUser();
        UserBalance balance = userBalanceRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException(
                        "Votre solde n'a pas pu être trouvé. Veuillez contacter le support."));

        // Sauvegarder l'ancien montant TOTAL (avec frais) pour le rembourser
        double oldTotalAmount = existingOrder.getTotalAmount();

        // Rembourser le MONTANT TOTAL (commande + frais)
        if (oldTotalAmount > 0) {
            balance.refundExpense(oldTotalAmount);
            log.info("↩️ Ancien montant TOTAL remboursé: {} FCFA (commande + frais)", oldTotalAmount);
        }

        existingOrder.getOrderItems().clear();

        for (OrderItemData itemData : payload.getItems()) {
            existingOrder.addOrderItem(buildOrderItem(itemData));
        }

        // Ne pas encore calculer les frais - sera fait par recalculateDeliveryFeesForDate()
        applyOrderTotals(existingOrder, user);

        Order updatedOrder = orderRepository.save(existingOrder);

        // Débiter le nouveau montant de commande (sans frais pour l'instant)
        double newAmount = updatedOrder.getAmountAfterSubsidy();
        if (newAmount > 0) {
            balance.addExpense(newAmount);
            log.info("💳 Nouveau montant débité: {} FCFA (sans frais)", newAmount);
        }
        userBalanceRepository.save(balance);

        // 🔄 RECALCULER LES FRAIS DE LIVRAISON EN TEMPS RÉEL
        // Ceci va ajouter les nouveaux frais dans le UserBalance
        deliveryFeeService.recalculateDeliveryFeesForDate(updatedOrder.getOrderDate());

        sendOrderConfirmationNotification(user, updatedOrder);
        log.info("🔄 Commande #{} modifiée - Nouveau total: {} FCFA", updatedOrder.getId(),
                updatedOrder.getTotalAmount());
        return convertToDTO(updatedOrder);
    }

    /**
     *  Ajoute les articles du panier à la commande CONFIRMÉE du jour.
     * Un article déjà présent (même plat + même restaurant) voit sa quantité cumulée,
     * les autres sont ajoutés en nouvelles lignes. La subvention reste appliquée UNE
     * SEULE FOIS sur le sous-total fusionné, et le solde est recalculé en conséquence.
     */
    private OrderDTO mergeIntoExistingOrder(Order existingOrder, OrderCreationPayload payload) {
        User user = existingOrder.getUser();
        UserBalance balance = userBalanceRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException(
                        "Votre solde n'a pas pu être trouvé. Veuillez contacter le support."));

        // Rembourser le MONTANT TOTAL actuel (commande + frais) avant recalcul
        double oldTotalAmount = existingOrder.getTotalAmount();
        if (oldTotalAmount > 0) {
            balance.refundExpense(oldTotalAmount);
            log.info("↩️ Ancien montant TOTAL remboursé: {} FCFA (commande + frais)", oldTotalAmount);
        }

        for (OrderItemData itemData : payload.getItems()) {
            OrderItem existingItem = findMatchingItem(existingOrder, itemData);

            if (existingItem != null) {
                existingItem.setQuantity(existingItem.getQuantity() + itemData.getQuantity());
                // Recalcul immédiat: Order.calculateSubtotal() somme le champ subtotal des items
                existingItem.calculateSubtotal();
                log.info("➕ Plat #{} cumulé: nouvelle quantité {}", itemData.getMealId(),
                        existingItem.getQuantity());
            } else {
                existingOrder.addOrderItem(buildOrderItem(itemData));
                log.info("➕ Nouveau plat #{} ajouté à la commande", itemData.getMealId());
            }
        }

        // Ne pas encore calculer les frais - sera fait par recalculateDeliveryFeesForDate()
        applyOrderTotals(existingOrder, user);

        Order updatedOrder = orderRepository.save(existingOrder);

        // Débiter le nouveau montant de commande (sans frais pour l'instant)
        double newAmount = updatedOrder.getAmountAfterSubsidy();
        if (newAmount > 0) {
            balance.addExpense(newAmount);
            log.info("💳 Nouveau montant débité: {} FCFA (sans frais)", newAmount);
        }
        userBalanceRepository.save(balance);

        // 🔄 RECALCULER LES FRAIS DE LIVRAISON EN TEMPS RÉEL
        deliveryFeeService.recalculateDeliveryFeesForDate(updatedOrder.getOrderDate());

        sendOrderConfirmationNotification(user, updatedOrder);
        log.info("➕ Commande #{} complétée - Nouveau total: {} FCFA", updatedOrder.getId(),
                updatedOrder.getTotalAmount());
        return convertToDTO(updatedOrder);
    }

    /**
     * 🔍 Retrouve la ligne de commande correspondant au plat/restaurant demandé
     */
    private OrderItem findMatchingItem(Order order, OrderItemData itemData) {
        return order.getOrderItems().stream()
                .filter(item -> item.getMeal().getId().equals(itemData.getMealId())
                        && item.getRestaurant().getId().equals(itemData.getRestaurantId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 🍽️ Construit une ligne de commande à partir du panier
     */
    private OrderItem buildOrderItem(OrderItemData itemData) {
        Meal meal = mealRepository.findById(itemData.getMealId())
                .orElseThrow(() -> new RuntimeException(
                        "Le plat '" + itemData.getMealId() + "' n'existe pas ou n'est plus disponible."));

        Restaurant restaurant = restaurantRepository.findById(itemData.getRestaurantId())
                .orElseThrow(() -> new RuntimeException("Le restaurant n'existe pas ou n'est plus disponible."));

        OrderItem orderItem = new OrderItem();
        orderItem.setMeal(meal);
        orderItem.setRestaurant(restaurant);
        orderItem.setQuantity(itemData.getQuantity());
        orderItem.setUnitPrice(itemData.getUnitPrice());
        return orderItem;
    }

    /**
     * 💰 Recalcule sous-total, subvention et montants de la commande.
     * Les frais de livraison sont remis à zéro: ils seront réinjectés par
     * {@link DeliveryFeeService#recalculateDeliveryFeesForDate(LocalDate)}.
     */
    private void applyOrderTotals(Order order, User user) {
        order.calculateSubtotal();
        double subsidy = getSubsidyForUser(user);
        order.setSubsidyAmount(subsidy);
        order.setAmountAfterSubsidy(Math.max(0, order.getSubtotal() - subsidy));
        order.setDeliveryFeeShare(0.0);
        order.setTotalAmount(order.getAmountAfterSubsidy());
    }

    @Transactional(readOnly = true)
    @Override
    public List<OrderDTO> getMyOrders(Authentication auth) {
        User user = getCurrentUser(auth);
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public OrderDTO cancelOrder(Authentication auth, Long orderId) {
        if (!isBeforeDeadline()) {
            throw new RuntimeException(
                    "Il est trop tard pour annuler votre commande. Les annulations sont acceptées jusqu'à "
                            + MODIFICATION_DEADLINE_HOUR + "h.");
        }

        User user = getCurrentUser(auth);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Cette commande n'existe pas ou a été supprimée."));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Vous n'êtes pas autorisé à annuler cette commande.");
        }

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new RuntimeException("Seules les commandes confirmées peuvent être annulées.");
        }

        LocalDate today = LocalDate.now();
        if (!order.getOrderDate().equals(today)) {
            throw new RuntimeException("Vous ne pouvez annuler que la commande du jour.");
        }

        // Rembourser le MONTANT TOTAL (commande + frais de livraison) AVANT de changer le statut
        double totalToRefund = order.getTotalAmount();

        if (totalToRefund > 0) {
            UserBalance balance = userBalanceRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException(
                            "Votre solde n'a pas pu être trouvé. Veuillez contacter le support."));

            balance.refundExpense(totalToRefund);
            userBalanceRepository.save(balance);
            log.info("↩️ Remboursement total de {} FCFA (commande + frais)", totalToRefund);
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);

        // 🔄 RECALCULER LES FRAIS DE LIVRAISON EN TEMPS RÉEL (redistribuer aux autres utilisateurs)
        deliveryFeeService.recalculateDeliveryFeesForDate(savedOrder.getOrderDate());

        log.info("✅ Commande #{} annulée avec succès", orderId);
        return convertToDTO(savedOrder);
    }

    @Transactional(readOnly = true)
    @Override
    public List<OrderDTO> getOrdersByDate(LocalDate date) {
        return orderRepository.findByOrderDate(date)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public List<OrderDTO> getOrdersByRestaurant(Long restaurantId) {
        return orderRepository.findByRestaurantId(restaurantId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ========== NOUVELLES MÉTHODES ADMIN ==========

    /**
     * ❌ ADMIN: Annule une commande (sans contrainte horaire)
     */
    @Transactional
    @Override
    public OrderDTO adminCancelOrder(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande #" + orderId + " introuvable"));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Cette commande est déjà annulée");
        }

        // Rembourser le MONTANT TOTAL (commande + frais de livraison) AVANT de changer le statut
        double refundAmount = order.getTotalAmount();

        // Rembourser l'utilisateur
        if (refundAmount > 0) {
            UserBalance balance = userBalanceRepository.findByUserId(order.getUser().getId())
                    .orElseThrow(() -> new RuntimeException("Balance introuvable"));

            balance.refundExpense(refundAmount);
            userBalanceRepository.save(balance);
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);

        // 🔄 RECALCULER LES FRAIS DE LIVRAISON EN TEMPS RÉEL (redistribuer aux autres utilisateurs)
        deliveryFeeService.recalculateDeliveryFeesForDate(savedOrder.getOrderDate());

        log.info("🛡️ Admin a annulé la commande #{} - Montant remboursé: {} FCFA (total) - Raison: {}",
                orderId, refundAmount, reason);

        return convertToDTO(savedOrder);
    }

    /**
     * 🗑️ ADMIN: Supprime un plat d'une commande
     */
    @Transactional
    @Override
    public OrderDTO adminRemoveOrderItem(Long orderId, Long orderItemId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande #" + orderId + " introuvable"));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Impossible de modifier une commande annulée");
        }

        OrderItem itemToRemove = order.getOrderItems().stream()
                .filter(item -> item.getId().equals(orderItemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Plat #" + orderItemId + " introuvable dans cette commande"));

        // Sauvegarder l'ancien montant TOTAL (avec frais)
        double oldTotalAmount = order.getTotalAmount();

        // Supprimer l'item
        order.getOrderItems().remove(itemToRemove);
        orderItemRepository.delete(itemToRemove);

        // Recalculer les montants
        order.calculateSubtotal();
        double subsidyRemove = getSubsidyForUser(order.getUser());
        order.setSubsidyAmount(subsidyRemove);
        order.setAmountAfterSubsidy(Math.max(0, order.getSubtotal() - subsidyRemove));

        // Réinitialiser les frais
        order.setDeliveryFeeShare(0.0);
        order.setTotalAmount(order.getAmountAfterSubsidy());

        Order savedOrder = orderRepository.save(order);

        // Rembourser l'ancien TOTAL et débiter le nouveau montant de commande (sans frais)
        UserBalance balance = userBalanceRepository.findByUserId(order.getUser().getId())
                .orElseThrow(() -> new RuntimeException("Balance introuvable"));

        if (oldTotalAmount > 0) {
            balance.refundExpense(oldTotalAmount);
        }

        double newAmount = savedOrder.getAmountAfterSubsidy();
        if (newAmount > 0) {
            balance.addExpense(newAmount);
        }

        userBalanceRepository.save(balance);

        // 🔄 RECALCULER LES FRAIS DE LIVRAISON EN TEMPS RÉEL
        // Ceci va ajouter les nouveaux frais dans le UserBalance
        deliveryFeeService.recalculateDeliveryFeesForDate(savedOrder.getOrderDate());

        log.info("🛡️ Admin a supprimé le plat #{} de la commande #{} - Ancien total: {} FCFA - Raison: {}",
                orderItemId, orderId, oldTotalAmount, reason);

        return convertToDTO(savedOrder);
    }

    /**
     * ✏️ ADMIN: Modifie la quantité d'un plat
     */
    @Transactional
    @Override
    public OrderDTO adminUpdateOrderItemQuantity(Long orderId, Long orderItemId, Integer newQuantity, String reason) {
        if (newQuantity <= 0) {
            throw new RuntimeException("La quantité doit être supérieure à 0");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande #" + orderId + " introuvable"));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Impossible de modifier une commande annulée");
        }

        OrderItem item = order.getOrderItems().stream()
                .filter(i -> i.getId().equals(orderItemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Plat #" + orderItemId + " introuvable"));

        // Sauvegarder l'ancien montant TOTAL (avec frais)
        double oldTotalAmount = order.getTotalAmount();

        // Modifier la quantité
        item.setQuantity(newQuantity);
        item.calculateSubtotal();

        // Recalculer les montants
        order.calculateSubtotal();
        double subsidyUpdate = getSubsidyForUser(order.getUser());
        order.setSubsidyAmount(subsidyUpdate);
        order.setAmountAfterSubsidy(Math.max(0, order.getSubtotal() - subsidyUpdate));

        // Réinitialiser les frais
        order.setDeliveryFeeShare(0.0);
        order.setTotalAmount(order.getAmountAfterSubsidy());

        Order savedOrder = orderRepository.save(order);

        // Rembourser l'ancien TOTAL et débiter le nouveau montant de commande (sans frais)
        UserBalance balance = userBalanceRepository.findByUserId(order.getUser().getId())
                .orElseThrow(() -> new RuntimeException("Balance introuvable"));

        if (oldTotalAmount > 0) {
            balance.refundExpense(oldTotalAmount);
        }

        double newAmount = savedOrder.getAmountAfterSubsidy();
        if (newAmount > 0) {
            balance.addExpense(newAmount);
        }

        userBalanceRepository.save(balance);

        // 🔄 RECALCULER LES FRAIS DE LIVRAISON EN TEMPS RÉEL
        // Ceci va ajouter les nouveaux frais dans le UserBalance
        deliveryFeeService.recalculateDeliveryFeesForDate(savedOrder.getOrderDate());

        log.info("🛡️ Admin a modifié la quantité du plat #{} (commande #{}) → {} unités - Ancien total: {} FCFA - Raison: {}",
                orderItemId, orderId, newQuantity, oldTotalAmount, reason);

        return convertToDTO(savedOrder);
    }

    /**
     * 🔄 ADMIN: Réactive une commande annulée
     */
    @Transactional
    @Override
    public OrderDTO adminReactivateOrder(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande #" + orderId + " introuvable"));

        if (order.getStatus() != OrderStatus.CANCELLED) {
            throw new RuntimeException("Seules les commandes annulées peuvent être réactivées");
        }

        double chargeAmount = order.getAmountAfterSubsidy();

        order.setStatus(OrderStatus.CONFIRMED);
        Order savedOrder = orderRepository.save(order);

        // Débiter à nouveau l'utilisateur
        if (chargeAmount > 0) {
            UserBalance balance = userBalanceRepository.findByUserId(order.getUser().getId())
                    .orElseThrow(() -> new RuntimeException("Balance introuvable"));

            balance.addExpense(chargeAmount);
            userBalanceRepository.save(balance);
        }

        // 🔄 RECALCULER LES FRAIS DE LIVRAISON EN TEMPS RÉEL
        deliveryFeeService.recalculateDeliveryFeesForDate(savedOrder.getOrderDate());

        log.info("🛡️ Admin a réactivé la commande #{} - Montant débité: {} FCFA - Raison: {}",
                orderId, chargeAmount, reason);

        return convertToDTO(savedOrder);
    }

    /**
     * ➕ ADMIN: Crée ou remplace une commande pour un utilisateur (sans contrainte horaire)
     */
    @Transactional
    @Override
    public OrderDTO adminCreateOrder(AdminCreateOrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("Le panier est vide. Ajoutez des plats avant de commander.");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Utilisateur #" + request.getUserId() + " introuvable"));

        LocalDate orderDate = request.getDate() != null ? request.getDate() : LocalDate.now();

        // L'admin édite explicitement la commande: sémantique de remplacement
        OrderCreationPayload payload = new OrderCreationPayload(request.getItems(), OrderMode.REPLACE);

        return orderRepository.findByUserAndOrderDate(user, orderDate)
                .map(existingOrder -> {
                    log.info("🛡️ Admin: remplacement de la commande #{} de {} pour le {}", existingOrder.getId(), user.getUsername(), orderDate);
                    // Rembourser si confirmée
                    if (existingOrder.getStatus() == OrderStatus.CONFIRMED) {
                        double oldTotal = existingOrder.getTotalAmount();
                        if (oldTotal > 0) {
                            UserBalance balance = userBalanceRepository.findByUserId(user.getId())
                                    .orElseThrow(() -> new RuntimeException("Balance introuvable"));
                            balance.refundExpense(oldTotal);
                            userBalanceRepository.save(balance);
                        }
                    }
                    // Remplacer les items
                    existingOrder.getOrderItems().clear();
                    for (OrderItemData itemData : payload.getItems()) {
                        existingOrder.addOrderItem(buildOrderItem(itemData));
                    }
                    applyOrderTotals(existingOrder, user);
                    existingOrder.setStatus(OrderStatus.CONFIRMED);
                    Order saved = orderRepository.save(existingOrder);
                    if (saved.getAmountAfterSubsidy() > 0) {
                        UserBalance balance = userBalanceRepository.findByUserId(user.getId())
                                .orElseThrow(() -> new RuntimeException("Balance introuvable"));
                        balance.addExpense(saved.getAmountAfterSubsidy());
                        userBalanceRepository.save(balance);
                    }
                    deliveryFeeService.recalculateDeliveryFeesForDate(saved.getOrderDate());
                    log.info("✅ Admin: commande #{} mise à jour pour {} - Total: {} FCFA", saved.getId(), user.getUsername(), saved.getTotalAmount());
                    return convertToDTO(saved);
                })
                .orElseGet(() -> {
                    log.info("🛡️ Admin: création d'une nouvelle commande pour {} le {}", user.getUsername(), orderDate);
                    return createNewOrder(user, orderDate, payload);
                });
    }

    // ========== UTILITAIRES ==========

    private User getCurrentUser(Authentication auth) {
        String keycloakId = ((org.springframework.security.oauth2.jwt.Jwt) auth.getPrincipal()).getSubject();
        return userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("Votre session a expiré. Veuillez vous reconnecter."));
    }

    private OrderDTO convertToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setUserId(order.getUser().getId());
        dto.setUsername(order.getUser().getUsername());
        dto.setOrderDate(order.getOrderDate());
        dto.setSubtotal(order.getSubtotal());
        dto.setSubsidyAmount(order.getSubsidyAmount());
        dto.setAmountAfterSubsidy(order.getAmountAfterSubsidy());
        dto.setDeliveryFeeShare(order.getDeliveryFeeShare());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus().name());
        dto.setCreatedAt(order.getCreatedAt());

        List<String> restaurantNames = order.getOrderItems().stream()
                .map(item -> item.getRestaurant().getRestaurantName())
                .distinct()
                .collect(Collectors.toList());

        dto.setRestaurantName(String.join(", ", restaurantNames));

        dto.setItems(order.getOrderItems().stream()
                .map(item -> {
                    OrderItemDTO itemDTO = new OrderItemDTO();
                    itemDTO.setId(item.getId());
                    itemDTO.setMealId(item.getMeal().getId());
                    itemDTO.setMealName(item.getMeal().getMealName());
                    itemDTO.setRestaurantId(item.getRestaurant().getId());
                    itemDTO.setRestaurantName(item.getRestaurant().getRestaurantName());
                    itemDTO.setQuantity(item.getQuantity());
                    itemDTO.setUnitPrice(item.getUnitPrice());
                    itemDTO.setSubtotal(item.getSubtotal());
                    return itemDTO;
                })
                .collect(Collectors.toList()));

        return dto;
    }
}