package heritage.africca.accel_connect.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsumptionReportService {

    private final JdbcTemplate jdbcTemplate;

    public byte[] generateConsumptionReport(String periodType, LocalDate startDate, LocalDate endDate) throws IOException {
        log.info("Generating consumption report - type: {}, start: {}, end: {}", periodType, startDate, endDate);

        try (Workbook workbook = new XSSFWorkbook()) {
            // Styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);

            // Générer les feuilles pour chaque restaurant
            generateRestaurantSheet(workbook, "JOLOF", startDate, endDate, headerStyle, dataStyle, numberStyle);
            generateRestaurantSheet(workbook, "EVA", startDate, endDate, headerStyle, dataStyle, numberStyle);
            generateRestaurantSheet(workbook, "RODRA", startDate, endDate, headerStyle, dataStyle, numberStyle);
            generateRestaurantSheet(workbook, "NOWELLI", startDate, endDate, headerStyle, dataStyle, numberStyle);

            // Générer la feuille récapitulative quotidienne
            generateDailyRecapSheet(workbook, startDate, endDate, headerStyle, dataStyle, numberStyle);

            // Générer la feuille récapitulative hebdomadaire
            generateWeeklyRecapSheet(workbook, startDate, endDate, headerStyle, dataStyle, numberStyle);

            // Générer la feuille récapitulative hebdomadaire par personne
            generateWeeklyPersonReportSheet(workbook, startDate, endDate, headerStyle, dataStyle, numberStyle);

            // Écrire dans un ByteArrayOutputStream
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            log.info("Consumption report generated successfully");
            return out.toByteArray();
        }
    }

    private void generateRestaurantSheet(Workbook workbook, String restaurant, LocalDate startDate, LocalDate endDate,
                                        CellStyle headerStyle, CellStyle dataStyle, CellStyle numberStyle) {
        String query = String.format(
            "SELECT order_date, first_name, last_name, meal_name, subtotal, subsidy_amount, " +
            "amount_after_subsidy, delivery_fee_share, total_amount " +
            "FROM view_%s WHERE order_date BETWEEN ? AND ? ORDER BY order_date DESC, last_name",
            restaurant.toLowerCase()
        );

        List<Map<String, Object>> data = jdbcTemplate.queryForList(query, startDate, endDate);

        if (data.isEmpty()) {
            log.info("No data for restaurant: {}", restaurant);
            return;
        }

        Sheet sheet = workbook.createSheet(restaurant);

        // En-têtes
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Date", "Prénom", "Nom", "Plat", "Sous-total", "Subvention",
                           "Montant après sub.", "Frais livraison", "Total à payer"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Données
        int rowNum = 1;
        for (Map<String, Object> row : data) {
            Row dataRow = sheet.createRow(rowNum++);

            // Date
            Cell cell0 = dataRow.createCell(0);
            cell0.setCellValue(row.get("order_date").toString());
            cell0.setCellStyle(dataStyle);

            // Prénom
            Cell cell1 = dataRow.createCell(1);
            cell1.setCellValue((String) row.get("first_name"));
            cell1.setCellStyle(dataStyle);

            // Nom
            Cell cell2 = dataRow.createCell(2);
            cell2.setCellValue((String) row.get("last_name"));
            cell2.setCellStyle(dataStyle);

            // Plat
            Cell cell3 = dataRow.createCell(3);
            cell3.setCellValue((String) row.get("meal_name"));
            cell3.setCellStyle(dataStyle);

            // Montants
            createNumberCell(dataRow, 4, row.get("subtotal"), numberStyle);
            createNumberCell(dataRow, 5, row.get("subsidy_amount"), numberStyle);
            createNumberCell(dataRow, 6, row.get("amount_after_subsidy"), numberStyle);
            createNumberCell(dataRow, 7, row.get("delivery_fee_share"), numberStyle);
            createNumberCell(dataRow, 8, row.get("total_amount"), numberStyle);
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            // Ajouter un peu de marge
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 512);
        }

        // Freeze first row
        sheet.createFreezePane(0, 1);
    }

    private void generateDailyRecapSheet(Workbook workbook, LocalDate startDate, LocalDate endDate,
                                        CellStyle headerStyle, CellStyle dataStyle, CellStyle numberStyle) {
        String query = "SELECT order_date, restaurant_name, nbre_commandes, nbre_commandes_subventionnees, " +
                      "sous_total, total_subvention, total_a_payer " +
                      "FROM view_recap_daily WHERE order_date BETWEEN ? AND ? ORDER BY order_date DESC, restaurant_name";

        List<Map<String, Object>> data = jdbcTemplate.queryForList(query, startDate, endDate);

        if (data.isEmpty()) {
            return;
        }

        Sheet sheet = workbook.createSheet("Recap Quotidien");

        // En-têtes
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Date", "Restaurant", "Nb Commandes", "Nb Subventionnées",
                           "Sous-total", "Total Subvention", "Total à payer"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Données
        int rowNum = 1;
        for (Map<String, Object> row : data) {
            Row dataRow = sheet.createRow(rowNum++);

            Cell cell0 = dataRow.createCell(0);
            cell0.setCellValue(row.get("order_date").toString());
            cell0.setCellStyle(dataStyle);

            Cell cell1 = dataRow.createCell(1);
            cell1.setCellValue((String) row.get("restaurant_name"));
            cell1.setCellStyle(dataStyle);

            createNumberCell(dataRow, 2, row.get("nbre_commandes"), numberStyle);
            createNumberCell(dataRow, 3, row.get("nbre_commandes_subventionnees"), numberStyle);
            createNumberCell(dataRow, 4, row.get("sous_total"), numberStyle);
            createNumberCell(dataRow, 5, row.get("total_subvention"), numberStyle);
            createNumberCell(dataRow, 6, row.get("total_a_payer"), numberStyle);
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 512);
        }

        sheet.createFreezePane(0, 1);
    }

    private void generateWeeklyRecapSheet(Workbook workbook, LocalDate startDate, LocalDate endDate,
                                         CellStyle headerStyle, CellStyle dataStyle, CellStyle numberStyle) {
        String query = "SELECT periode, restaurant_name, nbre_commandes, nbre_commandes_subventionnees, " +
                      "sous_total, total_subvention, total_a_payer, total_livraison_semaine " +
                      "FROM view_recap_weekly ORDER BY periode DESC, restaurant_name";

        List<Map<String, Object>> data = jdbcTemplate.queryForList(query);

        // Filtrer par dates si nécessaire
        if (startDate != null && endDate != null) {
            data = data.stream()
                .filter(row -> {
                    String periode = (String) row.get("periode");
                    String dateDebut = periode.split(" - ")[0];
                    try {
                        LocalDate weekStart = LocalDate.parse(dateDebut);
                        return !weekStart.isBefore(startDate) && !weekStart.isAfter(endDate);
                    } catch (Exception e) {
                        return true;
                    }
                })
                .toList();
        }

        if (data.isEmpty()) {
            return;
        }

        Sheet sheet = workbook.createSheet("Recap Hebdomadaire");

        // En-têtes
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Période", "Restaurant", "Nb Commandes", "Nb Subventionnées",
                           "Sous-total", "Total Subvention", "Total à payer", "Frais Livraison"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Données
        int rowNum = 1;
        for (Map<String, Object> row : data) {
            Row dataRow = sheet.createRow(rowNum++);

            Cell cell0 = dataRow.createCell(0);
            cell0.setCellValue((String) row.get("periode"));
            cell0.setCellStyle(dataStyle);

            Cell cell1 = dataRow.createCell(1);
            cell1.setCellValue((String) row.get("restaurant_name"));
            cell1.setCellStyle(dataStyle);

            createNumberCell(dataRow, 2, row.get("nbre_commandes"), numberStyle);
            createNumberCell(dataRow, 3, row.get("nbre_commandes_subventionnees"), numberStyle);
            createNumberCell(dataRow, 4, row.get("sous_total"), numberStyle);
            createNumberCell(dataRow, 5, row.get("total_subvention"), numberStyle);
            createNumberCell(dataRow, 6, row.get("total_a_payer"), numberStyle);
            createNumberCell(dataRow, 7, row.get("total_livraison_semaine"), numberStyle);
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 512);
        }

        sheet.createFreezePane(0, 1);
    }

    private void generateWeeklyPersonReportSheet(Workbook workbook, LocalDate startDate, LocalDate endDate,
                                                 CellStyle headerStyle, CellStyle dataStyle, CellStyle numberStyle) {

        // Requête SQL corrigée pour calculer l'état historique correct
        // AVANCE = Solde positif au début de la période (totalDeposited - dépenses avant)
        // DETTE ANTERIEURE = Dettes accumulées avant la période (solde négatif)
        String query =
            "WITH user_weekly_spending AS (" +
            "  SELECT o.user_id, " +
            "    SUM(o.amount_after_subsidy) AS orders_total, " +
            "    SUM(COALESCE(o.delivery_fee_share, 0)) AS delivery_fees_total, " +
            "    SUM(COALESCE(o.total_amount, o.amount_after_subsidy)) AS total_spent " +
            "  FROM orders o " +
            "  WHERE o.status = 'CONFIRMED' AND o.order_date BETWEEN ? AND ? " +
            "  GROUP BY o.user_id" +
            "), " +
            "spending_before_period AS (" +
            "  SELECT o.user_id, " +
            "    SUM(COALESCE(o.total_amount, o.amount_after_subsidy)) AS total_spent_before " +
            "  FROM orders o " +
            "  WHERE o.status = 'CONFIRMED' AND o.order_date < ? " +
            "  GROUP BY o.user_id" +
            "), " +
            "balance_at_period_start AS (" +
            "  SELECT ub.user_id, " +
            "    COALESCE(ub.total_deposited, 0) - COALESCE(sbp.total_spent_before, 0) AS balance_start " +
            "  FROM user_balances ub " +
            "  LEFT JOIN spending_before_period sbp ON ub.user_id = sbp.user_id" +
            "), " +
            "active_users AS (" +
            "  SELECT DISTINCT u.id, u.first_name, u.last_name " +
            "  FROM users u " +
            "  WHERE u.id IN (" +
            "    SELECT user_id FROM user_weekly_spending " +
            "    UNION " +
            "    SELECT user_id FROM spending_before_period" +
            "    UNION " +
            "    SELECT user_id FROM user_balances WHERE total_deposited > 0" +
            "  )" +
            ") " +
            "SELECT u.id AS user_id, u.first_name, u.last_name, " +
            "  COALESCE(uws.orders_total, 0) AS commandes_total, " +
            "  COALESCE(uws.delivery_fees_total, 0) AS frais_livraison, " +
            "  COALESCE(uws.total_spent, 0) AS somme_due, " +
            "  CASE WHEN COALESCE(bps.balance_start, 0) > 0 " +
            "    THEN bps.balance_start " +
            "    ELSE 0 END AS avance, " +
            "  CASE WHEN COALESCE(bps.balance_start, 0) < 0 " +
            "    THEN ABS(bps.balance_start) " +
            "    ELSE 0 END AS dette_anterieure, " +
            "  CASE WHEN (COALESCE(bps.balance_start, 0) - COALESCE(uws.total_spent, 0)) < 0 " +
            "  THEN ABS(COALESCE(bps.balance_start, 0) - COALESCE(uws.total_spent, 0)) " +
            "  ELSE 0 END AS net_a_payer, " +
            "  CASE WHEN (COALESCE(bps.balance_start, 0) - COALESCE(uws.total_spent, 0)) >= 0 " +
            "  THEN COALESCE(bps.balance_start, 0) - COALESCE(uws.total_spent, 0) " +
            "  ELSE 0 END AS avoir_en_caisse " +
            "FROM active_users u " +
            "LEFT JOIN user_weekly_spending uws ON u.id = uws.user_id " +
            "LEFT JOIN balance_at_period_start bps ON u.id = bps.user_id " +
            "ORDER BY u.last_name, u.first_name";

        List<Map<String, Object>> data = jdbcTemplate.queryForList(query, startDate, endDate, startDate);

        if (data.isEmpty()) {
            log.info("No data for weekly person report");
            return;
        }

        Sheet sheet = workbook.createSheet("Recap Hebdo Personnes");

        // En-têtes avec rotation à 45° (comme sur la capture)
        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(80); // Hauteur pour le texte incliné

        String[] headers = {"NOM ET PRENOMS", "COMMANDES", "LIVRAISON", "SOMME DUE A T", "AVANCE",
                           "DETTE ANTERIEURE", "NET A PAYER", "AVOIR EN CAISSE"};

        CellStyle rotatedHeaderStyle = createRotatedHeaderStyle(workbook);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            // Première colonne (NOM) sans rotation
            if (i == 0) {
                cell.setCellStyle(headerStyle);
            } else {
                cell.setCellStyle(rotatedHeaderStyle);
            }
        }

        // Données avec formatage conditionnel
        int rowNum = 1;
        for (Map<String, Object> row : data) {
            Row dataRow = sheet.createRow(rowNum++);

            // Nom complet
            Cell cell0 = dataRow.createCell(0);
            String fullName = row.get("first_name") + " " + row.get("last_name");
            cell0.setCellValue(fullName.toUpperCase());
            cell0.setCellStyle(dataStyle);

            // Montants numériques
            createNumberCell(dataRow, 1, row.get("commandes_total"), numberStyle);  // Commandes seules
            createNumberCell(dataRow, 2, row.get("frais_livraison"), numberStyle);  // Frais de livraison
            createNumberCell(dataRow, 3, row.get("somme_due"), numberStyle);        // Total (commandes + livraison)
            createNumberCell(dataRow, 4, row.get("avance"), numberStyle);
            createNumberCell(dataRow, 5, row.get("dette_anterieure"), numberStyle);

            // NET A PAYER avec style conditionnel (orange si négatif)
            Cell netCell = dataRow.createCell(6);
            Double netValue = getDoubleValue(row.get("net_a_payer"));
            netCell.setCellValue(netValue);
            if (netValue < 0) {
                netCell.setCellStyle(createNegativeNumberStyle(workbook));
            } else {
                netCell.setCellStyle(numberStyle);
            }

            // AVOIR EN CAISSE avec style conditionnel
            Cell avoirCell = dataRow.createCell(7);
            Double avoirValue = getDoubleValue(row.get("avoir_en_caisse"));
            avoirCell.setCellValue(avoirValue);
            if (avoirValue < 0) {
                avoirCell.setCellStyle(createNegativeNumberStyle(workbook));
            } else {
                avoirCell.setCellStyle(numberStyle);
            }
        }

        // Auto-size colonnes
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 512);
        }

        // Figer les en-têtes
        sheet.createFreezePane(0, 1);
    }

    private void createNumberCell(Row row, int columnIndex, Object value, CellStyle numberStyle) {
        Cell cell = row.createCell(columnIndex);
        if (value != null) {
            if (value instanceof Number) {
                cell.setCellValue(((Number) value).doubleValue());
            } else {
                try {
                    cell.setCellValue(Double.parseDouble(value.toString()));
                } catch (NumberFormatException e) {
                    cell.setCellValue(0);
                }
            }
        } else {
            cell.setCellValue(0);
        }
        cell.setCellStyle(numberStyle);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        // Font
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        // Background
        style.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Alignment
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);

        // Borders
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        // Alignment
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // Borders
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    private CellStyle createNumberStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        // Number format
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0"));

        // Alignment
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // Borders
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    private CellStyle createRotatedHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        // Font
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.BLACK.getIndex());
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);

        // Background vert clair (comme sur la capture)
        style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Rotation à 45°
        style.setRotation((short) 45);

        // Alignment
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.BOTTOM);
        style.setWrapText(true);

        // Borders
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    private CellStyle createNegativeNumberStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        // Number format
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0"));

        // Background orange pour les valeurs négatives
        style.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Alignment
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // Borders
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    private Double getDoubleValue(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
