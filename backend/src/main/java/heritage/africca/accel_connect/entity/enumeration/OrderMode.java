package heritage.africca.accel_connect.entity.enumeration;

/**
 * 🛒 MODE DE PRISE DE COMMANDE
 *
 * - ADD : les articles envoyés s'AJOUTENT à la commande confirmée du jour (mode par défaut)
 * - REPLACE : les articles envoyés REMPLACENT ceux de la commande du jour
 * (utilisé par le bouton "Modifier" de Mes commandes)
 */
public enum OrderMode {
    ADD,
    REPLACE
}
