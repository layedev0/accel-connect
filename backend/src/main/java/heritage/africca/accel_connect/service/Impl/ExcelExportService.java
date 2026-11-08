package heritage.africca.accel_connect.service.Impl;

import heritage.africca.accel_connect.dto.WeeklyExportDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 📊 SERVICE D'EXPORT EXCEL ULTRA-PROFESSIONNEL
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ExcelExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * 📥 Génère un fichier Excel professionnel pour les commandes hebdomadaires
     */
    public byte[] generateWeeklyReport(WeeklyExportDTO data) throws IOException {
        Workbook workbook = new XSSFWorkbook();

        // Créer les styles
        Map<String, CellStyle> styles = createEnhancedStyles(workbook);

        // 📋 Onglet 1: Récapitulatif Général
        createEnhancedSummarySheet(workbook, data, styles);

        // 👥 Onglet 2: Détails par Utilisateur (AMÉLIORÉ)
        createEnhancedUserDetailsSheet(workbook, data, styles);

        // 📊 Onglet 3: Statistiques par Restaurant (AMÉLIORÉ)
        createEnhancedRestaurantStatsSheet(workbook, data, styles);

        // 🍽️ Onglet 4: Détails par Plat (NOUVEAU)
        createMealDetailsSheet(workbook, data, styles);

        // 💰 Onglet 5: Gestion des Dettes (NOUVEAU)
        createDebtManagementSheet(workbook, data, styles);

        // Écrire dans un ByteArrayOutputStream
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        log.info("✅ Export Excel PREMIUM généré - {} utilisateurs, {} onglets",
                data.getUserConsumptions().size(), workbook.getNumberOfSheets());

        return outputStream.toByteArray();
    }

    /**
     * 🎨 Crée des styles améliorés pour le document Excel
     */
    private Map<String, CellStyle> createEnhancedStyles(Workbook workbook) {
        Map<String, CellStyle> styles = new HashMap<>();

        // === FONTS ===
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 18);
        titleFont.setColor(IndexedColors.WHITE.getIndex());

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 11);
        headerFont.setColor(IndexedColors.WHITE.getIndex());

        Font subtotalFont = workbook.createFont();
        subtotalFont.setBold(true);
        subtotalFont.setFontHeightInPoints((short) 11);

        Font boldFont = workbook.createFont();
        boldFont.setBold(true);

        // === STYLE TITRE PRINCIPAL ===
        CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFont(titleFont);
        titleStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(titleStyle, BorderStyle.MEDIUM);
        styles.put("title", titleStyle);

        // === STYLE EN-TÊTE ===
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(headerStyle, BorderStyle.THIN);
        headerStyle.setWrapText(true);
        styles.put("header", headerStyle);

        // === STYLE CELLULE NORMALE ===
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setAlignment(HorizontalAlignment.LEFT);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(cellStyle, BorderStyle.THIN);
        styles.put("cell", cellStyle);

        // === STYLE CELLULE CENTRÉE ===
        CellStyle cellCenterStyle = workbook.createCellStyle();
        cellCenterStyle.cloneStyleFrom(cellStyle);
        cellCenterStyle.setAlignment(HorizontalAlignment.CENTER);
        styles.put("cellCenter", cellCenterStyle);

        // === STYLE MONTANT ===
        DataFormat format = workbook.createDataFormat();
        CellStyle amountStyle = workbook.createCellStyle();
        amountStyle.cloneStyleFrom(cellStyle);
        amountStyle.setAlignment(HorizontalAlignment.RIGHT);
        amountStyle.setDataFormat(format.getFormat("#,##0 \"FCFA\""));
        styles.put("amount", amountStyle);

        // === STYLE SOUS-TOTAL ===
        CellStyle subtotalStyle = workbook.createCellStyle();
        subtotalStyle.setFont(subtotalFont);
        subtotalStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        subtotalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        subtotalStyle.setAlignment(HorizontalAlignment.RIGHT);
        subtotalStyle.setDataFormat(format.getFormat("#,##0 \"FCFA\""));
        setBorders(subtotalStyle, BorderStyle.THIN);
        styles.put("subtotal", subtotalStyle);

        // === STYLE TOTAL ===
        CellStyle totalStyle = workbook.createCellStyle();
        totalStyle.setFont(titleFont);
        totalStyle.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
        totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        totalStyle.setAlignment(HorizontalAlignment.RIGHT);
        totalStyle.setDataFormat(format.getFormat("#,##0 \"FCFA\""));
        setBorders(totalStyle, BorderStyle.THICK);
        styles.put("total", totalStyle);

        // === STYLE ALERTE (DETTE) ===
        CellStyle alertStyle = workbook.createCellStyle();
        alertStyle.cloneStyleFrom(amountStyle);
        alertStyle.setFillForegroundColor(IndexedColors.CORAL.getIndex());
        alertStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font alertFont = workbook.createFont();
        alertFont.setBold(true);
        alertFont.setColor(IndexedColors.DARK_RED.getIndex());
        alertStyle.setFont(alertFont);
        styles.put("alert", alertStyle);

        // === STYLE POSITIF (CRÉDIT) ===
        CellStyle positiveStyle = workbook.createCellStyle();
        positiveStyle.cloneStyleFrom(amountStyle);
        positiveStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        positiveStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put("positive", positiveStyle);

        // === STYLE GRAS ===
        CellStyle boldStyle = workbook.createCellStyle();
        boldStyle.cloneStyleFrom(cellStyle);
        boldStyle.setFont(boldFont);
        styles.put("bold", boldStyle);

        // === STYLE LIGNE ALTERNÉE ===
        CellStyle alternateRowStyle = workbook.createCellStyle();
        alternateRowStyle.cloneStyleFrom(cellStyle);
        alternateRowStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        alternateRowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put("alternateRow", alternateRowStyle);

        return styles;
    }

    /**
     * 🔲 Helper pour définir les bordures
     */
    private void setBorders(CellStyle style, BorderStyle borderStyle) {
        style.setBorderBottom(borderStyle);
        style.setBorderTop(borderStyle);
        style.setBorderLeft(borderStyle);
        style.setBorderRight(borderStyle);
    }

    /**
     * 📋 Crée l'onglet récapitulatif général AMÉLIORÉ
     */
    private void createEnhancedSummarySheet(Workbook workbook, WeeklyExportDTO data, Map<String, CellStyle> styles) {
        Sheet sheet = workbook.createSheet("📊 Récapitulatif");
        int rowNum = 0;

        // === TITRE PRINCIPAL ===
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("🍽️ RAPPORT HEBDOMADAIRE - ACCEL CONNECT");
        titleCell.setCellStyle(styles.get("title"));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));
        titleRow.setHeightInPoints(35);

        rowNum++;

        // === INFORMATIONS PÉRIODE ===
        createInfoSection(sheet, rowNum, styles,
                "📅 Période", data.getStartDate() + " au " + data.getEndDate());
        rowNum += 2;

        createInfoSection(sheet, rowNum, styles,
                "📥 Date d'export", data.getExportDate());
        rowNum += 3;

        // === STATISTIQUES GLOBALES ===
        WeeklyExportDTO.WeeklyGlobalStats stats = data.getGlobalStats();

        // Titre section
        Row sectionTitleRow = sheet.createRow(rowNum++);
        Cell sectionTitleCell = sectionTitleRow.createCell(0);
        sectionTitleCell.setCellValue("📊 STATISTIQUES GLOBALES");
        sectionTitleCell.setCellStyle(styles.get("header"));
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 7));

        rowNum++;

        // Données statistiques
        Object[][] statsData = {
                {"👥 Utilisateurs actifs", stats.getTotalUsers(), "personnes"},
                {"📦 Commandes totales", stats.getTotalOrders(), "commandes"},
                {"🏪 Restaurants partenaires", stats.getTotalRestaurants(), "restaurants"},
                {"💰 Montant AVEC subvention", stats.getTotalAmountWithSubsidy(), "FCFA"},
                {"💵 Montant SANS subvention", stats.getTotalAmountWithoutSubsidy(), "FCFA"},
                {"🎁 Subvention totale", stats.getTotalSubsidy(), "FCFA"},
                {"🚚 Frais de livraison", stats.getTotalDeliveryFees(), "FCFA"}
        };

        for (Object[] stat : statsData) {
            Row row = sheet.createRow(rowNum++);

            // Label
            Cell labelCell = row.createCell(0);
            labelCell.setCellValue((String) stat[0]);
            labelCell.setCellStyle(styles.get("bold"));
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 4));

            // Valeur
            Cell valueCell = row.createCell(5);
            if (stat[1] instanceof Double) {
                valueCell.setCellValue((Double) stat[1]);
                valueCell.setCellStyle(styles.get("total"));
            } else {
                valueCell.setCellValue((Integer) stat[1]);
                valueCell.setCellStyle(styles.get("cellCenter"));
            }
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 5, 6));

            // Unité
            Cell unitCell = row.createCell(7);
            unitCell.setCellValue((String) stat[2]);
            unitCell.setCellStyle(styles.get("cell"));
        }

        // Ajuster largeurs
        for (int i = 0; i < 8; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 2000);
        }
    }

    /**
     * 📝 Helper pour créer une section d'information
     */
    private void createInfoSection(Sheet sheet, int rowNum, Map<String, CellStyle> styles,
                                   String label, String value) {
        Row row = sheet.createRow(rowNum);

        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(styles.get("header"));
        sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, 2));

        Cell valueCell = row.createCell(3);
        valueCell.setCellValue(value);
        valueCell.setCellStyle(styles.get("bold"));
        sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 3, 7));
    }

    /**
     * 👥 Crée l'onglet détails utilisateurs AMÉLIORÉ
     */
    private void createEnhancedUserDetailsSheet(Workbook workbook, WeeklyExportDTO data, Map<String, CellStyle> styles) {
        Sheet sheet = workbook.createSheet("👥 Consommation Utilisateurs");
        int rowNum = 0;

        // Titre
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("👥 DÉTAIL PAR UTILISATEUR");
        titleCell.setCellStyle(styles.get("title"));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 12));
        titleRow.setHeightInPoints(30);

        rowNum += 2;

        // En-têtes
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {
                "N°", "Nom", "Email", "Lundi", "Mardi", "Mercredi",
                "Jeudi", "Vendredi", "Total", "Subv.", "Livraison", "Solde", "Statut"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(styles.get("header"));
        }

        headerRow.setHeightInPoints(25);

        // Données utilisateurs avec lignes alternées
        int userIndex = 1;
        for (WeeklyExportDTO.UserWeeklyConsumption user : data.getUserConsumptions()) {
            Row row = sheet.createRow(rowNum++);
            boolean isAlternate = (userIndex % 2 == 0);

            CellStyle rowStyle = isAlternate ? styles.get("alternateRow") : styles.get("cell");

            // N°
            createCell(row, 0, userIndex++, styles.get("cellCenter"));

            // Nom
            String firstName = user.getFirstName() != null ? user.getFirstName() : "";
            String lastName  = user.getLastName()  != null ? user.getLastName()  : "";

            String fullName = (firstName + " " + lastName).trim();
            createCell(row, 1, fullName, rowStyle);

            // Email
            createCell(row, 2, user.getEmail(), rowStyle);

            // Consommation par jour
            String[] days = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"};
            for (int i = 0; i < days.length; i++) {
                WeeklyExportDTO.DayConsumption dayCons = user.getDailyConsumptions().get(days[i]);

                if (dayCons != null && dayCons.isHasOrder()) {
                    createCell(row, 3 + i, dayCons.getDailyTotal(), styles.get("amount"));
                } else {
                    createCell(row, 3 + i, "-", styles.get("cellCenter"));
                }
            }

            // Total
            createCell(row, 8, user.getTotalAmountWithSubsidy(), styles.get("subtotal"));

            // Subvention
            createCell(row, 9, user.getTotalSubsidy(), styles.get("amount"));

            // Livraison
            createCell(row, 10, user.getTotalDeliveryFees(), styles.get("amount"));

            // Solde
            CellStyle balanceStyle = user.isHasDebt() ? styles.get("alert") :
                    user.getBalance() > 0 ? styles.get("positive") : styles.get("amount");
            createCell(row, 11, user.getBalance(), balanceStyle);

            // Statut
            String status = user.isHasDebt() ? "⚠️ DETTE" :
                    user.getBalance() > 0 ? "✅ CRÉDIT" : "✓ OK";
            createCell(row, 12, status, styles.get("cellCenter"));
        }

        // Ajuster largeurs
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
        }

        // Figer les en-têtes
        sheet.createFreezePane(0, 4);
    }

    /**
     * 📊 Crée l'onglet stats restaurants AMÉLIORÉ
     */
    private void createEnhancedRestaurantStatsSheet(Workbook workbook, WeeklyExportDTO data, Map<String, CellStyle> styles) {
        Sheet sheet = workbook.createSheet("🏪 Restaurants");
        int rowNum = 0;

        // Titre
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("🏪 STATISTIQUES PAR RESTAURANT");
        titleCell.setCellStyle(styles.get("title"));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));
        titleRow.setHeightInPoints(30);

        rowNum += 2;

        // Regrouper par restaurant
        Map<String, RestaurantData> restaurantStats = new LinkedHashMap<>();

        for (WeeklyExportDTO.UserWeeklyConsumption user : data.getUserConsumptions()) {
            for (WeeklyExportDTO.DayConsumption day : user.getDailyConsumptions().values()) {
                if (day != null && day.isHasOrder()) {
                    for (WeeklyExportDTO.MealDetail meal : day.getMeals()) {
                        RestaurantData rd = restaurantStats.computeIfAbsent(
                                meal.getRestaurantName(),
                                k -> new RestaurantData()
                        );
                        rd.orderCount++;
                        rd.totalAmount += meal.getSubtotal();
                        rd.mealCount += meal.getQuantity();
                        rd.mealDetails.computeIfAbsent(meal.getMealName(), k -> 0);
                        rd.mealDetails.put(meal.getMealName(),
                                rd.mealDetails.get(meal.getMealName()) + meal.getQuantity());
                    }
                }
            }
        }

        // Trier par montant décroissant
        List<Map.Entry<String, RestaurantData>> sortedRestaurants = new ArrayList<>(restaurantStats.entrySet());
        sortedRestaurants.sort((a, b) -> Double.compare(b.getValue().totalAmount, a.getValue().totalAmount));

        // En-têtes
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"N°", "Restaurant", "Commandes", "Plats", "Montant", "Moy/Cmd", "Top Plat"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(styles.get("header"));
        }

        // Données
        int restaurantIndex = 1;
        for (Map.Entry<String, RestaurantData> entry : sortedRestaurants) {
            Row row = sheet.createRow(rowNum++);
            RestaurantData rd = entry.getValue();

            createCell(row, 0, restaurantIndex++, styles.get("cellCenter"));
            createCell(row, 1, entry.getKey(), styles.get("bold"));
            createCell(row, 2, rd.orderCount, styles.get("cellCenter"));
            createCell(row, 3, rd.mealCount, styles.get("cellCenter"));
            createCell(row, 4, rd.totalAmount, styles.get("subtotal"));
            createCell(row, 5, rd.totalAmount / rd.orderCount, styles.get("amount"));

            // Top plat
            String topMeal = rd.mealDetails.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("-");
            createCell(row, 6, topMeal, styles.get("cell"));
        }

        // Ajuster largeurs
        for (int i = 0; i < 7; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
        }
    }

    /**
     * 🍽️ NOUVEAU: Crée l'onglet détails par plat
     */
    private void createMealDetailsSheet(Workbook workbook, WeeklyExportDTO data, Map<String, CellStyle> styles) {
        Sheet sheet = workbook.createSheet("🍽️ Détails Plats");
        int rowNum = 0;

        // Titre
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("🍽️ DÉTAILS PAR PLAT");
        titleCell.setCellStyle(styles.get("title"));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
        titleRow.setHeightInPoints(30);

        rowNum += 2;

        // Collecter tous les plats
        Map<String, MealStats> mealStats = new HashMap<>();

        for (WeeklyExportDTO.UserWeeklyConsumption user : data.getUserConsumptions()) {
            for (WeeklyExportDTO.DayConsumption day : user.getDailyConsumptions().values()) {
                if (day != null && day.isHasOrder()) {
                    for (WeeklyExportDTO.MealDetail meal : day.getMeals()) {
                        String key = meal.getRestaurantName() + " - " + meal.getMealName();
                        MealStats ms = mealStats.computeIfAbsent(key, k -> new MealStats());
                        ms.quantity += meal.getQuantity();
                        ms.totalAmount += meal.getSubtotal();
                        ms.restaurant = meal.getRestaurantName();
                        ms.mealName = meal.getMealName();
                        ms.unitPrice = meal.getUnitPrice();
                    }
                }
            }
        }

        // Trier par quantité décroissante
        List<MealStats> sortedMeals = new ArrayList<>(mealStats.values());
        sortedMeals.sort((a, b) -> Integer.compare(b.quantity, a.quantity));

        // En-têtes
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"N°", "Restaurant", "Plat", "Prix Unit.", "Quantité", "Total"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(styles.get("header"));
        }

        // Données
        int mealIndex = 1;
        for (MealStats ms : sortedMeals) {
            Row row = sheet.createRow(rowNum++);

            createCell(row, 0, mealIndex++, styles.get("cellCenter"));
            createCell(row, 1, ms.restaurant, styles.get("cell"));
            createCell(row, 2, ms.mealName, styles.get("bold"));
            createCell(row, 3, ms.unitPrice, styles.get("amount"));
            createCell(row, 4, ms.quantity, styles.get("cellCenter"));
            createCell(row, 5, ms.totalAmount, styles.get("subtotal"));
        }

        // Ajuster largeurs
        for (int i = 0; i < 6; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
        }
    }

    /**
     * 💰 NOUVEAU: Crée l'onglet gestion des dettes
     */
    private void createDebtManagementSheet(Workbook workbook, WeeklyExportDTO data, Map<String, CellStyle> styles) {
        Sheet sheet = workbook.createSheet("💰 Gestion Dettes");
        int rowNum = 0;

        // Titre
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("💰 GESTION DES DETTES ET CRÉDITS");
        titleCell.setCellStyle(styles.get("title"));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
        titleRow.setHeightInPoints(30);

        rowNum += 2;

        // Filtrer utilisateurs avec dette ou crédit
        List<WeeklyExportDTO.UserWeeklyConsumption> usersWithBalance = data.getUserConsumptions().stream()
                .filter(u -> u.getBalance() != 0)
                .sorted((a, b) -> Double.compare(a.getBalance(), b.getBalance()))
                .toList();

        if (usersWithBalance.isEmpty()) {
            Row noDataRow = sheet.createRow(rowNum);
            Cell noDataCell = noDataRow.createCell(0);
            noDataCell.setCellValue("✅ Aucune dette ou crédit à gérer");
            noDataCell.setCellStyle(styles.get("bold"));
            sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, 5));
            return;
        }

        // En-têtes
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"N°", "Utilisateur", "Email", "Solde", "Type", "Action Requise"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(styles.get("header"));
        }

        // Données
        int userIndex = 1;
        for (WeeklyExportDTO.UserWeeklyConsumption user : usersWithBalance) {
            Row row = sheet.createRow(rowNum++);

            createCell(row, 0, userIndex++, styles.get("cellCenter"));
            createCell(row, 1, user.getUsername(), styles.get("bold"));
            createCell(row, 2, user.getEmail(), styles.get("cell"));

            CellStyle balanceStyle = user.isHasDebt() ? styles.get("alert") : styles.get("positive");
            createCell(row, 3, user.getBalance(), balanceStyle);

            String type = user.isHasDebt() ? "⚠️ DETTE" : "✅ CRÉDIT";
            createCell(row, 4, type, styles.get("cellCenter"));

            String action = user.isHasDebt() ?
                    "Contacter pour régularisation" :
                    "Disponible pour remboursement";
            createCell(row, 5, action, styles.get("cell"));
        }

        // Ajuster largeurs
        for (int i = 0; i < 6; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1500);
        }
    }

    /**
     * 📝 Helper pour créer une cellule
     */
    private void createCell(Row row, int column, Object value, CellStyle style) {
        Cell cell = row.createCell(column);

        if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        } else if (value instanceof Double) {
            cell.setCellValue((Double) value);
        }

        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    // === CLASSES HELPER ===

    private static class RestaurantData {
        int orderCount = 0;
        int mealCount = 0;
        double totalAmount = 0.0;
        Map<String, Integer> mealDetails = new HashMap<>();
    }

    private static class MealStats {
        String restaurant;
        String mealName;
        double unitPrice;
        int quantity;
        double totalAmount;
    }
}