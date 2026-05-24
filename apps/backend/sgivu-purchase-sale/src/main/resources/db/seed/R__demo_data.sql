-- Demo data — development environment only
-- Contratos sintéticos alineados con el seed de vehículos. Cada PURCHASE tiene
-- vehicle_id = id (ambos seeds enumeran los mismos (s_idx, mb, instance) en el
-- mismo orden). Cada SALE se empareja con un PURCHASE del mismo segmento
-- mediante rank_in_segment (kth SALE ↔ kth SOLD PURCHASE).
--
-- Modelo de demanda compartido (idéntico al de
-- apps/backend/sgivu-vehicle/.../R__demo_data.sql y
-- apps/ml/sgivu-ml/tests/generate_contracts.py):
--   ventas_objetivo(s, mb) = max(1, round(
--       v_base[s]
--       × v_seasonality[mes_calendario(now(), mb)]
--       × (1 + 0.005 × (v_horizon - mb))
--       × (0.85 + 0.30 × pseudo_random(s, mb))
--   ))
--   pseudo_random(s, mb) = ((s × 7919 + mb × 31337) % 1000) / 1000.0
--
-- Volumen aproximado: ~6.700 PURCHASE + ~5.300 SALE ≈ 12.000 contratos sobre
-- 36 meses, 39 segmentos, con estacionalidad anual marcada y crecimiento
-- mensual del 0,5 %.
--
-- Idempotente: TRUNCATE ... RESTART IDENTITY CASCADE antes de re-insertar.

