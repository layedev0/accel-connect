-- ============================================================
-- VUES DE CONSOMMATION - AVEC SUBVENTION DYNAMIQUE (STAGIAIRES)
-- Stagiaires (is_intern = true): subvention 2000 FCFA
-- Non-stagiaires: subvention 1000 FCFA
-- ============================================================


-- ============================================================
-- VUE JOLOF (livraison gratuite, subvention dynamique)
-- ============================================================
DROP VIEW IF EXISTS public.view_jolof;

CREATE OR REPLACE VIEW public.view_jolof AS
WITH user_subsidy AS (
    SELECT id AS user_id,
        CASE WHEN COALESCE(is_intern, false) THEN 2000 ELSE 1000 END AS subsidy_cap
    FROM public.users
),
items_par_jour AS (
    SELECT
        o.user_id,
        o.order_date,
        oi.id AS item_id,
        oi.subtotal,
        r.restaurant_name
    FROM public.orders o
    INNER JOIN public.order_items oi ON oi.order_id = o.id
    INNER JOIN public.restaurant r ON oi.restaurant_id = r.id
    WHERE o.status = 'CONFIRMED'
),
max_prix AS (
    SELECT user_id, order_date, MAX(subtotal) AS max_subtotal
    FROM items_par_jour
    GROUP BY user_id, order_date
),
items_ordonnes AS (
    SELECT
        i.*,
        m.max_subtotal,
        ROW_NUMBER() OVER (PARTITION BY i.user_id, i.order_date ORDER BY i.subtotal ASC, i.item_id) AS rn,
        SUM(i.subtotal) OVER (PARTITION BY i.user_id, i.order_date ORDER BY i.subtotal ASC, i.item_id ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS cumul
    FROM items_par_jour i
    INNER JOIN max_prix m ON i.user_id = m.user_id AND i.order_date = m.order_date
),
items_avec_cumul_prec AS (
    SELECT
        *,
        COALESCE(LAG(cumul) OVER (PARTITION BY user_id, order_date ORDER BY rn), 0) AS cumul_precedent
    FROM items_ordonnes
),
item_principal AS (
    SELECT DISTINCT ON (user_id, order_date)
        user_id, order_date, item_id AS item_principal_id
    FROM items_par_jour
    ORDER BY user_id, order_date, subtotal DESC, item_id
),
subvention_calculee AS (
    SELECT
        ic.*,
        ip.item_principal_id,
        us.subsidy_cap,
        CASE
            WHEN ic.max_subtotal >= us.subsidy_cap THEN
                CASE WHEN ic.item_id = ip.item_principal_id THEN us.subsidy_cap ELSE 0 END
            ELSE
                CASE
                    WHEN ic.cumul <= us.subsidy_cap THEN ic.subtotal
                    WHEN ic.cumul_precedent < us.subsidy_cap THEN us.subsidy_cap - ic.cumul_precedent
                    ELSE 0
                END
        END AS subsidy_amount
    FROM items_avec_cumul_prec ic
    INNER JOIN item_principal ip ON ic.user_id = ip.user_id AND ic.order_date = ip.order_date
    INNER JOIN user_subsidy us ON ic.user_id = us.user_id
)
SELECT
    o.order_date,
    u.first_name,
    u.last_name,
    m.meal_name AS meal_name,
    oi.subtotal,
    sc.subsidy_amount,
    GREATEST(oi.subtotal - sc.subsidy_amount, 0) AS amount_after_subsidy,
    0 AS delivery_fee_share,
    GREATEST(oi.subtotal - sc.subsidy_amount, 0) AS total_amount
FROM public.orders o
INNER JOIN public.users u ON o.user_id = u.id
INNER JOIN public.order_items oi ON oi.order_id = o.id
INNER JOIN public.restaurant r ON oi.restaurant_id = r.id
INNER JOIN public.meals m ON oi.meal_id = m.id
INNER JOIN subvention_calculee sc ON sc.item_id = oi.id
WHERE o.status = 'CONFIRMED'
  AND r.restaurant_name = 'JOLOF'
ORDER BY o.order_date DESC, u.last_name, oi.subtotal DESC;


-- ============================================================
-- VUE EVA (livraison 1000 partagee, subvention dynamique)
-- ============================================================
DROP VIEW IF EXISTS public.view_eva;

CREATE OR REPLACE VIEW public.view_eva AS
WITH user_subsidy AS (
    SELECT id AS user_id,
        CASE WHEN COALESCE(is_intern, false) THEN 2000 ELSE 1000 END AS subsidy_cap
    FROM public.users
),
items_par_jour AS (
    SELECT
        o.user_id,
        o.order_date,
        oi.id AS item_id,
        oi.subtotal,
        r.restaurant_name
    FROM public.orders o
    INNER JOIN public.order_items oi ON oi.order_id = o.id
    INNER JOIN public.restaurant r ON oi.restaurant_id = r.id
    WHERE o.status = 'CONFIRMED'
),
max_prix AS (
    SELECT user_id, order_date, MAX(subtotal) AS max_subtotal
    FROM items_par_jour
    GROUP BY user_id, order_date
),
items_ordonnes AS (
    SELECT
        i.*,
        m.max_subtotal,
        ROW_NUMBER() OVER (PARTITION BY i.user_id, i.order_date ORDER BY i.subtotal ASC, i.item_id) AS rn,
        SUM(i.subtotal) OVER (PARTITION BY i.user_id, i.order_date ORDER BY i.subtotal ASC, i.item_id ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS cumul
    FROM items_par_jour i
    INNER JOIN max_prix m ON i.user_id = m.user_id AND i.order_date = m.order_date
),
items_avec_cumul_prec AS (
    SELECT
        *,
        COALESCE(LAG(cumul) OVER (PARTITION BY user_id, order_date ORDER BY rn), 0) AS cumul_precedent
    FROM items_ordonnes
),
item_principal AS (
    SELECT DISTINCT ON (user_id, order_date)
        user_id, order_date, item_id AS item_principal_id
    FROM items_par_jour
    ORDER BY user_id, order_date, subtotal DESC, item_id
),
subvention_calculee AS (
    SELECT
        ic.*,
        ip.item_principal_id,
        us.subsidy_cap,
        CASE
            WHEN ic.max_subtotal >= us.subsidy_cap THEN
                CASE WHEN ic.item_id = ip.item_principal_id THEN us.subsidy_cap ELSE 0 END
            ELSE
                CASE
                    WHEN ic.cumul <= us.subsidy_cap THEN ic.subtotal
                    WHEN ic.cumul_precedent < us.subsidy_cap THEN us.subsidy_cap - ic.cumul_precedent
                    ELSE 0
                END
        END AS subsidy_amount
    FROM items_avec_cumul_prec ic
    INNER JOIN item_principal ip ON ic.user_id = ip.user_id AND ic.order_date = ip.order_date
    INNER JOIN user_subsidy us ON ic.user_id = us.user_id
),
daily_clients_eva AS (
    SELECT
        o.order_date,
        COUNT(DISTINCT o.user_id) AS nb_clients
    FROM public.orders o
    INNER JOIN public.order_items oi ON oi.order_id = o.id
    INNER JOIN public.restaurant r ON oi.restaurant_id = r.id
    WHERE o.status = 'CONFIRMED'
      AND r.restaurant_name = 'EVA'
    GROUP BY o.order_date
)
SELECT
    o.order_date,
    u.first_name,
    u.last_name,
    m.meal_name AS meal_name,
    oi.subtotal,
    sc.subsidy_amount,
    GREATEST(oi.subtotal - sc.subsidy_amount, 0) AS amount_after_subsidy,
    ROUND(1000.0 / dc.nb_clients, 0) AS delivery_fee_share,
    GREATEST(oi.subtotal - sc.subsidy_amount, 0) + ROUND(1000.0 / dc.nb_clients, 0) AS total_amount
FROM public.orders o
INNER JOIN public.users u ON o.user_id = u.id
INNER JOIN public.order_items oi ON oi.order_id = o.id
INNER JOIN public.restaurant r ON oi.restaurant_id = r.id
INNER JOIN public.meals m ON oi.meal_id = m.id
INNER JOIN subvention_calculee sc ON sc.item_id = oi.id
INNER JOIN daily_clients_eva dc ON o.order_date = dc.order_date
WHERE o.status = 'CONFIRMED'
  AND r.restaurant_name = 'EVA'
ORDER BY o.order_date DESC, u.last_name, oi.subtotal DESC;


-- ============================================================
-- VUE RODRA (livraison 1000 partagee, subvention dynamique)
-- ============================================================
DROP VIEW IF EXISTS public.view_rodra;

CREATE OR REPLACE VIEW public.view_rodra AS
WITH user_subsidy AS (
    SELECT id AS user_id,
        CASE WHEN COALESCE(is_intern, false) THEN 2000 ELSE 1000 END AS subsidy_cap
    FROM public.users
),
items_par_jour AS (
    SELECT
        o.user_id,
        o.order_date,
        oi.id AS item_id,
        oi.subtotal,
        r.restaurant_name
    FROM public.orders o
    INNER JOIN public.order_items oi ON oi.order_id = o.id
    INNER JOIN public.restaurant r ON oi.restaurant_id = r.id
    WHERE o.status = 'CONFIRMED'
),
max_prix AS (
    SELECT user_id, order_date, MAX(subtotal) AS max_subtotal
    FROM items_par_jour
    GROUP BY user_id, order_date
),
items_ordonnes AS (
    SELECT
        i.*,
        m.max_subtotal,
        ROW_NUMBER() OVER (PARTITION BY i.user_id, i.order_date ORDER BY i.subtotal ASC, i.item_id) AS rn,
        SUM(i.subtotal) OVER (PARTITION BY i.user_id, i.order_date ORDER BY i.subtotal ASC, i.item_id ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS cumul
    FROM items_par_jour i
    INNER JOIN max_prix m ON i.user_id = m.user_id AND i.order_date = m.order_date
),
items_avec_cumul_prec AS (
    SELECT
        *,
        COALESCE(LAG(cumul) OVER (PARTITION BY user_id, order_date ORDER BY rn), 0) AS cumul_precedent
    FROM items_ordonnes
),
item_principal AS (
    SELECT DISTINCT ON (user_id, order_date)
        user_id, order_date, item_id AS item_principal_id
    FROM items_par_jour
    ORDER BY user_id, order_date, subtotal DESC, item_id
),
subvention_calculee AS (
    SELECT
        ic.*,
        ip.item_principal_id,
        us.subsidy_cap,
        CASE
            WHEN ic.max_subtotal >= us.subsidy_cap THEN
                CASE WHEN ic.item_id = ip.item_principal_id THEN us.subsidy_cap ELSE 0 END
            ELSE
                CASE
                    WHEN ic.cumul <= us.subsidy_cap THEN ic.subtotal
                    WHEN ic.cumul_precedent < us.subsidy_cap THEN us.subsidy_cap - ic.cumul_precedent
                    ELSE 0
                END
        END AS subsidy_amount
    FROM items_avec_cumul_prec ic
    INNER JOIN item_principal ip ON ic.user_id = ip.user_id AND ic.order_date = ip.order_date
    INNER JOIN user_subsidy us ON ic.user_id = us.user_id
),
daily_clients_rodra AS (
    SELECT
        o.order_date,
        COUNT(DISTINCT o.user_id) AS nb_clients
    FROM public.orders o
    INNER JOIN public.order_items oi ON oi.order_id = o.id
    INNER JOIN public.restaurant r ON oi.restaurant_id = r.id
    WHERE o.status = 'CONFIRMED'
      AND r.restaurant_name = 'RODRA'
    GROUP BY o.order_date
)
SELECT
    o.order_date,
    u.first_name,
    u.last_name,
    m.meal_name AS meal_name,
    oi.subtotal,
    sc.subsidy_amount,
    GREATEST(oi.subtotal - sc.subsidy_amount, 0) AS amount_after_subsidy,
    ROUND(1000.0 / dc.nb_clients, 0) AS delivery_fee_share,
    GREATEST(oi.subtotal - sc.subsidy_amount, 0) + ROUND(1000.0 / dc.nb_clients, 0) AS total_amount
FROM public.orders o
INNER JOIN public.users u ON o.user_id = u.id
INNER JOIN public.order_items oi ON oi.order_id = o.id
INNER JOIN public.restaurant r ON oi.restaurant_id = r.id
INNER JOIN public.meals m ON oi.meal_id = m.id
INNER JOIN subvention_calculee sc ON sc.item_id = oi.id
INNER JOIN daily_clients_rodra dc ON o.order_date = dc.order_date
WHERE o.status = 'CONFIRMED'
  AND r.restaurant_name = 'RODRA'
ORDER BY o.order_date DESC, u.last_name, oi.subtotal DESC;


-- ============================================================
-- VUE NOWELLI (livraison 1000 partagee, subvention dynamique)
-- ============================================================
DROP VIEW IF EXISTS public.view_nowelli;

CREATE OR REPLACE VIEW public.view_nowelli AS
WITH user_subsidy AS (
    SELECT id AS user_id,
        CASE WHEN COALESCE(is_intern, false) THEN 2000 ELSE 1000 END AS subsidy_cap
    FROM public.users
),
items_par_jour AS (
    SELECT
        o.user_id,
        o.order_date,
        oi.id AS item_id,
        oi.subtotal,
        r.restaurant_name
    FROM public.orders o
    INNER JOIN public.order_items oi ON oi.order_id = o.id
    INNER JOIN public.restaurant r ON oi.restaurant_id = r.id
    WHERE o.status = 'CONFIRMED'
),
max_prix AS (
    SELECT user_id, order_date, MAX(subtotal) AS max_subtotal
    FROM items_par_jour
    GROUP BY user_id, order_date
),
items_ordonnes AS (
    SELECT
        i.*,
        m.max_subtotal,
        ROW_NUMBER() OVER (PARTITION BY i.user_id, i.order_date ORDER BY i.subtotal ASC, i.item_id) AS rn,
        SUM(i.subtotal) OVER (PARTITION BY i.user_id, i.order_date ORDER BY i.subtotal ASC, i.item_id ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS cumul
    FROM items_par_jour i
    INNER JOIN max_prix m ON i.user_id = m.user_id AND i.order_date = m.order_date
),
items_avec_cumul_prec AS (
    SELECT
        *,
        COALESCE(LAG(cumul) OVER (PARTITION BY user_id, order_date ORDER BY rn), 0) AS cumul_precedent
    FROM items_ordonnes
),
item_principal AS (
    SELECT DISTINCT ON (user_id, order_date)
        user_id, order_date, item_id AS item_principal_id
    FROM items_par_jour
    ORDER BY user_id, order_date, subtotal DESC, item_id
),
subvention_calculee AS (
    SELECT
        ic.*,
        ip.item_principal_id,
        us.subsidy_cap,
        CASE
            WHEN ic.max_subtotal >= us.subsidy_cap THEN
                CASE WHEN ic.item_id = ip.item_principal_id THEN us.subsidy_cap ELSE 0 END
            ELSE
                CASE
                    WHEN ic.cumul <= us.subsidy_cap THEN ic.subtotal
                    WHEN ic.cumul_precedent < us.subsidy_cap THEN us.subsidy_cap - ic.cumul_precedent
                    ELSE 0
                END
        END AS subsidy_amount
    FROM items_avec_cumul_prec ic
    INNER JOIN item_principal ip ON ic.user_id = ip.user_id AND ic.order_date = ip.order_date
    INNER JOIN user_subsidy us ON ic.user_id = us.user_id
),
daily_clients_nowelli AS (
    SELECT
        o.order_date,
        COUNT(DISTINCT o.user_id) AS nb_clients
    FROM public.orders o
    INNER JOIN public.order_items oi ON oi.order_id = o.id
    INNER JOIN public.restaurant r ON oi.restaurant_id = r.id
    WHERE o.status = 'CONFIRMED'
      AND r.restaurant_name = 'NOWELLI'
    GROUP BY o.order_date
)
SELECT
    o.order_date,
    u.first_name,
    u.last_name,
    m.meal_name AS meal_name,
    oi.subtotal,
    sc.subsidy_amount,
    GREATEST(oi.subtotal - sc.subsidy_amount, 0) AS amount_after_subsidy,
    ROUND(1000.0 / dc.nb_clients, 0) AS delivery_fee_share,
    GREATEST(oi.subtotal - sc.subsidy_amount, 0) + ROUND(1000.0 / dc.nb_clients, 0) AS total_amount
FROM public.orders o
INNER JOIN public.users u ON o.user_id = u.id
INNER JOIN public.order_items oi ON oi.order_id = o.id
INNER JOIN public.restaurant r ON oi.restaurant_id = r.id
INNER JOIN public.meals m ON oi.meal_id = m.id
INNER JOIN subvention_calculee sc ON sc.item_id = oi.id
INNER JOIN daily_clients_nowelli dc ON o.order_date = dc.order_date
WHERE o.status = 'CONFIRMED'
  AND r.restaurant_name = 'NOWELLI'
ORDER BY o.order_date DESC, u.last_name, oi.subtotal DESC;


-- ============================================================
-- VUE RECAPITULATIVE PAR RESTO ET PAR JOUR
-- ============================================================
DROP VIEW IF EXISTS public.view_recap_daily;

CREATE OR REPLACE VIEW public.view_recap_daily AS
WITH user_subsidy AS (
    SELECT id AS user_id,
        CASE WHEN COALESCE(is_intern, false) THEN 2000 ELSE 1000 END AS subsidy_cap
    FROM public.users
),
items_par_jour AS (
    SELECT
        o.user_id,
        o.order_date,
        oi.id AS item_id,
        oi.subtotal,
        r.restaurant_name
    FROM public.orders o
    INNER JOIN public.order_items oi ON oi.order_id = o.id
    INNER JOIN public.restaurant r ON oi.restaurant_id = r.id
    WHERE o.status = 'CONFIRMED'
),
max_prix AS (
    SELECT user_id, order_date, MAX(subtotal) AS max_subtotal
    FROM items_par_jour
    GROUP BY user_id, order_date
),
items_ordonnes AS (
    SELECT
        i.*,
        m.max_subtotal,
        ROW_NUMBER() OVER (PARTITION BY i.user_id, i.order_date ORDER BY i.subtotal ASC, i.item_id) AS rn,
        SUM(i.subtotal) OVER (PARTITION BY i.user_id, i.order_date ORDER BY i.subtotal ASC, i.item_id ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS cumul
    FROM items_par_jour i
    INNER JOIN max_prix m ON i.user_id = m.user_id AND i.order_date = m.order_date
),
items_avec_cumul_prec AS (
    SELECT
        *,
        COALESCE(LAG(cumul) OVER (PARTITION BY user_id, order_date ORDER BY rn), 0) AS cumul_precedent
    FROM items_ordonnes
),
item_principal AS (
    SELECT DISTINCT ON (user_id, order_date)
        user_id, order_date, item_id AS item_principal_id
    FROM items_par_jour
    ORDER BY user_id, order_date, subtotal DESC, item_id
),
subvention_calculee AS (
    SELECT
        ic.*,
        ip.item_principal_id,
        us.subsidy_cap,
        CASE
            WHEN ic.max_subtotal >= us.subsidy_cap THEN
                CASE WHEN ic.item_id = ip.item_principal_id THEN us.subsidy_cap ELSE 0 END
            ELSE
                CASE
                    WHEN ic.cumul <= us.subsidy_cap THEN ic.subtotal
                    WHEN ic.cumul_precedent < us.subsidy_cap THEN us.subsidy_cap - ic.cumul_precedent
                    ELSE 0
                END
        END AS subsidy_amount
    FROM items_avec_cumul_prec ic
    INNER JOIN item_principal ip ON ic.user_id = ip.user_id AND ic.order_date = ip.order_date
    INNER JOIN user_subsidy us ON ic.user_id = us.user_id
)
SELECT
    sc.order_date,
    sc.restaurant_name,
    COUNT(sc.item_id) AS nbre_commandes,
    COUNT(CASE WHEN sc.subsidy_amount > 0 THEN 1 END) AS nbre_commandes_subventionnees,
    SUM(sc.subtotal) AS sous_total,
    SUM(sc.subsidy_amount) AS total_subvention,
    SUM(sc.subtotal) - SUM(sc.subsidy_amount) AS total_a_payer
FROM subvention_calculee sc
GROUP BY sc.order_date, sc.restaurant_name
ORDER BY sc.order_date DESC, sc.restaurant_name;


-- ============================================================
-- VUE RECAPITULATIVE HEBDOMADAIRE PAR RESTO
-- ============================================================
DROP VIEW IF EXISTS public.view_recap_weekly;

CREATE OR REPLACE VIEW public.view_recap_weekly AS
WITH user_subsidy AS (
    SELECT id AS user_id,
        CASE WHEN COALESCE(is_intern, false) THEN 2000 ELSE 1000 END AS subsidy_cap
    FROM public.users
),
items_par_jour AS (
    SELECT
        o.user_id,
        o.order_date,
        oi.id AS item_id,
        oi.subtotal,
        r.restaurant_name
    FROM public.orders o
    INNER JOIN public.order_items oi ON oi.order_id = o.id
    INNER JOIN public.restaurant r ON oi.restaurant_id = r.id
    WHERE o.status = 'CONFIRMED'
),
max_prix AS (
    SELECT user_id, order_date, MAX(subtotal) AS max_subtotal
    FROM items_par_jour
    GROUP BY user_id, order_date
),
items_ordonnes AS (
    SELECT
        i.*,
        m.max_subtotal,
        ROW_NUMBER() OVER (PARTITION BY i.user_id, i.order_date ORDER BY i.subtotal ASC, i.item_id) AS rn,
        SUM(i.subtotal) OVER (PARTITION BY i.user_id, i.order_date ORDER BY i.subtotal ASC, i.item_id ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS cumul
    FROM items_par_jour i
    INNER JOIN max_prix m ON i.user_id = m.user_id AND i.order_date = m.order_date
),
items_avec_cumul_prec AS (
    SELECT
        *,
        COALESCE(LAG(cumul) OVER (PARTITION BY user_id, order_date ORDER BY rn), 0) AS cumul_precedent
    FROM items_ordonnes
),
item_principal AS (
    SELECT DISTINCT ON (user_id, order_date)
        user_id, order_date, item_id AS item_principal_id
    FROM items_par_jour
    ORDER BY user_id, order_date, subtotal DESC, item_id
),
subvention_calculee AS (
    SELECT
        ic.*,
        ip.item_principal_id,
        us.subsidy_cap,
        CASE
            WHEN ic.max_subtotal >= us.subsidy_cap THEN
                CASE WHEN ic.item_id = ip.item_principal_id THEN us.subsidy_cap ELSE 0 END
            ELSE
                CASE
                    WHEN ic.cumul <= us.subsidy_cap THEN ic.subtotal
                    WHEN ic.cumul_precedent < us.subsidy_cap THEN us.subsidy_cap - ic.cumul_precedent
                    ELSE 0
                END
        END AS subsidy_amount
    FROM items_avec_cumul_prec ic
    INNER JOIN item_principal ip ON ic.user_id = ip.user_id AND ic.order_date = ip.order_date
    INNER JOIN user_subsidy us ON ic.user_id = us.user_id
),
jours_commandes AS (
    SELECT
        DATE_TRUNC('week', order_date)::date AS semaine_debut,
        restaurant_name,
        COUNT(DISTINCT order_date) AS nb_jours_livraison
    FROM items_par_jour
    GROUP BY DATE_TRUNC('week', order_date), restaurant_name
),
recap_semaine AS (
    SELECT
        DATE_TRUNC('week', sc.order_date)::date AS semaine_debut,
        sc.restaurant_name,
        COUNT(sc.item_id) AS nbre_commandes,
        COUNT(CASE WHEN sc.subsidy_amount > 0 THEN 1 END) AS nbre_commandes_subventionnees,
        SUM(sc.subtotal) AS sous_total,
        SUM(sc.subsidy_amount) AS total_subvention
    FROM subvention_calculee sc
    GROUP BY DATE_TRUNC('week', sc.order_date), sc.restaurant_name
)
SELECT
    rs.semaine_debut || ' - ' || (rs.semaine_debut + INTERVAL '4 days')::date AS periode,
    rs.restaurant_name,
    rs.nbre_commandes,
    rs.nbre_commandes_subventionnees,
    rs.sous_total,
    rs.total_subvention,
    rs.sous_total - rs.total_subvention AS total_a_payer,
    CASE
        WHEN rs.restaurant_name = 'JOLOF' THEN 0
        ELSE jc.nb_jours_livraison * 1000
    END AS total_livraison_semaine
FROM recap_semaine rs
INNER JOIN jours_commandes jc ON rs.semaine_debut = jc.semaine_debut AND rs.restaurant_name = jc.restaurant_name
ORDER BY rs.semaine_debut DESC, rs.restaurant_name;