DO
$$
    DECLARE
        v_methods       TEXT[] := ARRAY ['CASH','BANK_TRANSFER','FINANCING','BANK_DEPOSIT','CASHIERS_CHECK','DIGITAL_WALLET','INSTALLMENT_PAYMENT'];
        v_terms         TEXT[] := ARRAY [
            'Pago único al momento de entrega',
            '50% al inicio y 50% a la entrega',
            '24 cuotas mensuales',
            'Pago en 3 partes iguales',
            'Pago mediante cheque certificado',
            '6 cuotas mensuales iguales',
            'Pago por billetera digital',
            '60% al inicio y 40% a la entrega'
            ];
        v_limitations   TEXT[] := ARRAY [
            'Sin cuotas adicionales',
            'Máximo 2 cuotas',
            'Financiación bancaria aprobada',
            'Máximo 3 cuotas',
            'Cheque de gerencia requerido',
            'Máximo 6 cuotas',
            'Pago digital único',
            'Sujeto a validación bancaria'
            ];
        v_observations  TEXT[] := ARRAY [
            NULL,
            'Vehículo con SOAT vigente, revisión técnico-mecánica al día.',
            'Sin deudas reportadas en RUNT y SIMIT.',
            'Mantenimiento reciente en concesionario.',
            'Un solo dueño, historial de servicios completo.',
            'Llantas nuevas instaladas hace menos de 2 meses.',
            NULL
            ];
        -- Catálogo de 39 segmentos (idéntico al seed de vehicle).
        v_brand         TEXT[] := ARRAY [
            'Yamaha','Yamaha','Yamaha','Yamaha',
            'Honda','Honda','Honda','Honda',
            'Bajaj','Bajaj','Bajaj','Bajaj',
            'Suzuki','Suzuki','Suzuki','Suzuki',
            'Kawasaki','Kawasaki','Kawasaki',
            'Mazda','Mazda','Mazda','Mazda',
            'Hyundai','Hyundai','Hyundai','Hyundai',
            'Chevrolet','Chevrolet','Chevrolet','Chevrolet',
            'Renault','Renault','Renault','Renault',
            'Ford','Ford','Ford','Ford'
            ];
        v_model         TEXT[] := ARRAY [
            'FZ 2.0','FZ 2.0','NMAX','R3',
            'CB 190R','Wave 110','XR 150L','PCX 150',
            'Pulsar 200NS','Boxer CT100','Discover 125','Dominar 400',
            'Gixxer 155','GN 125','AX4','DR 650',
            'Z400','Ninja 300','Versys 650',
            '3 Touring','3 Touring','CX-5','2 Sedan',
            'i25','i25','Tucson','Elantra',
            'Onix','Tracker','Sail','Spark GT',
            'Logan','Sandero','Duster','Kwid',
            'Fiesta','Fiesta','Ranger','Explorer'
            ];
        v_base          INT[]   := ARRAY [
            6,4,4,2, 5,6,3,2, 4,6,4,2, 3,4,2,2, 2,4,2,
            3,4,5,2, 5,4,3,2, 4,3,2,3, 3,4,4,2, 3,2,3,4
            ];
        v_seasonality   NUMERIC[] := ARRAY [
            0.70, 0.80, 0.95, 1.10, 1.20, 1.25,
            1.10, 0.90, 0.75, 0.85, 1.05, 1.30
            ];
        v_horizon       INT      := 36;
        v_now           TIMESTAMPTZ := NOW();
        v_purchase_max  BIGINT;
    BEGIN
        PERFORM setseed(0.42);

        TRUNCATE purchase_sales, contract_status_history RESTART IDENTITY CASCADE;

        -- ============================================================
        -- PURCHASE: emitir n_sales+1 compras por (segmento, mes).
        -- vehicle_id = id (alineado con la enumeración del seed de vehicle).
        -- instance ∈ [1, n_sales] → COMPLETED (luego serán vendidas);
        -- instance = n_sales+1 → 70/20/10 (COMPLETED/ACTIVE/PENDING) sobre la
        -- compra "sobrante" que queda en inventario.
        -- ============================================================
        INSERT INTO purchase_sales (id, client_id, user_id, vehicle_id,
                                    purchase_price, sale_price,
                                    contract_type, contract_status,
                                    payment_limitations, payment_terms, payment_method,
                                    observations, created_at, updated_at)
        WITH
        seg_month AS (
            SELECT
                s_idx, mb,
                v_base[s_idx] AS base,
                ((extract(month from v_now)::int - mb - 1) % 12 + 12) % 12 + 1 AS cal_month
            FROM
                generate_series(1, 39) AS s_idx,
                generate_series(1, v_horizon) AS mb
        ),
        cell AS (
            SELECT
                s_idx, mb, base,
                GREATEST(1, ROUND(
                    base
                    * v_seasonality[cal_month]
                    * (1.0 + 0.005 * (v_horizon - mb))
                    * (0.85 + 0.30 * (((s_idx * 7919 + mb * 31337) % 1000)::numeric / 1000.0))
                ))::int AS n_sales
            FROM seg_month
        ),
        expanded AS (
            SELECT
                c.s_idx, c.mb, c.n_sales, instance,
                row_number() OVER (ORDER BY c.mb DESC, c.s_idx, instance) AS id
            FROM cell c, LATERAL generate_series(1, c.n_sales + 1) AS instance
        )
        SELECT
            e.id,
            1 + ((e.id - 1) % 350),
            1 + ((e.id - 1) % 16),
            e.id,
            -- Precio base por marca con ruido del 30 %.
            ((CASE v_brand[e.s_idx]
                WHEN 'Yamaha'    THEN 12000000
                WHEN 'Honda'     THEN 12000000
                WHEN 'Bajaj'     THEN  9000000
                WHEN 'Suzuki'    THEN 11000000
                WHEN 'Kawasaki'  THEN 20000000
                WHEN 'Mazda'     THEN 45000000
                WHEN 'Hyundai'   THEN 40000000
                WHEN 'Chevrolet' THEN 35000000
                WHEN 'Renault'   THEN 32000000
                WHEN 'Ford'      THEN 50000000
            END) * (0.85 + random() * 0.30))::NUMERIC(12, 2),
            -- Precio de venta esperado (margen 10-35 % sobre el precio base).
            ((CASE v_brand[e.s_idx]
                WHEN 'Yamaha'    THEN 12000000
                WHEN 'Honda'     THEN 12000000
                WHEN 'Bajaj'     THEN  9000000
                WHEN 'Suzuki'    THEN 11000000
                WHEN 'Kawasaki'  THEN 20000000
                WHEN 'Mazda'     THEN 45000000
                WHEN 'Hyundai'   THEN 40000000
                WHEN 'Chevrolet' THEN 35000000
                WHEN 'Renault'   THEN 32000000
                WHEN 'Ford'      THEN 50000000
            END) * (1.10 + random() * 0.25))::NUMERIC(12, 2),
            'PURCHASE',
            CASE
                WHEN e.instance <= e.n_sales THEN 'COMPLETED'
                WHEN (e.id % 10) < 6 THEN 'COMPLETED'
                WHEN (e.id % 10) < 8 THEN 'ACTIVE'
                WHEN (e.id % 10) < 9 THEN 'PENDING'
                ELSE 'CANCELED'
            END,
            v_limitations[1 + (e.id::int % 8)],
            v_terms[1 + (e.id::int % 8)],
            v_methods[1 + (e.id::int % 7)],
            'Compra ' || v_brand[e.s_idx] || ' ' || v_model[e.s_idx],
            date_trunc('month', v_now)
                - make_interval(months => e.mb::int)
                + make_interval(days => ((e.s_idx * 3 + e.instance * 7) % 28))
                + make_interval(hours => (e.id::int % 24)),
            date_trunc('month', v_now)
                - make_interval(months => e.mb::int)
                + make_interval(days => ((e.s_idx * 3 + e.instance * 7) % 28) + 2)
                + make_interval(hours => (e.id::int % 24))
        FROM expanded e;

        -- Sincronizar la secuencia para que las SALEs continúen tras el último PURCHASE.
        SELECT MAX(id) INTO v_purchase_max FROM purchase_sales;
        PERFORM setval('purchase_sales_id_seq', v_purchase_max);

        -- ============================================================
        -- SALE: por cada (segmento, mes) emitir n_sales ventas, cada una
        -- pareada con la kth SOLD PURCHASE del mismo segmento.
        -- Estado: 70 % COMPLETED, 15 % ACTIVE, 5 % PENDING, 10 % CANCELED.
        -- ============================================================
        INSERT INTO purchase_sales (client_id, user_id, vehicle_id,
                                    purchase_price, sale_price,
                                    contract_type, contract_status,
                                    payment_limitations, payment_terms, payment_method,
                                    observations, created_at, updated_at)
        WITH
        seg_month AS (
            SELECT
                s_idx, mb,
                v_base[s_idx] AS base,
                ((extract(month from v_now)::int - mb - 1) % 12 + 12) % 12 + 1 AS cal_month
            FROM
                generate_series(1, 39) AS s_idx,
                generate_series(1, v_horizon) AS mb
        ),
        cell AS (
            SELECT
                s_idx, mb, base,
                GREATEST(1, ROUND(
                    base
                    * v_seasonality[cal_month]
                    * (1.0 + 0.005 * (v_horizon - mb))
                    * (0.85 + 0.30 * (((s_idx * 7919 + mb * 31337) % 1000)::numeric / 1000.0))
                ))::int AS n_sales
            FROM seg_month
        ),
        -- Enumerar PURCHASEs en el mismo orden global (mb DESC, s_idx, instance)
        -- que el INSERT anterior, para que purchase_id coincida con la id real.
        purchase_all AS (
            SELECT
                c.s_idx, c.mb, c.n_sales, instance,
                row_number() OVER (ORDER BY c.mb DESC, c.s_idx, instance) AS purchase_id
            FROM cell c, LATERAL generate_series(1, c.n_sales + 1) AS instance
        ),
        -- Solo las PURCHASE "vendidas" (instance ≤ n_sales) reciben un rango
        -- dentro de su segmento. Filtrar ANTES del row_number garantiza que el
        -- kth SALE empareje con el kth SOLD PURCHASE (las +1 de inventario no
        -- desplazan los rangos).
        sold_purchase_ranks AS (
            SELECT
                s_idx, purchase_id,
                row_number() OVER (PARTITION BY s_idx ORDER BY mb DESC, instance) AS rank_in_segment
            FROM purchase_all
            WHERE instance <= n_sales
        ),
        sale_enum AS (
            SELECT
                c.s_idx, c.mb, c.n_sales, instance,
                row_number() OVER (PARTITION BY c.s_idx ORDER BY c.mb DESC, instance) AS rank_in_segment
            FROM cell c, LATERAL generate_series(1, c.n_sales) AS instance
        ),
        sale_paired AS (
            SELECT
                s.s_idx, s.mb, s.instance,
                sp.purchase_id AS vehicle_id
            FROM sale_enum s
            JOIN sold_purchase_ranks sp
                ON sp.s_idx = s.s_idx
               AND sp.rank_in_segment = s.rank_in_segment
        )
        SELECT
            1 + ((sp.vehicle_id * 7 + 13) % 350),
            1 + (((sp.vehicle_id + 3) * 5) % 16),
            sp.vehicle_id,
            -- precio_compra del vehículo: lo recuperamos de la PURCHASE original.
            ps.purchase_price,
            -- precio_venta con margen real: 5-15 % sobre el sale_price del vehículo.
            (ps.sale_price * (0.98 + random() * 0.10))::NUMERIC(12, 2),
            'SALE',
            CASE
                WHEN ((sp.vehicle_id * 13) % 20) < 14 THEN 'COMPLETED'
                WHEN ((sp.vehicle_id * 13) % 20) < 17 THEN 'ACTIVE'
                WHEN ((sp.vehicle_id * 13) % 20) < 18 THEN 'PENDING'
                ELSE 'CANCELED'
            END,
            v_limitations[1 + ((sp.vehicle_id + 4)::int % 8)],
            v_terms[1 + ((sp.vehicle_id + 4)::int % 8)],
            v_methods[1 + ((sp.vehicle_id + 2)::int % 7)],
            v_observations[1 + ((sp.vehicle_id + 3)::int % 7)],
            -- SALE.created_at = PURCHASE.created_at + 7..36 días, capped a v_now.
            LEAST(ps.created_at + make_interval(days => (7 + ((sp.vehicle_id * 7) % 30))::int), v_now),
            LEAST(ps.created_at + make_interval(days => (10 + ((sp.vehicle_id * 7) % 32))::int), v_now)
        FROM sale_paired sp
        JOIN purchase_sales ps ON ps.id = sp.vehicle_id;

        -- ============================================================
        -- contract_status_history — 1..3 filas por contrato según estado final.
        -- ============================================================
        INSERT INTO contract_status_history (purchase_sale_id, previous_status, new_status,
                                             changed_by, changed_at, reason)
        SELECT ps.id,
               CASE step.n WHEN 1 THEN NULL WHEN 2 THEN 'PENDING' WHEN 3 THEN 'ACTIVE' END,
               CASE step.n
                   WHEN 1 THEN 'PENDING'
                   WHEN 2 THEN CASE WHEN ps.contract_status = 'CANCELED' THEN 'CANCELED' ELSE 'ACTIVE' END
                   WHEN 3 THEN 'COMPLETED'
               END,
               ps.user_id,
               LEAST(ps.created_at + make_interval(days => (step.n - 1) * 3), v_now),
               CASE step.n
                   WHEN 1 THEN 'Contrato creado'
                   WHEN 2 THEN CASE WHEN ps.contract_status = 'CANCELED' THEN 'Contrato cancelado'
                                    ELSE 'Condiciones verificadas' END
                   ELSE CASE WHEN ps.contract_type = 'SALE' THEN 'Venta completada' ELSE 'Compra completada' END
               END
        FROM purchase_sales ps
                 CROSS JOIN generate_series(1, 3) AS step(n)
        WHERE (ps.contract_status = 'COMPLETED' AND step.n <= 3)
           OR (ps.contract_status = 'ACTIVE' AND step.n <= 2)
           OR (ps.contract_status = 'PENDING' AND step.n <= 1)
           OR (ps.contract_status = 'CANCELED' AND step.n <= 2);

        -- Sincronizar secuencias finales.
        PERFORM setval('purchase_sales_id_seq', (SELECT COALESCE(MAX(id), 1) FROM purchase_sales));
        PERFORM setval('contract_status_history_id_seq',
                       (SELECT COALESCE(MAX(id), 1) FROM contract_status_history));
    END
$$;
